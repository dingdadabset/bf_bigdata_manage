<template>
  <div class="quality-view">
    <div class="module-header">
      <div>
        <h1>数据质量中心</h1>
        <p>基于 HiveServer2 执行质量规则，沉淀异常问题与负责人闭环</p>
      </div>
      <div class="header-actions">
        <a-button icon="reload" :loading="loadingOverview || loadingRules" @click="refreshAll">刷新</a-button>
        <a-button v-if="canManageQuality" type="primary" icon="plus" @click="showRuleModal()">新建规则</a-button>
      </div>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span>规则总数</span>
        <strong>{{ overview.totalRules || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>启用规则</span>
        <strong>{{ overview.activeRules || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>失败规则</span>
        <strong class="danger-text">{{ overview.failedRules || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>待处理问题</span>
        <strong class="warning-text">{{ overview.openIssues || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>最近成功率</span>
        <strong>{{ formatPercent(overview.recentSuccessRate) }}</strong>
      </div>
    </div>

    <a-tabs :active-key="activeTab" @change="activeTab = $event">
      <a-tab-pane key="dashboard" tab="质量看板">
        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-title">最近失败规则</div>
            <a-table
              size="small"
              row-key="id"
              :columns="dashboardRuleColumns"
              :data-source="failedRules"
              :pagination="false"
              :loading="loadingRules"
            >
              <template slot="ruleTarget" slot-scope="text, record">
                <span>{{ targetName(record) }}</span>
              </template>
              <template slot="statusTag" slot-scope="text">
                <a-tag :color="statusColor(text)">{{ text || '未执行' }}</a-tag>
              </template>
            </a-table>
          </section>
          <section class="panel">
            <div class="panel-title">待处理问题</div>
            <a-table
              size="small"
              row-key="id"
              :columns="dashboardIssueColumns"
              :data-source="openIssues"
              :pagination="false"
              :loading="loadingIssues"
            >
              <template slot="severity" slot-scope="text">
                <a-tag :color="severityColor(text)">{{ severityLabel(text) }}</a-tag>
              </template>
            </a-table>
          </section>
        </div>
      </a-tab-pane>

      <a-tab-pane key="rules" tab="规则管理">
        <div class="toolbar">
          <a-select v-model="ruleFilters.dataSourceId" allowClear placeholder="数据源" style="width: 220px" @change="onDataSourceFilterChange">
            <a-select-option v-for="item in dataSources" :key="item.id" :value="item.id">
              {{ item.name }} / {{ item.clusterCode || '-' }}
            </a-select-option>
          </a-select>
          <a-select v-model="ruleFilters.tableId" allowClear show-search option-filter-prop="children" placeholder="表资产" style="width: 280px" @change="fetchRules">
            <a-select-option v-for="table in tableOptions" :key="table.id" :value="table.id">
              {{ table.clusterCode || '-' }} / {{ table.dbName }}.{{ table.tableName }}
            </a-select-option>
          </a-select>
          <a-select v-model="ruleFilters.status" allowClear placeholder="状态" style="width: 130px" @change="fetchRules">
            <a-select-option value="ACTIVE">启用</a-select-option>
            <a-select-option value="DISABLED">停用</a-select-option>
          </a-select>
          <a-input-search v-model="tableKeyword" allow-clear placeholder="搜索表名/库名" style="width: 220px" @search="fetchTables" />
        </div>
        <a-table
          row-key="id"
          :columns="ruleColumns"
          :data-source="rules"
          :loading="loadingRules"
          :scroll="{ x: 1400 }"
          :pagination="{ pageSize: 10 }"
        >
          <template slot="ruleType" slot-scope="text">
            <a-tag color="blue">{{ ruleTypeLabel(text) }}</a-tag>
          </template>
          <template slot="ruleTarget" slot-scope="text, record">
            <div class="primary-text">{{ targetName(record) }}</div>
            <div class="muted-text">{{ record.columnName || '表级规则' }}</div>
          </template>
          <template slot="severity" slot-scope="text">
            <a-tag :color="severityColor(text)">{{ severityLabel(text) }}</a-tag>
          </template>
          <template slot="status" slot-scope="text">
            <a-tag :color="text === 'ACTIVE' ? 'green' : 'default'">{{ text === 'ACTIVE' ? '启用' : '停用' }}</a-tag>
          </template>
          <template slot="lastStatus" slot-scope="text">
            <a-tag :color="statusColor(text)">{{ executionStatusLabel(text) }}</a-tag>
          </template>
          <template slot="lastTime" slot-scope="text">{{ formatTime(text) }}</template>
          <template slot="action" slot-scope="text, record">
            <a-space>
              <a-button type="link" size="small" icon="thunderbolt" :disabled="!canManageQuality || record.status !== 'ACTIVE'" :loading="executingRuleId === record.id" @click="executeRule(record)">
                执行
              </a-button>
              <a-button v-if="canManageQuality" type="link" size="small" icon="edit" @click="showRuleModal(record)">编辑</a-button>
              <a-popconfirm v-if="canManageQuality" title="确认删除这条质量规则？" @confirm="deleteRule(record)">
                <a-button type="link" size="small" icon="delete">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="executions" tab="执行记录">
        <div class="toolbar">
          <a-select v-model="executionFilters.status" allowClear placeholder="执行状态" style="width: 140px" @change="fetchExecutions">
            <a-select-option value="SUCCESS">成功</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
          </a-select>
        </div>
        <a-table
          row-key="id"
          :columns="executionColumns"
          :data-source="executions"
          :loading="loadingExecutions"
          :scroll="{ x: 1500 }"
          :pagination="executionPagination"
          @change="onExecutionTableChange"
        >
          <template slot="status" slot-scope="text">
            <a-tag :color="statusColor(text)">{{ executionStatusLabel(text) }}</a-tag>
          </template>
          <template slot="number" slot-scope="text">{{ formatNumber(text) }}</template>
          <template slot="sql" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="ellipsis">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <template slot="message" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="ellipsis">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="issues" tab="问题清单">
        <div class="toolbar">
          <a-select v-model="issueFilters.status" allowClear placeholder="问题状态" style="width: 140px" @change="fetchIssues">
            <a-select-option value="OPEN">待处理</a-select-option>
            <a-select-option value="RESOLVED">已解决</a-select-option>
          </a-select>
          <a-input v-model="issueFilters.owner" allow-clear placeholder="负责人" style="width: 180px" @pressEnter="fetchIssues" />
          <a-button icon="search" @click="fetchIssues">查询</a-button>
        </div>
        <a-table
          row-key="id"
          :columns="issueColumns"
          :data-source="issues"
          :loading="loadingIssues"
          :scroll="{ x: 1300 }"
          :pagination="issuePagination"
          @change="onIssueTableChange"
        >
          <template slot="status" slot-scope="text">
            <a-tag :color="text === 'OPEN' ? 'red' : 'green'">{{ text === 'OPEN' ? '待处理' : '已解决' }}</a-tag>
          </template>
          <template slot="severity" slot-scope="text">
            <a-tag :color="severityColor(text)">{{ severityLabel(text) }}</a-tag>
          </template>
          <template slot="message" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="ellipsis">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      :title="editingRule ? '编辑质量规则' : '新建质量规则'"
      :visible="ruleModalVisible"
      :confirm-loading="savingRule"
      width="760px"
      @ok="saveRule"
      @cancel="ruleModalVisible = false"
    >
      <a-form-model layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-model-item label="规则名称">
              <a-input v-model="ruleForm.ruleName" placeholder="例如：订单表支付金额非空率" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="表资产">
              <a-select v-model="ruleForm.tableId" show-search option-filter-prop="children" placeholder="选择元数据表" @change="onRuleTableChange">
                <a-select-option v-for="table in tableOptions" :key="table.id" :value="table.id">
                  {{ table.clusterCode || '-' }} / {{ table.dbName }}.{{ table.tableName }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-model-item label="规则类型">
              <a-select v-model="ruleForm.ruleType" @change="onRuleTypeChange">
                <a-select-option v-for="item in ruleTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="8">
            <a-form-model-item label="字段">
              <a-select v-model="ruleForm.columnName" allowClear placeholder="字段级规则选择字段" :disabled="!requiresColumn(ruleForm.ruleType)">
                <a-select-option v-for="column in columnOptions" :key="column.columnName" :value="column.columnName">
                  {{ column.columnName }} / {{ column.columnType || column.dataType || '-' }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="8">
            <a-form-model-item label="扫描范围">
              <a-select v-model="ruleForm.scanScope">
                <a-select-option value="LATEST_PARTITION">最新分区</a-select-option>
                <a-select-option value="FULL_TABLE">全表</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-model-item label="严重级别">
              <a-select v-model="ruleForm.severity">
                <a-select-option value="HIGH">高</a-select-option>
                <a-select-option value="MEDIUM">中</a-select-option>
                <a-select-option value="LOW">低</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="8">
            <a-form-model-item label="状态">
              <a-select v-model="ruleForm.status">
                <a-select-option value="ACTIVE">启用</a-select-option>
                <a-select-option value="DISABLED">停用</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="8">
            <a-form-model-item label="负责人">
              <a-input v-model="ruleForm.owner" placeholder="默认取表负责人" />
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col v-if="usesThreshold(ruleForm.ruleType)" :span="8">
            <a-form-model-item label="失败率阈值">
              <a-input-number v-model="ruleForm.threshold" :min="0" :max="1" :step="0.01" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col v-if="ruleForm.ruleType === 'VALUE_RANGE'" :span="8">
            <a-form-model-item label="最小值">
              <a-input-number v-model="ruleForm.minValue" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col v-if="ruleForm.ruleType === 'VALUE_RANGE'" :span="8">
            <a-form-model-item label="最大值">
              <a-input-number v-model="ruleForm.maxValue" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col v-if="ruleForm.ruleType === 'ROW_COUNT'" :span="8">
            <a-form-model-item label="最小行数">
              <a-input v-model="ruleForm.expectedValue" placeholder="例如：1" />
            </a-form-model-item>
          </a-col>
          <a-col v-if="ruleForm.ruleType === 'FRESHNESS'" :span="8">
            <a-form-model-item label="最大延迟小时">
              <a-input v-model="ruleForm.expectedValue" placeholder="例如：24" />
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-form-model-item v-if="ruleForm.ruleType === 'REGEX_MATCH'" label="正则表达式">
          <a-input v-model="ruleForm.regexPattern" placeholder="例如：^\\d+$" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import { canDelete } from '../utils/currentUser';

export default {
  name: 'Quality',
  data() {
    return {
      activeTab: 'dashboard',
      overview: {},
      dataSources: [],
      tableOptions: [],
      columnOptions: [],
      rules: [],
      executions: [],
      issues: [],
      tableKeyword: '',
      loadingOverview: false,
      loadingRules: false,
      loadingExecutions: false,
      loadingIssues: false,
      savingRule: false,
      executingRuleId: null,
      ruleModalVisible: false,
      editingRule: null,
      ruleFilters: {
        dataSourceId: undefined,
        tableId: undefined,
        status: undefined
      },
      executionFilters: {
        status: undefined,
        page: 1,
        size: 20,
        total: 0
      },
      issueFilters: {
        status: 'OPEN',
        owner: '',
        page: 1,
        size: 20,
        total: 0
      },
      ruleForm: this.emptyRuleForm(),
      ruleTypeOptions: [
        { value: 'NULL_RATE', label: '空值率' },
        { value: 'UNIQUE_RATE', label: '重复率' },
        { value: 'VALUE_RANGE', label: '数值范围' },
        { value: 'ROW_COUNT', label: '行数检查' },
        { value: 'FRESHNESS', label: '新鲜度' },
        { value: 'REGEX_MATCH', label: '正则匹配' }
      ],
      dashboardRuleColumns: [
        { title: '规则', dataIndex: 'ruleName', key: 'ruleName' },
        { title: '对象', key: 'ruleTarget', scopedSlots: { customRender: 'ruleTarget' } },
        { title: '状态', dataIndex: 'lastExecutionStatus', key: 'lastExecutionStatus', scopedSlots: { customRender: 'statusTag' }, width: 100 }
      ],
      dashboardIssueColumns: [
        { title: '问题', dataIndex: 'issueTitle', key: 'issueTitle' },
        { title: '级别', dataIndex: 'severity', key: 'severity', scopedSlots: { customRender: 'severity' }, width: 90 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', width: 120 }
      ],
      ruleColumns: [
        { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 220 },
        { title: '对象', key: 'ruleTarget', scopedSlots: { customRender: 'ruleTarget' }, width: 260 },
        { title: '类型', dataIndex: 'ruleType', key: 'ruleType', scopedSlots: { customRender: 'ruleType' }, width: 120 },
        { title: '级别', dataIndex: 'severity', key: 'severity', scopedSlots: { customRender: 'severity' }, width: 90 },
        { title: '规则状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' }, width: 110 },
        { title: '最近结果', dataIndex: 'lastExecutionStatus', key: 'lastExecutionStatus', scopedSlots: { customRender: 'lastStatus' }, width: 120 },
        { title: '结果值', dataIndex: 'lastResultValue', key: 'lastResultValue', customRender: text => this.formatNumber(text), width: 110 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', width: 120 },
        { title: '最近执行', dataIndex: 'lastExecutedAt', key: 'lastExecutedAt', scopedSlots: { customRender: 'lastTime' }, width: 180 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 220, fixed: 'right' }
      ],
      executionColumns: [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
        { title: '规则ID', dataIndex: 'ruleId', key: 'ruleId', width: 90 },
        { title: '表ID', dataIndex: 'tableId', key: 'tableId', width: 90 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' }, width: 100 },
        { title: '结果值', dataIndex: 'resultValue', key: 'resultValue', scopedSlots: { customRender: 'number' }, width: 110 },
        { title: '阈值', dataIndex: 'threshold', key: 'threshold', scopedSlots: { customRender: 'number' }, width: 100 },
        { title: '扫描条件', dataIndex: 'scanFilter', key: 'scanFilter', scopedSlots: { customRender: 'sql' }, width: 220 },
        { title: '执行SQL', dataIndex: 'executedSql', key: 'executedSql', scopedSlots: { customRender: 'sql' }, width: 260 },
        { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', scopedSlots: { customRender: 'message' }, width: 220 },
        { title: '耗时(ms)', dataIndex: 'durationMs', key: 'durationMs', width: 100 },
        { title: '执行人', dataIndex: 'executedBy', key: 'executedBy', width: 110 },
        { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt', scopedSlots: { customRender: 'time' }, width: 180 }
      ],
      issueColumns: [
        { title: '问题', dataIndex: 'issueTitle', key: 'issueTitle', width: 220 },
        { title: '描述', dataIndex: 'issueDescription', key: 'issueDescription', scopedSlots: { customRender: 'message' }, width: 320 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' }, width: 100 },
        { title: '级别', dataIndex: 'severity', key: 'severity', scopedSlots: { customRender: 'severity' }, width: 90 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', width: 120 },
        { title: '结果值', dataIndex: 'resultValue', key: 'resultValue', customRender: text => this.formatNumber(text), width: 110 },
        { title: '阈值', dataIndex: 'threshold', key: 'threshold', customRender: text => this.formatNumber(text), width: 100 },
        { title: '最近发现', dataIndex: 'lastSeenAt', key: 'lastSeenAt', scopedSlots: { customRender: 'time' }, width: 180 },
        { title: '解决时间', dataIndex: 'resolvedAt', key: 'resolvedAt', scopedSlots: { customRender: 'time' }, width: 180 }
      ]
    };
  },
  computed: {
    canManageQuality() {
      return canDelete();
    },
    failedRules() {
      return this.rules.filter(item => item.lastExecutionStatus === 'FAILED').slice(0, 6);
    },
    openIssues() {
      return this.issues.filter(item => item.status === 'OPEN').slice(0, 6);
    },
    executionPagination() {
      return {
        current: this.executionFilters.page,
        pageSize: this.executionFilters.size,
        total: this.executionFilters.total,
        showTotal: total => `共 ${total} 条`
      };
    },
    issuePagination() {
      return {
        current: this.issueFilters.page,
        pageSize: this.issueFilters.size,
        total: this.issueFilters.total,
        showTotal: total => `共 ${total} 条`
      };
    }
  },
  mounted() {
    if (this.$route.query.tableId) {
      this.ruleFilters.tableId = Number(this.$route.query.tableId);
      this.activeTab = 'rules';
    }
    this.refreshAll();
    this.fetchDataSources();
    this.fetchTables();
  },
  methods: {
    emptyRuleForm() {
      return {
        ruleName: '',
        tableId: undefined,
        columnName: undefined,
        ruleType: 'NULL_RATE',
        severity: 'MEDIUM',
        status: 'ACTIVE',
        scanScope: 'LATEST_PARTITION',
        threshold: 0,
        expectedValue: '',
        minValue: undefined,
        maxValue: undefined,
        regexPattern: '',
        owner: '',
        actionType: 'ALARM'
      };
    },
    async refreshAll() {
      await Promise.all([
        this.fetchOverview(),
        this.fetchRules(),
        this.fetchExecutions(),
        this.fetchIssues()
      ]);
    },
    async fetchOverview() {
      this.loadingOverview = true;
      try {
        const res = await axios.get('/api/quality/overview');
        this.overview = res.data || {};
      } catch (e) {
        this.$message.error('加载质量概览失败');
      } finally {
        this.loadingOverview = false;
      }
    },
    async fetchDataSources() {
      try {
        const res = await axios.get('/api/datasource');
        this.dataSources = (res.data || []).filter(item => String(item.type || '').toUpperCase() === 'HIVE');
      } catch (e) {
        this.$message.error('加载数据源失败');
      }
    },
    async fetchTables() {
      try {
        const params = {
          page: 0,
          size: 100,
          keyword: this.tableKeyword || undefined,
          dataSourceId: this.ruleFilters.dataSourceId || undefined
        };
        const res = await axios.get('/api/metadata/search', { params });
        this.tableOptions = res.data?.content || [];
        if (this.ruleFilters.tableId && !this.tableOptions.some(item => item.id === this.ruleFilters.tableId)) {
          const tableRes = await axios.get(`/api/metadata/table/${this.ruleFilters.tableId}`);
          if (tableRes.data) {
            this.tableOptions = [tableRes.data].concat(this.tableOptions);
          }
        }
      } catch (e) {
        this.$message.error('加载元数据表失败');
      }
    },
    async fetchColumns(tableId) {
      if (!tableId) {
        this.columnOptions = [];
        return;
      }
      try {
        const res = await axios.get(`/api/metadata/table/${tableId}/columns`);
        this.columnOptions = res.data || [];
      } catch (e) {
        this.columnOptions = [];
        this.$message.error('加载字段失败');
      }
    },
    async fetchRules() {
      this.loadingRules = true;
      try {
        const res = await axios.get('/api/quality/rules', { params: this.cleanParams(this.ruleFilters) });
        this.rules = res.data || [];
      } catch (e) {
        this.$message.error('加载质量规则失败');
      } finally {
        this.loadingRules = false;
      }
    },
    async fetchExecutions() {
      this.loadingExecutions = true;
      try {
        const params = this.cleanParams({
          status: this.executionFilters.status,
          page: this.executionFilters.page - 1,
          size: this.executionFilters.size
        });
        const res = await axios.get('/api/quality/executions', { params });
        this.executions = res.data?.content || [];
        this.executionFilters.total = res.data?.totalElements || 0;
      } catch (e) {
        this.$message.error('加载执行记录失败');
      } finally {
        this.loadingExecutions = false;
      }
    },
    async fetchIssues() {
      this.loadingIssues = true;
      try {
        const params = this.cleanParams({
          status: this.issueFilters.status,
          owner: this.issueFilters.owner,
          page: this.issueFilters.page - 1,
          size: this.issueFilters.size
        });
        const res = await axios.get('/api/quality/issues', { params });
        this.issues = res.data?.content || [];
        this.issueFilters.total = res.data?.totalElements || 0;
      } catch (e) {
        this.$message.error('加载质量问题失败');
      } finally {
        this.loadingIssues = false;
      }
    },
    onDataSourceFilterChange() {
      this.ruleFilters.tableId = undefined;
      this.fetchTables();
      this.fetchRules();
    },
    onExecutionTableChange(pagination) {
      this.executionFilters.page = pagination.current;
      this.executionFilters.size = pagination.pageSize;
      this.fetchExecutions();
    },
    onIssueTableChange(pagination) {
      this.issueFilters.page = pagination.current;
      this.issueFilters.size = pagination.pageSize;
      this.fetchIssues();
    },
    async showRuleModal(record) {
      this.editingRule = record || null;
      this.ruleForm = record ? {
        ruleName: record.ruleName || '',
        tableId: record.tableId,
        columnName: record.columnName || undefined,
        ruleType: record.ruleType || 'NULL_RATE',
        severity: record.severity || 'MEDIUM',
        status: record.status || 'ACTIVE',
        scanScope: record.scanScope || 'LATEST_PARTITION',
        threshold: record.threshold === null || record.threshold === undefined ? 0 : record.threshold,
        expectedValue: record.expectedValue || '',
        minValue: record.minValue,
        maxValue: record.maxValue,
        regexPattern: record.regexPattern || '',
        owner: record.owner || '',
        actionType: record.actionType || 'ALARM'
      } : this.emptyRuleForm();
      await this.fetchColumns(this.ruleForm.tableId);
      this.ruleModalVisible = true;
    },
    async onRuleTableChange(tableId) {
      await this.fetchColumns(tableId);
      this.ruleForm.columnName = undefined;
      const table = this.tableOptions.find(item => item.id === tableId);
      if (table && !this.ruleForm.owner) {
        this.ruleForm.owner = table.owner || table.sourceOwner || '';
      }
    },
    onRuleTypeChange(type) {
      if (!this.requiresColumn(type)) {
        this.ruleForm.columnName = undefined;
      }
      if (!this.usesThreshold(type)) {
        this.ruleForm.threshold = undefined;
      } else if (this.ruleForm.threshold === undefined || this.ruleForm.threshold === null) {
        this.ruleForm.threshold = 0;
      }
    },
    async saveRule() {
      if (!this.ruleForm.tableId) {
        this.$message.warning('请选择元数据表');
        return;
      }
      if (this.requiresColumn(this.ruleForm.ruleType) && !this.ruleForm.columnName) {
        this.$message.warning('请选择字段');
        return;
      }
      this.savingRule = true;
      try {
        if (this.editingRule) {
          await axios.put(`/api/quality/rules/${this.editingRule.id}`, this.ruleForm);
        } else {
          await axios.post('/api/quality/rules', this.ruleForm);
        }
        this.$message.success('质量规则已保存');
        this.ruleModalVisible = false;
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存质量规则失败');
      } finally {
        this.savingRule = false;
      }
    },
    async executeRule(record) {
      this.executingRuleId = record.id;
      try {
        const res = await axios.post(`/api/quality/execute/${record.id}`);
        const status = res.data?.status;
        if (status === 'SUCCESS') {
          this.$message.success('规则执行成功');
        } else {
          this.$message.warning(res.data?.errorMessage || '规则执行失败，已生成问题记录');
        }
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '执行质量规则失败');
      } finally {
        this.executingRuleId = null;
      }
    },
    async deleteRule(record) {
      try {
        await axios.delete(`/api/quality/rules/${record.id}`);
        this.$message.success('质量规则已删除');
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '删除质量规则失败');
      }
    },
    targetName(record) {
      const db = record.dbName || '-';
      const table = record.tableName || record.tableId || '-';
      return `${db}.${table}`;
    },
    requiresColumn(type) {
      return ['NULL_RATE', 'UNIQUE_RATE', 'VALUE_RANGE', 'REGEX_MATCH'].includes(type);
    },
    usesThreshold(type) {
      return ['NULL_RATE', 'UNIQUE_RATE', 'VALUE_RANGE', 'REGEX_MATCH'].includes(type);
    },
    ruleTypeLabel(type) {
      const found = this.ruleTypeOptions.find(item => item.value === type);
      return found ? found.label : (type || '-');
    },
    executionStatusLabel(status) {
      if (status === 'SUCCESS') return '成功';
      if (status === 'FAILED') return '失败';
      return status || '未执行';
    },
    statusColor(status) {
      if (status === 'SUCCESS') return 'green';
      if (status === 'FAILED') return 'red';
      return 'default';
    },
    severityColor(severity) {
      if (severity === 'HIGH') return 'red';
      if (severity === 'LOW') return 'blue';
      return 'orange';
    },
    severityLabel(severity) {
      if (severity === 'HIGH') return '高';
      if (severity === 'LOW') return '低';
      return '中';
    },
    formatTime(value) {
      return value ? new Date(value).toLocaleString() : '-';
    },
    formatNumber(value) {
      if (value === null || value === undefined || value === '') return '-';
      const number = Number(value);
      if (Number.isNaN(number)) return value;
      return Math.round(number * 10000) / 10000;
    },
    formatPercent(value) {
      if (value === null || value === undefined) return '0%';
      return `${value}%`;
    },
    cleanParams(params) {
      const result = {};
      Object.keys(params).forEach(key => {
        const value = params[key];
        if (value !== undefined && value !== null && value !== '') {
          result[key] = value;
        }
      });
      return result;
    }
  }
};
</script>

<style scoped>
.quality-view {
  background: #fff;
  min-height: 100%;
  padding: 24px;
  border-radius: 4px;
}
.module-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}
.module-header h1 {
  margin: 0 0 6px;
}
.module-header p,
.muted-text {
  color: #667085;
  margin: 0;
}
.header-actions,
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar {
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.summary-item,
.panel {
  border: 1px solid #edf0f5;
  border-radius: 4px;
  background: #fafcff;
}
.summary-item {
  padding: 16px;
}
.summary-item span {
  display: block;
  color: #667085;
}
.summary-item strong {
  display: block;
  margin-top: 8px;
  color: #24364b;
  font-size: 28px;
}
.danger-text {
  color: #cf1322 !important;
}
.warning-text {
  color: #d46b08 !important;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.panel {
  padding: 16px;
}
.panel-title {
  margin-bottom: 12px;
  color: #24364b;
  font-weight: 600;
}
.primary-text {
  color: #24364b;
  font-weight: 500;
}
.ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}
</style>
