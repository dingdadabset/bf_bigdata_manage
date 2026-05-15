<template>
  <div class="governance-panel">
    <div class="module-header">
      <div>
        <h1>权限治理风险</h1>
        <p>按授权明细识别角色基线外权限、高权限复核、无人负责权限，并提醒长期空置 LDAP 组；未使用权限仅在接入精确审计后判定</p>
      </div>
      <div class="header-actions">
        <a-select
          v-model="scanForm.sourceSystems"
          mode="multiple"
          size="small"
          placeholder="审计来源"
          style="min-width: 360px"
        >
          <a-select-option v-for="source in sourceOptions" :key="source.value" :value="source.value">
            {{ source.label }}
          </a-select-option>
        </a-select>
        <a-input-number v-model="scanForm.inactiveDays" :min="1" :max="365" size="small" />
        <span class="toolbar-label">未使用判定天数</span>
        <a-input-number v-model="scanForm.reviewDays" :min="1" :max="365" size="small" />
        <span class="toolbar-label">复核周期</span>
        <a-button icon="profile" @click="ruleModalVisible = true">规则示例</a-button>
        <a-button icon="reload" :loading="loadingIssues || loadingSummary" @click="refreshAll">刷新</a-button>
        <a-popconfirm
          :title="clearConfirmTitle"
          ok-text="清空"
          cancel-text="取消"
          @confirm="clearResults"
        >
          <a-button type="danger" icon="delete" :loading="clearingResults">清空结果</a-button>
        </a-popconfirm>
        <a-button type="primary" icon="security-scan" :loading="scanning" @click="scanRisks">扫描风险</a-button>
      </div>
    </div>

    <div class="source-strip">
      <span>审计来源</span>
      <a-tooltip v-for="source in sourceStatuses" :key="source.sourceSystem" :title="source.message || '-'">
        <a-tag :color="source.configured ? sourceColor(source.sourceSystem) : 'default'">
          {{ source.label || source.sourceSystem }} · {{ source.configured ? '已接入' : '未接入' }}
        </a-tag>
      </a-tooltip>
    </div>

    <a-alert
      v-if="showHiveUnusedWarning"
      class="capability-alert"
      type="warning"
      show-icon
      message="HiveServer2 不能单独判断长期未使用权限"
      description="当前 HiveServer2 只能提供授权明细。未使用权限需要 Ranger Audit DB、Hive 审计表或 StarRocks/Doris 审计表这类可按 user + db/table + operation 匹配的精确审计来源；HDFS/YARN 仅作为账号级辅助活动证据，不会单独生成未使用权限风险。"
    />

    <div v-if="lastScanResult" class="scan-result-panel">
      <div class="scan-result-header">
        <div>
          <strong>最近扫描结果</strong>
          <span>{{ formatTime(lastScanResult.scannedAt) }}</span>
        </div>
        <a-tag color="blue">{{ lastScanResult.cluster || currentCluster || '全部集群' }}</a-tag>
      </div>
      <div class="scan-result-metrics">
        <div>
          <span>新增风险</span>
          <strong>{{ lastScanResult.created || 0 }}</strong>
        </div>
        <div>
          <span>刷新风险</span>
          <strong>{{ lastScanResult.refreshed || 0 }}</strong>
        </div>
        <div>
          <span>活动证据</span>
          <strong>{{ lastScanResult.evidenceCount || 0 }}</strong>
        </div>
        <div>
          <span>权限审计覆盖</span>
          <strong>{{ lastScanResult.permissionUsageCoverageCount || 0 }}</strong>
        </div>
        <div>
          <span>未使用判定天数</span>
          <strong>{{ lastScanResult.inactiveDays || scanForm.inactiveDays }}</strong>
        </div>
      </div>
      <div v-if="lastScanResult.unusedPermissionCapability" class="scan-capability-note">
        {{ lastScanResult.unusedPermissionCapability }}
      </div>
      <div class="scan-result-sources">
        <a-tooltip
          v-for="source in scanResultSources"
          :key="source.sourceSystem"
          :title="source.message || '-'"
        >
          <a-tag :color="scanSourceColor(source)">
            {{ source.label || sourceLabel(source.sourceSystem) }} · {{ scanSourceText(source) }}
          </a-tag>
        </a-tooltip>
      </div>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span>未闭环风险</span>
        <strong>{{ summary.total || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>未使用权限</span>
        <strong>{{ typeCount('UNUSED_DATABASE_PERMISSION') }}</strong>
      </div>
      <div class="summary-item">
        <span>超出角色</span>
        <strong>{{ typeCount('ROLE_BASELINE_EXCEEDED') }}</strong>
      </div>
      <div class="summary-item">
        <span>高权限复核</span>
        <strong>{{ typeCount('HIGH_PRIVILEGE_REVIEW') }}</strong>
      </div>
      <div class="summary-item">
        <span>无人负责权限</span>
        <strong>{{ typeCount('UNOWNED_PERMISSION') }}</strong>
      </div>
      <div class="summary-item">
        <span>长期空组</span>
        <strong>{{ typeCount('STALE_EMPTY_LDAP_GROUP') }}</strong>
      </div>
    </div>

    <div class="toolbar">
      <a-select
        v-model="filters.issueTypes"
        mode="multiple"
        allowClear
        placeholder="风险类型"
        style="width: 280px"
        @change="fetchIssues"
      >
        <a-select-option v-for="item in issueTypeOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </a-select-option>
      </a-select>
      <a-select v-model="filters.status" placeholder="状态" style="width: 140px" @change="fetchIssues">
        <a-select-option value="OPEN">待处理</a-select-option>
        <a-select-option value="CLAIMED">已认领</a-select-option>
        <a-select-option value="RESOLVED">已关闭</a-select-option>
        <a-select-option value="ALL">全部</a-select-option>
      </a-select>
      <a-input-search v-model="filters.username" allow-clear placeholder="搜索账号 / 组" style="width: 220px" @search="fetchIssues" />
      <a-tag v-if="currentCluster" color="blue">当前集群：{{ currentCluster }}</a-tag>
    </div>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="issues"
      :loading="loadingIssues"
      :pagination="pagination"
      :scroll="{ x: 1780 }"
      @change="onTableChange"
    >
      <template slot="issueType" slot-scope="text">
        <a-tag :color="issueTypeColor(text)">{{ issueTypeLabel(text) }}</a-tag>
      </template>
      <template slot="severity" slot-scope="text">
        <a-tag :color="severityColor(text)">{{ severityLabel(text) }}</a-tag>
      </template>
      <template slot="status" slot-scope="text">
        <a-tag :color="statusColor(text)">{{ statusLabel(text) }}</a-tag>
      </template>
      <template slot="resource" slot-scope="text, record">
        <div class="primary-text">{{ record.username || '-' }}</div>
        <div class="muted-text">{{ resourceName(record) }}</div>
      </template>
      <template slot="permission" slot-scope="text">
        <a-tag v-if="text" color="green">{{ text }}</a-tag>
        <span v-else>-</span>
      </template>
      <template slot="owners" slot-scope="text, record">
        <div class="primary-text">{{ record.owner || '-' }}</div>
        <div v-if="record.collaboratorOwners" class="muted-text">
          协同：{{ record.collaboratorOwners }}
        </div>
      </template>
      <template slot="activity" slot-scope="text, record">
        <div class="primary-text">{{ record.lastActiveSource || '-' }}</div>
        <div class="muted-text">{{ formatTime(record.lastActiveAt) }}</div>
        <a-tag v-if="record.confidence" :color="confidenceColor(record.confidence)" class="confidence-tag">
          {{ confidenceLabel(record.confidence) }}
        </a-tag>
      </template>
      <template slot="sources" slot-scope="text">
        <a-tag v-for="source in sourceTags(text)" :key="source" :color="sourceColor(source)">
          {{ source }}
        </a-tag>
      </template>
      <template slot="message" slot-scope="text">
        <a-tooltip v-if="text" :title="text">
          <span class="ellipsis">{{ text }}</span>
        </a-tooltip>
        <span v-else>-</span>
      </template>
      <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
      <template slot="action" slot-scope="text, record">
        <a-space>
          <a-button
            v-if="record.issueType === 'UNOWNED_PERMISSION' && record.status !== 'RESOLVED'"
            type="link"
            size="small"
            icon="user-add"
            @click="openAction('owner', record)"
          >
            认领
          </a-button>
          <a-button
            v-if="record.issueType === 'HIGH_PRIVILEGE_REVIEW' && record.status !== 'RESOLVED'"
            type="link"
            size="small"
            icon="audit"
            @click="openAction('review', record)"
          >
            已复核
          </a-button>
          <a-button
            v-if="record.status !== 'RESOLVED'"
            type="link"
            size="small"
            icon="team"
            @click="openAction('claim', record)"
          >
            指派
          </a-button>
          <a-popconfirm
            v-if="record.status !== 'RESOLVED'"
            title="确认关闭这条风险？"
            @confirm="resolveIssue(record)"
          >
            <a-button type="link" size="small" icon="check">关闭</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </a-table>

    <a-modal
      :visible="actionModalVisible"
      :title="actionTitle"
      :confirm-loading="savingAction"
      @ok="submitAction"
      @cancel="actionModalVisible = false"
    >
      <a-form-model layout="vertical">
        <template v-if="actionMode === 'owner' || actionMode === 'claim'">
          <a-form-model-item label="主负责人">
            <a-select
              v-model="actionForm.owner"
              showSearch
              mode="combobox"
              :filterOption="false"
              placeholder="搜索平台用户 / 负责人，或直接输入新负责人"
              @search="fetchOwners"
              @focus="fetchOwners('')"
            >
              <a-select-option v-for="owner in ownerOptions" :key="owner.ownerCode" :value="owner.ownerCode">
                {{ owner.label || owner.ownerCode }}
                <span class="select-extra">{{ owner.source || 'MANUAL' }}</span>
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="协同负责人">
            <a-select
              v-model="actionForm.collaboratorOwners"
              showSearch
              mode="tags"
              :filterOption="false"
              placeholder="可选择多个协同负责人"
              @search="fetchOwners"
              @focus="fetchOwners('')"
            >
              <a-select-option v-for="owner in ownerOptions" :key="owner.ownerCode" :value="owner.ownerCode">
                {{ owner.label || owner.ownerCode }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <div class="owner-create-box">
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-model-item label="显示名">
                  <a-input v-model="actionForm.ownerDisplayName" placeholder="可选，用于新负责人档案" />
                </a-form-model-item>
              </a-col>
              <a-col :span="12">
                <a-form-model-item label="邮箱">
                  <a-input v-model="actionForm.ownerEmail" placeholder="可选" />
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-button size="small" icon="plus" :loading="creatingOwner" @click="createOwnerProfile">
              创建负责人档案
            </a-button>
          </div>
        </template>
        <a-form-model-item v-if="actionMode === 'review'" label="下次复核周期">
          <a-input-number v-model="actionForm.reviewDays" :min="1" :max="365" style="width: 100%" />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-textarea v-model="actionForm.comment" :rows="3" placeholder="可选" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal
      :visible="ruleModalVisible"
      title="数据库权限治理规则与审计来源示例"
      width="780px"
      :footer="null"
      @cancel="ruleModalVisible = false"
    >
      <div class="rule-modal">
        <div v-for="section in governanceRuleSections" :key="section.engine" class="rule-section">
          <div class="rule-title">
            <strong>{{ section.engine }}</strong>
            <a-tag :color="sourceColor(section.sourceSystem)">{{ section.sourceSystem }}</a-tag>
          </div>
          <div class="rule-line">
            <span>采集来源</span>
            <p>{{ section.source }}</p>
          </div>
          <div class="rule-line">
            <span>治理规则</span>
            <p>{{ section.rules }}</p>
          </div>
          <div class="rule-line">
            <span>证据示例</span>
            <p>{{ section.example }}</p>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import moment from 'moment';
import { store } from '../../../store';

export default {
  name: 'AccessGovernancePanel',
  data() {
    const sourceOptions = [
      { value: 'LDAP', label: 'OpenLDAP' },
      { value: 'HIVE_SERVER2', label: 'HiveServer2' },
      { value: 'RANGER', label: 'Ranger Audit' },
      { value: 'HDFS', label: 'HDFS Audit' },
      { value: 'YARN', label: 'YARN History' },
      { value: 'STARROCKS', label: 'StarRocks' },
      { value: 'DORIS', label: 'Doris' }
    ];
    return {
      store,
      sourceOptions,
      summary: {},
      issues: [],
      ownerOptions: [],
      loadingSummary: false,
      loadingIssues: false,
      scanning: false,
      lastScanResult: null,
      savingAction: false,
      creatingOwner: false,
      clearingResults: false,
      actionModalVisible: false,
      ruleModalVisible: false,
      actionMode: '',
      actionRecord: null,
      actionForm: {
        owner: '',
        ownerDisplayName: '',
        ownerEmail: '',
        collaboratorOwners: [],
        reviewDays: 90,
        comment: ''
      },
      scanForm: {
        sourceSystems: sourceOptions.map(item => item.value),
        inactiveDays: 90,
        reviewDays: 90
      },
      filters: {
        issueTypes: [],
        status: 'OPEN',
        username: ''
      },
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: total => `共 ${total} 条`
      },
      issueTypeOptions: [
        { value: 'UNUSED_DATABASE_PERMISSION', label: '未使用数据库权限' },
        { value: 'ROLE_BASELINE_EXCEEDED', label: '超出角色权限' },
        { value: 'HIGH_PRIVILEGE_REVIEW', label: '高权限复核' },
        { value: 'UNOWNED_PERMISSION', label: '无人负责权限' },
        { value: 'STALE_EMPTY_LDAP_GROUP', label: '长期空组' }
      ],
      governanceRuleSections: [
        {
          engine: 'Hive',
          sourceSystem: 'HIVE_SERVER2',
          source: 'Hive 强证据来自 Ranger Audit MySQL x_access_audit/xa_access_audit 或 HiveServer2 audit/query log；HDP 可配置 RANGER_DB 端点直接读取 Ranger 审计库。',
          rules: 'HiveServer2 授权明细只说明账号拥有哪些权限，不能说明是否长期未使用。扫描会同时将账号实际权限与其有效 RBAC 角色范围比对，识别角色基线外权限。',
          example: '用户有 tmp_db 的 SELECT 权限：若仅采集 HiveServer2 授权明细，只生成高权限复核/无人负责类风险；若 Ranger/Hive 审计最近 N 天无 select/query 命中，才生成未使用数据库权限。'
        },
        {
          engine: 'Hive 辅助来源',
          sourceSystem: 'HDFS',
          source: 'HDFS Audit 可通过 HDFS 端点服务名配置规范化审计表/视图，字段建议 user/event_time/path；没有审计表时仅尝试 WebHDFS 用户目录活动作为低置信度辅助。',
          rules: 'HDFS 只能说明账号有文件层访问痕迹，不能直接替代 Ranger/HiveServer2 的权限动作匹配。',
          example: '审计表显示用户 5 天前访问过仓库路径，会作为辅助证据展示；但如果 SELECT 权限在 Ranger/HiveServer2 审计中长期无命中，仍需要复核该权限。'
        },
        {
          engine: 'Hive 作业来源',
          sourceSystem: 'YARN',
          source: 'YARN application history 可通过 YARN REST 或规范化 history 表/视图采集，字段建议 user/event_time/application_id。',
          rules: 'YARN 只能说明账号近期提交过作业，不能精确证明某个数据库权限被使用；扫描会把它作为低置信度辅助依据。',
          example: '用户近期有 Hive/Tez 作业但无 Ranger/HiveServer2 权限动作命中时，风险证据会提示 YARN 最近活动，方便人工判断是否误报。'
        },
        {
          engine: 'StarRocks',
          sourceSystem: 'STARROCKS',
          source: '直接通过 StarRocks JDBC 查询审计表，默认 starrocks_audit_db__.starrocks_audit_tbl__，也可在端点服务名填写自定义审计表名。',
          rules: 'StarRocks 按 user + db + stmt 解析权限动作；SELECT/WITH/SHOW 匹配 SELECT，INSERT/LOAD/UPDATE 匹配 INSERT，CREATE/ALTER/DROP 分别匹配对应权限，并展示最后使用距今天数。',
          example: '账号有 db1 的 INSERT 权限，但 StarRocks 审计表最近 N 天没有 user=该用户、db=db1、stmt 以 insert/load/update 开头的记录，则生成未使用数据库权限并显示 INSERT 已多久未使用。'
        },
        {
          engine: 'Doris',
          sourceSystem: 'DORIS',
          source: '直接通过 Doris JDBC 查询审计表，默认 doris_audit_db__.doris_audit_tbl__，也可在端点服务名填写自定义审计表名。',
          rules: 'Doris 与 StarRocks 一样按 user + db + stmt 解析权限动作，但权限归一仍保留 Doris 语义：SELECT_PRIV=SELECT，LOAD_PRIV=INSERT。',
          example: 'test 有 SELECT_PRIV 和 LOAD_PRIV；若 Doris 审计表最近 N 天只有 select 没有 load/insert，则只把 INSERT 视为未使用权限，并显示 LOAD/INSERT 最后使用时间。'
        }
      ],
      columns: [
        { title: '风险类型', dataIndex: 'issueType', key: 'issueType', scopedSlots: { customRender: 'issueType' }, width: 150 },
        { title: '级别', dataIndex: 'severity', key: 'severity', scopedSlots: { customRender: 'severity' }, width: 90 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' }, width: 100 },
        { title: '账号 / 资源', key: 'resource', scopedSlots: { customRender: 'resource' }, width: 250 },
        { title: '权限', dataIndex: 'permission', key: 'permission', scopedSlots: { customRender: 'permission' }, width: 110 },
        { title: '负责人', key: 'owners', scopedSlots: { customRender: 'owners' }, width: 190 },
        { title: '最近使用', key: 'activity', scopedSlots: { customRender: 'activity' }, width: 180 },
        { title: '来源', dataIndex: 'sourceSystems', key: 'sourceSystems', scopedSlots: { customRender: 'sources' }, width: 220 },
        { title: '证据', dataIndex: 'evidence', key: 'evidence', scopedSlots: { customRender: 'message' }, width: 300 },
        { title: '建议', dataIndex: 'recommendation', key: 'recommendation', scopedSlots: { customRender: 'message' }, width: 300 },
        { title: '发现时间', dataIndex: 'detectedAt', key: 'detectedAt', scopedSlots: { customRender: 'time' }, width: 170 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 230, fixed: 'right' }
      ]
    };
  },
  computed: {
    currentCluster() {
      return this.store.headerSelectedCluster || this.store.currentCluster || '';
    },
    sourceStatuses() {
      const statuses = this.summary.sourceStatuses || [];
      const bySource = {};
      statuses.forEach(item => {
        bySource[item.sourceSystem] = item;
      });
      return this.sourceOptions.map(option => ({
        sourceSystem: option.value,
        label: option.label,
        configured: option.value === 'DGA',
        message: option.value === 'DGA' ? '使用平台登录和 DGA 用户时间兜底' : '未接入 endpoint',
        ...(bySource[option.value] || {})
      }));
    },
    directPermissionAuditReady() {
      const selected = this.scanForm.sourceSystems || [];
      return this.sourceStatuses.some(source => {
        if (!selected.includes(source.sourceSystem) || !source.configured) {
          return false;
        }
        const message = source.message || '';
        if (source.sourceSystem === 'STARROCKS' || source.sourceSystem === 'DORIS') {
          return true;
        }
        if (source.sourceSystem === 'RANGER') {
          return message.includes('Ranger Audit MySQL');
        }
        if (source.sourceSystem === 'HIVE_SERVER2') {
          return message.includes('直接查询');
        }
        return false;
      });
    },
    showHiveUnusedWarning() {
      const selected = this.scanForm.sourceSystems || [];
      const hiveSelected = selected.includes('HIVE_SERVER2');
      return hiveSelected && !this.directPermissionAuditReady;
    },
    scanResultSources() {
      const statuses = this.lastScanResult?.sourceStatuses || [];
      return statuses.map(source => ({
        ...source,
        label: source.label || this.sourceLabel(source.sourceSystem)
      }));
    },
    actionTitle() {
      if (this.actionMode === 'owner') return '按账号认领权限负责人';
      if (this.actionMode === 'review') return '记录高权限复核';
      return '指派风险处理人';
    },
    clearConfirmTitle() {
      const scope = this.currentCluster ? `${this.currentCluster} 集群` : '全部集群';
      return `确认清空 ${scope} 的治理风险和活动证据缓存？账号、权限和负责人不会删除。`;
    }
  },
  watch: {
    currentCluster() {
      this.pagination.current = 1;
      this.refreshAll();
    }
  },
  mounted() {
    this.refreshAll();
  },
  methods: {
    async refreshAll() {
      await Promise.all([this.fetchSummary(), this.fetchIssues()]);
    },
    async fetchSummary() {
      this.loadingSummary = true;
      try {
        const res = await axios.get('/api/access/governance/summary', {
          params: this.cleanParams({ cluster: this.currentCluster })
        });
        this.summary = res.data || {};
      } catch (e) {
        this.$message.error('加载治理概览失败');
      } finally {
        this.loadingSummary = false;
      }
    },
    async fetchIssues() {
      this.loadingIssues = true;
      try {
        const res = await axios.get('/api/access/governance/issues', {
          params: this.cleanParams({
            cluster: this.currentCluster,
            issueTypes: this.filters.issueTypes.join(','),
            status: this.filters.status,
            username: this.filters.username,
            page: this.pagination.current - 1,
            size: this.pagination.pageSize
          })
        });
        this.issues = res.data?.content || [];
        this.pagination.total = res.data?.totalElements || 0;
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载治理问题失败');
      } finally {
        this.loadingIssues = false;
      }
    },
    async scanRisks() {
      if (!this.scanForm.sourceSystems.length) {
        this.$message.warning('请选择至少一个审计来源');
        return;
      }
      this.scanning = true;
      try {
        const res = await axios.post('/api/access/governance/scan', null, {
          params: this.cleanParams({
            cluster: this.currentCluster,
            inactiveDays: this.scanForm.inactiveDays,
            reviewDays: this.scanForm.reviewDays,
            sources: this.scanForm.sourceSystems.join(',')
          })
        });
        const data = res.data || {};
        this.lastScanResult = data;
        this.$message.success(`扫描完成：新增 ${data.created || 0}，刷新 ${data.refreshed || 0}`);
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '扫描失败');
      } finally {
        this.scanning = false;
      }
    },
    async clearResults() {
      this.clearingResults = true;
      try {
        const res = await axios.delete('/api/access/governance/results', {
          params: this.cleanParams({
            cluster: this.currentCluster,
            includeEvidence: true
          })
        });
        const data = res.data || {};
        this.$message.success(`已清理：风险 ${data.deletedIssues || 0} 条，证据 ${data.deletedEvidence || 0} 条`);
        this.pagination.current = 1;
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '清理治理结果失败');
      } finally {
        this.clearingResults = false;
      }
    },
    openAction(mode, record) {
      this.actionMode = mode;
      this.actionRecord = record;
      this.actionForm = {
        owner: record.owner || record.assignee || '',
        ownerDisplayName: '',
        ownerEmail: '',
        collaboratorOwners: this.ownerList(record.collaboratorOwners),
        reviewDays: this.scanForm.reviewDays,
        comment: ''
      };
      this.actionModalVisible = true;
      this.fetchOwners(this.actionForm.owner);
    },
    async fetchOwners(query) {
      try {
        const res = await axios.get('/api/access/governance/owners', {
          params: this.cleanParams({
            q: query,
            cluster: this.currentCluster
          })
        });
        this.ownerOptions = res.data || [];
      } catch (e) {
        this.ownerOptions = [];
      }
    },
    async createOwnerProfile() {
      if (!this.actionForm.owner || !this.actionForm.owner.trim()) {
        this.$message.warning('请先填写主负责人账号');
        return;
      }
      this.creatingOwner = true;
      try {
        await axios.post('/api/access/governance/owners', {
          ownerCode: this.actionForm.owner.trim(),
          displayName: this.actionForm.ownerDisplayName,
          email: this.actionForm.ownerEmail,
          source: 'MANUAL'
        });
        this.$message.success('负责人档案已创建');
        await this.fetchOwners(this.actionForm.owner);
      } catch (e) {
        this.$message.error(e.response?.data?.message || '创建负责人失败');
      } finally {
        this.creatingOwner = false;
      }
    },
    async submitAction() {
      if ((this.actionMode === 'owner' || this.actionMode === 'claim') && !this.actionForm.owner.trim()) {
        this.$message.warning('请输入主负责人');
        return;
      }
      this.savingAction = true;
      try {
        const record = this.actionRecord;
        if (this.actionMode === 'owner') {
          if (record.accessId) {
            await axios.put(`/api/access/governance/permissions/${record.accessId}/owner`, this.actionForm);
          } else {
            await axios.put(`/api/access/governance/issues/${record.id}/claim`, this.actionForm);
          }
          this.$message.success('已按账号认领权限负责人');
        } else if (this.actionMode === 'review') {
          if (record.accessId) {
            await axios.put(`/api/access/governance/permissions/${record.accessId}/review`, this.actionForm);
          } else {
            await axios.put(`/api/access/governance/issues/${record.id}/review`, this.actionForm);
          }
          this.$message.success('已记录复核');
        } else {
          await axios.put(`/api/access/governance/issues/${record.id}/claim`, this.actionForm);
          this.$message.success('已指派处理人');
        }
        this.actionModalVisible = false;
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存失败');
      } finally {
        this.savingAction = false;
      }
    },
    async resolveIssue(record) {
      try {
        await axios.put(`/api/access/governance/issues/${record.id}/resolve`);
        this.$message.success('已关闭');
        await this.refreshAll();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '关闭失败');
      }
    },
    onTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.fetchIssues();
    },
    typeCount(type) {
      return (this.summary.byType && this.summary.byType[type]) || 0;
    },
    issueTypeLabel(type) {
      const item = this.issueTypeOptions.find(option => option.value === type);
      return item ? item.label : (type || '-');
    },
    issueTypeColor(type) {
      const colors = {
        UNUSED_DATABASE_PERMISSION: 'orange',
        ROLE_BASELINE_EXCEEDED: 'red',
        HIGH_PRIVILEGE_REVIEW: 'red',
        UNOWNED_PERMISSION: 'geekblue',
        STALE_EMPTY_LDAP_GROUP: 'volcano'
      };
      return colors[type] || 'default';
    },
    severityLabel(text) {
      return { HIGH: '高', MEDIUM: '中', LOW: '低' }[text] || text || '-';
    },
    severityColor(text) {
      return { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }[text] || 'default';
    },
    statusLabel(text) {
      return { OPEN: '待处理', CLAIMED: '已认领', RESOLVED: '已关闭' }[text] || text || '-';
    },
    statusColor(text) {
      return { OPEN: 'red', CLAIMED: 'blue', RESOLVED: 'green' }[text] || 'default';
    },
    confidenceLabel(text) {
      return { HIGH: '高置信', MEDIUM: '中置信', LOW: '低置信' }[text] || text || '-';
    },
    confidenceColor(text) {
      return { HIGH: 'green', MEDIUM: 'blue', LOW: 'orange' }[text] || 'default';
    },
    sourceColor(source) {
      const colors = {
        LDAP: 'green',
        RANGER: 'purple',
        HIVE_SERVER2: 'blue',
        STARROCKS: 'geekblue',
        DORIS: 'volcano',
        HDFS: 'cyan',
        YARN: 'gold',
        HUE: 'magenta',
        DGA: 'default'
      };
      return colors[source] || 'default';
    },
    sourceLabel(source) {
      const item = this.sourceOptions.find(option => option.value === source);
      return item ? item.label : (source || '-');
    },
    scanSourceText(source) {
      if (!source || !source.configured) return '未接入';
      const message = source.message || '';
      if (message.includes('失败')) return '采集失败';
      if (message.includes('完成')) return '采集完成';
      return '已接入';
    },
    scanSourceColor(source) {
      if (!source || !source.configured) return 'default';
      const message = source.message || '';
      if (message.includes('失败')) return 'red';
      return this.sourceColor(source.sourceSystem);
    },
    sourceTags(text) {
      if (!text) return [];
      return text.split(',').map(item => item.trim()).filter(Boolean);
    },
    ownerList(text) {
      if (!text) return [];
      return text.split(',').map(item => item.trim()).filter(Boolean);
    },
    resourceName(record) {
      if (record.resourceType === 'ACCOUNT') {
        return `${record.clusterName || record.clusterCode || '-'} · 账号粒度`;
      }
      if (record.resourceType === 'LDAP_GROUP') {
        return `${record.clusterName || record.clusterCode || '-'} · LDAP 组`;
      }
      if (!record.databaseName && !record.tableName) {
        return record.clusterName || record.clusterCode || '-';
      }
      const table = record.tableName ? `.${record.tableName}` : '';
      const scope = record.resourceType ? `${record.resourceType} ` : '';
      return `${scope}${record.databaseName || '-'}${table}`;
    },
    formatTime(value) {
      return value ? moment(value).format('YYYY-MM-DD HH:mm') : '-';
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
.governance-panel {
  min-height: 100%;
}
.module-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.module-header h1 {
  margin: 0 0 4px;
  color: #101828;
  font-size: 24px;
  font-weight: 700;
}
.module-header p {
  margin: 0;
  color: #667085;
}
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-label {
  color: #667085;
  font-size: 12px;
}
.source-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  color: #667085;
}
.capability-alert {
  margin-bottom: 14px;
}
.scan-result-panel {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #d9e7ff;
  border-radius: 8px;
}
.scan-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.scan-result-header strong {
  margin-right: 10px;
  color: #101828;
}
.scan-result-header span {
  color: #667085;
  font-size: 12px;
}
.scan-result-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(100px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.scan-result-metrics div {
  padding: 10px 12px;
  background: #f8fbff;
  border: 1px solid #edf4ff;
  border-radius: 6px;
}
.scan-result-metrics span {
  display: block;
  color: #667085;
  font-size: 12px;
}
.scan-result-metrics strong {
  display: block;
  margin-top: 4px;
  color: #101828;
  font-size: 20px;
  line-height: 26px;
}
.scan-result-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.scan-capability-note {
  margin: -2px 0 10px;
  color: #667085;
  font-size: 12px;
  line-height: 20px;
}
.summary-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-item {
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eaecf0;
  border-radius: 8px;
}
.summary-item span {
  display: block;
  color: #667085;
  font-size: 12px;
}
.summary-item strong {
  display: block;
  margin-top: 4px;
  color: #101828;
  font-size: 24px;
  line-height: 30px;
}
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.primary-text {
  color: #101828;
  font-weight: 600;
}
.muted-text {
  color: #667085;
  font-size: 12px;
}
.confidence-tag {
  margin-top: 4px;
}
.ellipsis {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.owner-create-box {
  padding: 10px 12px;
  margin: 4px 0 12px;
  background: #f8fafc;
  border: 1px solid #eaecf0;
  border-radius: 8px;
}
.owner-create-box ::v-deep .ant-form-item {
  margin-bottom: 8px;
}
.select-extra {
  float: right;
  color: #98a2b3;
  font-size: 12px;
}
.rule-modal {
  display: grid;
  gap: 12px;
}
.rule-section {
  padding: 14px 16px;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  background: #fff;
}
.rule-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #101828;
}
.rule-line {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
  margin-top: 8px;
}
.rule-line span {
  color: #667085;
  font-size: 12px;
}
.rule-line p {
  margin: 0;
  color: #344054;
  line-height: 22px;
}
@media (max-width: 960px) {
  .module-header {
    flex-direction: column;
  }
  .summary-row {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
