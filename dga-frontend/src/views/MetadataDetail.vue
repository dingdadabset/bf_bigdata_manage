<template>
  <div class="metadata-detail">
    <a-page-header
      style="border: 1px solid rgb(235, 237, 240)"
      @back="handleBack"
      :title="tableInfo.tableName"
      :sub-title="tableInfo.dbName"
      :breadcrumb="{ props: { routes: breadcrumbRoutes } }"
    >
      <template slot="extra">
        <a-button v-if="canManageOwner" key="owner" icon="user" @click="showOwnerModal">维护负责人</a-button>
        <a-button key="1" type="primary" icon="sync" :loading="syncing" @click="syncMetadata">同步元数据</a-button>
      </template>
      <template slot="tags">
        <a-tag color="blue">Hive</a-tag>
        <a-tag :color="getLifecycleColor(tableInfo.lifecycleStatus)">{{ getLifecycleText(tableInfo.lifecycleStatus) }}</a-tag>
      </template>
      <div class="content">
        <div class="main">
          <a-descriptions size="small" :column="2">
            <a-descriptions-item label="负责人">
              <a-avatar size="small" icon="user" style="margin-right: 8px" />
              {{ tableInfo.owner || '-' }}
              <a-tag v-if="tableInfo.ownerSource === 'MANUAL'" color="gold" style="margin-left: 8px">手动维护</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="Hive Owner">
              {{ tableInfo.sourceOwner || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="集群">
              {{ tableInfo.clusterCode || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="表备注">
              {{ tableInfo.tableComment || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="存储格式">
              <a-tag color="blue">{{ tableInfo.storageFormat }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="元数据大小">
              <a-tooltip :title="sizeHelpText">
                <span class="size-value">
                  {{ formatSize(tableInfo.totalSize) }}
                  <a-icon type="question-circle" class="size-help-icon" />
                </span>
              </a-tooltip>
            </a-descriptions-item>
            <a-descriptions-item label="记录数">
              {{ tableInfo.recordCount !== null ? tableInfo.recordCount : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="存储路径">
              <span style="word-break: break-all">{{ tableInfo.locationPath || '-' }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{ tableInfo.updatedAt ? new Date(tableInfo.updatedAt).toLocaleString() : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="同步时间">
              {{ tableInfo.syncTime ? new Date(tableInfo.syncTime).toLocaleString() : '-' }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </div>
    </a-page-header>

    <a-card style="margin-top: 24px" :bordered="false" :tab-list="tabList" :active-tab-key="activeTabKey" @tabChange="key => onTabChange(key, 'activeTabKey')">
      <div v-if="activeTabKey === 'basic'">
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="集群">{{ tableInfo.clusterCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数据源">{{ tableInfo.dataSourceId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数据库">{{ tableInfo.dbName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="表名">{{ tableInfo.tableName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="表备注">{{ tableInfo.tableComment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生命周期">
            <a-tag :color="getLifecycleColor(tableInfo.lifecycleStatus)">{{ getLifecycleText(tableInfo.lifecycleStatus) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="负责人">{{ tableInfo.owner || '-' }}</a-descriptions-item>
          <a-descriptions-item label="Hive Owner">{{ tableInfo.sourceOwner || '-' }}</a-descriptions-item>
          <a-descriptions-item label="存储格式">
            <a-tag color="blue">{{ tableInfo.storageFormat || '-' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="元数据大小">
            <a-tooltip :title="sizeHelpText">
              <span class="size-value">
                {{ formatSize(tableInfo.totalSize) }}
                <a-icon type="question-circle" class="size-help-icon" />
              </span>
            </a-tooltip>
            <span v-if="isZeroSize(tableInfo.totalSize)" class="size-inline-note">可能未刷新统计</span>
          </a-descriptions-item>
          <a-descriptions-item label="记录数">{{ tableInfo.recordCount !== null ? tableInfo.recordCount : '-' }}</a-descriptions-item>
          <a-descriptions-item label="分区数">{{ tableInfo.partitionCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="存储路径" :span="2">
            <span class="break-text">{{ tableInfo.locationPath || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="同步时间">{{ tableInfo.syncTime ? new Date(tableInfo.syncTime).toLocaleString() : '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ tableInfo.updatedAt ? new Date(tableInfo.updatedAt).toLocaleString() : '-' }}</a-descriptions-item>
        </a-descriptions>
      </div>
      <div v-else-if="activeTabKey === 'schema'">
        <div class="schema-toolbar">
          <a-alert
            class="schema-source-alert"
            :type="hiveCreateSqlSource === 'HIVE_SERVER2' ? 'success' : 'warning'"
            :message="schemaSourceMessage"
            show-icon
          />
          <a-space>
            <a-button icon="copy" @click="copyHiveCreateSql">复制 SQL</a-button>
            <a-button icon="reload" :loading="loadingColumns" @click="fetchColumns">刷新字段</a-button>
            <a-button type="primary" icon="search" :loading="loadingCreateSql" @click="fetchHiveCreateSql">查询 HS2 真实建表语句</a-button>
          </a-space>
        </div>
        <a-spin :spinning="loadingCreateSql || loadingColumns">
          <pre class="schema-json"><code><span
            v-for="line in highlightedHiveCreateSqlLines"
            :key="line.index"
            class="schema-code-line"
            :class="line.className"
            v-html="line.html"
          ></span></code></pre>
        </a-spin>
      </div>
      <div v-else-if="activeTabKey === 'partitions'">
        <div class="partition-summary">
          <div class="partition-summary-item">
            <span>总分区数</span>
            <strong>{{ formatNumber(partitionCount) }}</strong>
          </div>
          <div class="partition-summary-item">
            <span>平均文件大小</span>
            <strong>{{ formatSize(partitionAverageSize) }}</strong>
            <small>{{ partitionAverageBasis }}</small>
          </div>
          <div class="partition-summary-item">
            <span>当前展示</span>
            <strong>最新 {{ partitionData.length }} 个</strong>
            <small>上限 {{ partitionDisplayLimit }} 个</small>
          </div>
        </div>
        <a-table :columns="partitionColumns" :data-source="partitionData" row-key="id" :pagination="false" :loading="loadingPartitions" :scroll="{ x: 1100 }">
          <span slot="partitionSize" slot-scope="text">{{ formatSize(text) }}</span>
          <span slot="partitionTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
          <span slot="partitionPath" slot-scope="text">
            <span class="path-cell" :title="text">{{ text || '-' }}</span>
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'lineage'">
        <div class="lineage-toolbar">
          <a-space>
            <a-select
              v-model="lineageFilters.sourceEndpointId"
              allowClear
              placeholder="全部调度源"
              style="width: 260px"
              @change="onLineageEndpointChange"
            >
              <a-select-option v-for="endpoint in schedulerEndpoints" :key="endpoint.id" :value="endpoint.id">
                {{ endpoint.endpointType }} / {{ endpoint.url || endpoint.serviceName || endpoint.id }}
              </a-select-option>
            </a-select>
            <a-select
              v-model="lineageFilters.sourceType"
              allowClear
              placeholder="来源类型"
              style="width: 200px"
              @change="fetchLineage"
            >
              <a-select-option value="AZKABAN_DB">AZKABAN_DB</a-select-option>
              <a-select-option value="AZKABAN_WEB">AZKABAN_WEB</a-select-option>
              <a-select-option value="DOLPHINSCHEDULER_DB">DOLPHINSCHEDULER_DB</a-select-option>
              <a-select-option value="LEGACY">LEGACY</a-select-option>
            </a-select>
            <a-button icon="reload" :loading="lineageLoading" @click="refreshLineage">刷新血缘</a-button>
            <a-button
              type="primary"
              icon="deployment-unit"
              :disabled="!lineageFilters.sourceEndpointId || !tableInfo.dataSourceId"
              :loading="lineageCollecting"
              @click="collectLineage"
            >
              解析血缘
            </a-button>
          </a-space>
        </div>
        <a-alert
          v-if="!schedulerEndpoints.length"
          message="当前集群还没有配置 AZKABAN_DB 或 DOLPHINSCHEDULER_DB 调度源端点"
          description="请先到环境资源里新增调度源端点，再回到这里触发血缘解析。"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <div ref="lineageChart" class="lineage-chart"></div>
        <a-empty v-if="!lineageData || !lineageData.nodes || lineageData.nodes.length === 0" description="暂无血缘数据" />
        <a-divider orientation="left">调度上下文建议</a-divider>
        <a-table
          :columns="contextSuggestionColumns"
          :data-source="contextSuggestions"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 6 }"
          :loading="loadingContextSuggestions"
          :scroll="{ x: 1200 }"
        >
          <span slot="suggestionType" slot-scope="text">
            <a-tag :color="text === 'COLUMN_COMMENT' ? 'blue' : 'purple'">{{ suggestionTypeLabel(text) }}</a-tag>
          </span>
          <span slot="suggestionValue" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="lineage-message">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </span>
          <span slot="suggestionStatus" slot-scope="text">
            <a-tag :color="suggestionStatusColor(text)">{{ suggestionStatusLabel(text) }}</a-tag>
          </span>
          <span slot="suggestionTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
          <span slot="suggestionAction" slot-scope="text, record">
            <template v-if="record.status === 'PENDING' && canManageOwner">
              <a-button type="link" size="small" icon="check" @click="applyContextSuggestion(record)">应用</a-button>
              <a-button type="link" size="small" icon="close" @click="rejectContextSuggestion(record)">忽略</a-button>
            </template>
            <span v-else>-</span>
          </span>
        </a-table>
        <a-divider orientation="left">解析任务</a-divider>
        <a-table
          :columns="lineageTaskColumns"
          :data-source="lineageTasks"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 5 }"
          :loading="lineageTasksLoading"
          :scroll="{ x: 1100 }"
        >
          <span slot="lineageTaskStatus" slot-scope="text">
            <a-tag :color="getLineageTaskStatusColor(text)">{{ text || '-' }}</a-tag>
          </span>
          <span slot="lineageTaskTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
          <span slot="lineageTaskMessage" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="lineage-message">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'business'">
        <a-form-model layout="vertical" class="metadata-form">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-model-item label="数据主题">
                <a-select v-model="businessInfo.themeId" allowClear placeholder="选择数据主题" :disabled="!canManageOwner">
                  <a-select-option v-for="theme in themes" :key="theme.id" :value="theme.id">
                    {{ theme.themeName }}
                  </a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :span="8">
              <a-form-model-item label="业务负责人">
                <a-input v-model="businessInfo.businessOwner" placeholder="业务负责人" :disabled="!canManageOwner" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-form-model-item label="业务口径">
            <a-textarea v-model="businessInfo.businessDefinition" :rows="4" placeholder="指标口径、统计范围、过滤规则等" :disabled="!canManageOwner" />
          </a-form-model-item>
          <a-form-model-item label="业务说明">
            <a-textarea v-model="businessInfo.businessDescription" :rows="3" placeholder="说明该表服务的业务场景" :disabled="!canManageOwner" />
          </a-form-model-item>
          <a-button v-if="canManageOwner" type="primary" icon="save" :loading="businessSaving" @click="saveBusiness">保存业务元数据</a-button>
        </a-form-model>
        <a-divider orientation="left">指标定义</a-divider>
        <div class="table-toolbar">
          <a-button v-if="canManageOwner" type="primary" icon="plus" @click="showMetricModal()">新增指标</a-button>
        </div>
        <a-table :columns="metricColumns" :data-source="metricsData" row-key="id" :pagination="false" :loading="loadingMetrics">
          <span slot="metricStatus" slot-scope="text">
            <a-tag :color="text === 'ACTIVE' ? 'green' : 'default'">{{ text || 'ACTIVE' }}</a-tag>
          </span>
          <span slot="metricAction" slot-scope="text, record">
            <a-button v-if="canManageOwner" type="link" size="small" icon="edit" @click="showMetricModal(record)">编辑</a-button>
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'management'">
        <a-form-model layout="vertical" class="metadata-form">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-model-item label="生命周期">
                <a-select v-model="managementForm.lifecycleStatus" :disabled="!canManageOwner">
                  <a-select-option value="ONLINE">在线</a-select-option>
                  <a-select-option value="DEPRECATED">已废弃</a-select-option>
                  <a-select-option value="OFFLINE">已下线</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :span="8">
              <a-form-model-item label="资产负责人">
                <a-input v-model="managementForm.owner" placeholder="资产负责人" :disabled="!canManageOwner" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-form-model-item label="标签">
            <a-select v-model="managementForm.tagNames" mode="tags" placeholder="输入或选择标签" :disabled="!canManageOwner">
              <a-select-option v-for="tag in tags" :key="tag.tagName" :value="tag.tagName">
                {{ tag.tagName }}
              </a-select-option>
            </a-select>
          </a-form-model-item>
          <a-button v-if="canManageOwner" type="primary" icon="save" :loading="managementSaving" @click="saveManagement">保存管理元数据</a-button>
        </a-form-model>
      </div>
      <div v-else-if="activeTabKey === 'quality'">
        <div class="table-toolbar">
          <a-button icon="dashboard" @click="$router.push({ path: '/quality', query: { tableId } })">进入质量中心</a-button>
          <a-button v-if="canManageOwner" type="primary" icon="thunderbolt" :loading="qualityExecuting" @click="executeTableQuality">执行本表规则</a-button>
        </div>
        <a-table
          :columns="qualityRuleColumns"
          :data-source="qualityRules"
          row-key="id"
          :pagination="{ pageSize: 5 }"
          :loading="loadingQuality"
          :scroll="{ x: 1000 }"
        >
          <span slot="qualityRuleType" slot-scope="text">
            <a-tag color="blue">{{ qualityRuleTypeLabel(text) }}</a-tag>
          </span>
          <span slot="qualityStatus" slot-scope="text">
            <a-tag :color="text === 'SUCCESS' ? 'green' : text === 'FAILED' ? 'red' : 'default'">
              {{ text === 'SUCCESS' ? '成功' : text === 'FAILED' ? '失败' : '未执行' }}
            </a-tag>
          </span>
          <span slot="qualityTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
        </a-table>
        <a-divider orientation="left">质量问题</a-divider>
        <a-table
          :columns="qualityIssueColumns"
          :data-source="qualityIssues"
          row-key="id"
          :pagination="{ pageSize: 5 }"
          :loading="loadingQualityIssues"
          :scroll="{ x: 1000 }"
        >
          <span slot="qualityIssueStatus" slot-scope="text">
            <a-tag :color="text === 'OPEN' ? 'red' : 'green'">{{ text === 'OPEN' ? '待处理' : '已解决' }}</a-tag>
          </span>
          <span slot="qualityIssueMessage" slot-scope="text">
            <a-tooltip v-if="text" :title="text">
              <span class="lineage-message">{{ text }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </span>
          <span slot="qualityTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'permissions'">
        <a-table :columns="permissionColumns" :data-source="permissionsData" row-key="id" :pagination="{ pageSize: 10 }" :loading="loadingPermissions" :scroll="{ x: 1000 }">
          <span slot="permissionTime" slot-scope="text">{{ text ? new Date(text).toLocaleString() : '-' }}</span>
          <span slot="permissionStatus" slot-scope="text">
            <a-tag :color="text === 'ACTIVE' ? 'green' : 'default'">{{ text || '-' }}</a-tag>
          </span>
        </a-table>
      </div>
    </a-card>

    <a-modal
      title="维护资产负责人"
      :visible="ownerModalVisible"
      :confirm-loading="ownerSaving"
      @ok="saveOwner"
      @cancel="ownerModalVisible = false"
    >
      <a-form-model layout="vertical">
        <a-form-model-item label="资产">
          <a-input :value="`${tableInfo.dbName || ''}.${tableInfo.tableName || ''}`" disabled />
        </a-form-model-item>
        <a-form-model-item label="负责人">
          <a-input v-model="ownerForm.owner" placeholder="请输入负责人账号或姓名" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal
      :title="metricEditing ? '编辑指标' : '新增指标'"
      :visible="metricModalVisible"
      :confirm-loading="metricSaving"
      @ok="saveMetric"
      @cancel="metricModalVisible = false"
    >
      <a-form-model layout="vertical">
        <a-form-model-item label="指标名称">
          <a-input v-model="metricForm.metricName" placeholder="例如：支付成功金额" />
        </a-form-model-item>
        <a-form-model-item label="指标编码">
          <a-input v-model="metricForm.metricCode" placeholder="例如：pay_success_amount" />
        </a-form-model-item>
        <a-form-model-item label="负责人">
          <a-input v-model="metricForm.owner" placeholder="指标负责人" />
        </a-form-model-item>
        <a-form-model-item label="口径说明">
          <a-textarea v-model="metricForm.businessDefinition" :rows="3" />
        </a-form-model-item>
        <a-form-model-item label="计算逻辑">
          <a-textarea v-model="metricForm.calculationLogic" :rows="4" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import * as echarts from 'echarts';
import { canDelete } from '../utils/currentUser';

export default {
  data() {
    return {
      tableId: this.$route.params.id,
      tableInfo: {},
      columnData: [],
      hiveCreateSqlFromServer: '',
      hiveCreateSqlSource: 'LOCAL_FALLBACK',
      hiveCreateSqlError: '',
      partitionData: [],
      partitionCount: 0,
      partitionDisplayLimit: 10,
      taskData: [],
      permissionsData: [],
      metricsData: [],
      qualityRules: [],
      qualityIssues: [],
      themes: [],
      tags: [],
      businessInfo: {
        themeId: undefined,
        businessOwner: '',
        businessDefinition: '',
        businessDescription: ''
      },
      managementForm: {
        lifecycleStatus: 'ONLINE',
        owner: '',
        tagNames: []
      },
      schedulerEndpoints: [],
      lineageFilters: {
        sourceEndpointId: undefined,
        sourceType: undefined
      },
      lineageData: null,
      lineageChart: null,
      lineageTasks: [],
      contextSuggestions: [],
      loadingColumns: false,
      loadingCreateSql: false,
      loadingPartitions: false,
      loadingTasks: false,
      lineageLoading: false,
      lineageCollecting: false,
      lineageTasksLoading: false,
      loadingContextSuggestions: false,
      loadingPermissions: false,
      loadingMetrics: false,
      loadingQuality: false,
      loadingQualityIssues: false,
      qualityExecuting: false,
      syncing: false,
      businessSaving: false,
      managementSaving: false,
      metricModalVisible: false,
      metricSaving: false,
      metricEditing: null,
      metricForm: {
        metricName: '',
        metricCode: '',
        businessDefinition: '',
        calculationLogic: '',
        owner: '',
        status: 'ACTIVE'
      },
      ownerModalVisible: false,
      ownerSaving: false,
      ownerForm: {
        owner: ''
      },
      activeTabKey: 'basic',
      tabList: [
        {
          key: 'basic',
          tab: '基础信息',
        },
        {
          key: 'schema',
          tab: '字段',
        },
        {
          key: 'partitions',
          tab: '分区',
        },
        {
          key: 'lineage',
          tab: '血缘',
        },
        {
          key: 'business',
          tab: '业务元数据',
        },
        {
          key: 'management',
          tab: '管理元数据',
        },
        {
          key: 'quality',
          tab: '质量',
        },
        {
          key: 'permissions',
          tab: '权限',
        },
      ],
      columns: [
        { title: '字段名', dataIndex: 'columnName', key: 'columnName' },
        { title: '类型', dataIndex: 'columnType', key: 'columnType', scopedSlots: { customRender: 'columnType' } },
        { title: '描述', dataIndex: 'comment', key: 'comment' },
        { title: '主键', dataIndex: 'isPrimaryKey', key: 'isPrimaryKey', scopedSlots: { customRender: 'isPrimaryKey' } },
        { title: '安全等级', dataIndex: 'securityLevel', key: 'securityLevel' }
      ],
      contextSuggestionColumns: [
        { title: '类型', dataIndex: 'contextType', key: 'contextType', scopedSlots: { customRender: 'suggestionType' }, width: 150 },
        { title: '建议值', dataIndex: 'suggestedValue', key: 'suggestedValue', scopedSlots: { customRender: 'suggestionValue' }, width: 260 },
        { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 180 },
        { title: 'Flow', dataIndex: 'flowName', key: 'flowName', width: 220 },
        { title: 'Job', dataIndex: 'jobName', key: 'jobName', width: 240 },
        { title: '置信度', dataIndex: 'confidence', key: 'confidence', width: 100 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'suggestionStatus' }, width: 110 },
        { title: '解析时间', dataIndex: 'parsedAt', key: 'parsedAt', scopedSlots: { customRender: 'suggestionTime' }, width: 180 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'suggestionAction' }, width: 150 }
      ],
      taskColumns: [
        { title: '问题类型', dataIndex: 'issueType', key: 'issueType' },
        { title: '问题描述', dataIndex: 'issueDescription', key: 'issueDescription' },
        { title: '状态', dataIndex: 'taskStatus', key: 'taskStatus', scopedSlots: { customRender: 'taskStatus' } },
        { title: '处理人', dataIndex: 'handler', key: 'handler' },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', scopedSlots: { customRender: 'createTime' } }
      ],
      partitionColumns: [
        { title: '分区', dataIndex: 'partitionName', key: 'partitionName', width: 240 },
        { title: '格式', dataIndex: 'storageFormat', key: 'storageFormat', width: 110 },
        { title: '大小', dataIndex: 'totalSize', key: 'totalSize', scopedSlots: { customRender: 'partitionSize' }, width: 120 },
        { title: '记录数', dataIndex: 'recordCount', key: 'recordCount', width: 120 },
        { title: '更新时间', dataIndex: 'lastModifyTime', key: 'lastModifyTime', scopedSlots: { customRender: 'partitionTime' }, width: 180 },
        { title: '路径', dataIndex: 'locationPath', key: 'locationPath', scopedSlots: { customRender: 'partitionPath' }, width: 360 }
      ],
      lineageTaskColumns: [
        { title: '调度源', dataIndex: 'sourceType', key: 'sourceType', width: 160 },
        { title: '端点', dataIndex: 'sourceEndpointName', key: 'sourceEndpointName', width: 220 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'lineageTaskStatus' }, width: 130 },
        { title: '成功边数', dataIndex: 'successEdgeCount', key: 'successEdgeCount', width: 110 },
        { title: '失败数', dataIndex: 'failedEdgeCount', key: 'failedEdgeCount', width: 100 },
        { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', scopedSlots: { customRender: 'lineageTaskTime' }, width: 180 },
        { title: '结束时间', dataIndex: 'finishedAt', key: 'finishedAt', scopedSlots: { customRender: 'lineageTaskTime' }, width: 180 },
        { title: '信息', dataIndex: 'message', key: 'message', scopedSlots: { customRender: 'lineageTaskMessage' }, width: 260 }
      ],
      permissionColumns: [
        { title: '用户', dataIndex: 'username', key: 'username', width: 140 },
        { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 120 },
        { title: '权限', dataIndex: 'permission', key: 'permission', width: 120 },
        { title: '授权后端', dataIndex: 'authBackend', key: 'authBackend', width: 130 },
        { title: '来源', dataIndex: 'source', key: 'source', width: 120 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'permissionStatus' }, width: 100 },
        { title: '授权人', dataIndex: 'grantedBy', key: 'grantedBy', width: 130 },
        { title: '授权时间', dataIndex: 'grantTime', key: 'grantTime', scopedSlots: { customRender: 'permissionTime' }, width: 180 }
      ],
      metricColumns: [
        { title: '指标名称', dataIndex: 'metricName', key: 'metricName' },
        { title: '指标编码', dataIndex: 'metricCode', key: 'metricCode' },
        { title: '负责人', dataIndex: 'owner', key: 'owner' },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'metricStatus' }, width: 100 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'metricAction' }, width: 120 }
      ],
      qualityRuleColumns: [
        { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 220 },
        { title: '类型', dataIndex: 'ruleType', key: 'ruleType', scopedSlots: { customRender: 'qualityRuleType' }, width: 120 },
        { title: '字段', dataIndex: 'columnName', key: 'columnName', width: 140 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', width: 120 },
        { title: '最近状态', dataIndex: 'lastExecutionStatus', key: 'lastExecutionStatus', scopedSlots: { customRender: 'qualityStatus' }, width: 120 },
        { title: '结果值', dataIndex: 'lastResultValue', key: 'lastResultValue', width: 110 },
        { title: '最近执行', dataIndex: 'lastExecutedAt', key: 'lastExecutedAt', scopedSlots: { customRender: 'qualityTime' }, width: 180 }
      ],
      qualityIssueColumns: [
        { title: '问题', dataIndex: 'issueTitle', key: 'issueTitle', width: 220 },
        { title: '描述', dataIndex: 'issueDescription', key: 'issueDescription', scopedSlots: { customRender: 'qualityIssueMessage' }, width: 320 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'qualityIssueStatus' }, width: 110 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', width: 120 },
        { title: '最近发现', dataIndex: 'lastSeenAt', key: 'lastSeenAt', scopedSlots: { customRender: 'qualityTime' }, width: 180 },
        { title: '解决时间', dataIndex: 'resolvedAt', key: 'resolvedAt', scopedSlots: { customRender: 'qualityTime' }, width: 180 }
      ]
    };
  },
  computed: {
    canManageOwner() {
      return canDelete();
    },
    hiveCreateSql() {
      if (this.hiveCreateSqlFromServer) {
        return this.hiveCreateSqlFromServer;
      }
      const payload = this.buildHiveCreatePayload();
      return payload.createTableSql;
    },
    schemaSourceMessage() {
      if (this.hiveCreateSqlSource === 'HIVE_SERVER2') {
        return '建表语句来自 HiveServer2 SHOW CREATE TABLE';
      }
      return this.hiveCreateSqlError
        ? `HiveServer2 获取失败，当前展示本地元数据推断 SQL：${this.hiveCreateSqlError}`
        : '当前展示本地元数据推断 SQL，可点击“查询 HS2 真实建表语句”获取 Hive 原始 DDL';
    },
    highlightedHiveCreateSqlLines() {
      return this.hiveCreateSql.split('\n').map((line, index, lines) => ({
        index,
        html: this.highlightSqlLine(line),
        className: this.getSchemaSqlLineClass(line, lines[index - 1] || '')
      }));
    },
    breadcrumbRoutes() {
      return [
        { path: '/metadata', breadcrumbName: '元数据列表' },
        { path: `db-${this.tableInfo.dbName || 'unknown'}`, breadcrumbName: this.tableInfo.dbName || '...' },
        { path: `table-${this.tableInfo.id || this.tableId}`, breadcrumbName: this.tableInfo.tableName || '...' },
      ];
    },
    sizeHelpText() {
      return '当前大小来自 Hive Metastore TABLE_PARAMS.totalSize / PARTITION_PARAMS.totalSize。若未执行统计刷新或 HDFS 文件刚变化，可能显示 0 B 或与实际文件大小不一致。';
    },
    partitionAverageSize() {
      const sizes = (this.partitionData || [])
        .map(item => Number(item.totalSize))
        .filter(size => Number.isFinite(size) && size >= 0);
      if (!sizes.length) {
        return null;
      }
      return sizes.reduce((sum, size) => sum + size, 0) / sizes.length;
    },
    partitionAverageBasis() {
      if (!this.partitionData.length) {
        return '暂无展示分区';
      }
      return `基于当前展示 ${this.formatNumber(this.partitionData.length)} 个`;
    }
  },
  mounted() {
    this.fetchTableInfo();
    this.fetchColumns();
    this.fetchThemes();
    this.fetchTags();
    this.fetchBusiness();
    this.fetchManagement();
    this.fetchMetrics();
  },
  methods: {
    async fetchTableInfo() {
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}`);
        this.tableInfo = res.data;
        if (this.activeTabKey === 'schema' && Number(this.tableInfo.partitionCount || 0) > 0 && this.partitionData.length === 0) {
          this.fetchPartitions();
        }
        this.fetchSchedulerEndpoints();
      } catch (e) {
        this.$message.error('获取表详情失败');
      }
    },
    async fetchColumns() {
      this.loadingColumns = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/columns`);
        this.columnData = res.data || [];
      } catch (e) {
        this.$message.error('获取字段信息失败');
      } finally {
        this.loadingColumns = false;
      }
    },
    async fetchHiveCreateSql() {
      this.loadingCreateSql = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/create-ddl`);
        this.hiveCreateSqlFromServer = res.data?.sql || '';
        this.hiveCreateSqlSource = 'HIVE_SERVER2';
        this.hiveCreateSqlError = '';
      } catch (e) {
        this.hiveCreateSqlFromServer = '';
        this.hiveCreateSqlSource = 'LOCAL_FALLBACK';
        this.hiveCreateSqlError = this.resolveErrorMessage(e);
      } finally {
        this.loadingCreateSql = false;
      }
    },
    async fetchPartitions() {
      this.loadingPartitions = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/partitions`, {
          params: { limit: this.partitionDisplayLimit }
        });
        this.partitionCount = res.data?.partitionCount || 0;
        this.partitionDisplayLimit = res.data?.displayLimit || this.partitionDisplayLimit;
        this.partitionData = (res.data?.items || []).slice(0, this.partitionDisplayLimit);
      } catch (e) {
        this.$message.error('获取分区信息失败');
      } finally {
        this.loadingPartitions = false;
      }
    },
    async fetchThemes() {
      try {
        const res = await axios.get('/api/metadata/themes');
        this.themes = res.data || [];
      } catch (e) {
        console.error('Fetch themes failed', e);
      }
    },
    async fetchTags() {
      try {
        const res = await axios.get('/api/metadata/tags');
        this.tags = res.data || [];
      } catch (e) {
        console.error('Fetch tags failed', e);
      }
    },
    async fetchBusiness() {
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/business`);
        this.businessInfo = {
          themeId: res.data.themeId || undefined,
          businessOwner: res.data.businessOwner || '',
          businessDefinition: res.data.businessDefinition || '',
          businessDescription: res.data.businessDescription || ''
        };
      } catch (e) {
        this.$message.error('获取业务元数据失败');
      }
    },
    async fetchManagement() {
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/management`);
        const table = res.data.table || {};
        this.managementForm = {
          lifecycleStatus: table.lifecycleStatus || 'ONLINE',
          owner: table.owner || '',
          tagNames: (res.data.tags || []).map(tag => tag.tagName)
        };
      } catch (e) {
        this.$message.error('获取管理元数据失败');
      }
    },
    async fetchPermissions() {
      this.loadingPermissions = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/permissions`);
        this.permissionsData = res.data || [];
      } catch (e) {
        this.$message.error('获取权限信息失败');
      } finally {
        this.loadingPermissions = false;
      }
    },
    async fetchMetrics() {
      this.loadingMetrics = true;
      try {
        const res = await axios.get('/api/metadata/metrics', { params: { tableId: this.tableId } });
        this.metricsData = res.data || [];
      } catch (e) {
        this.$message.error('获取指标定义失败');
      } finally {
        this.loadingMetrics = false;
      }
    },
    async fetchQualityRules() {
      this.loadingQuality = true;
      try {
        const res = await axios.get('/api/quality/rules', { params: { tableId: this.tableId } });
        this.qualityRules = res.data || [];
      } catch (e) {
        this.$message.error('获取质量规则失败');
      } finally {
        this.loadingQuality = false;
      }
    },
    async fetchQualityIssues() {
      this.loadingQualityIssues = true;
      try {
        const res = await axios.get('/api/quality/issues', {
          params: {
            tableId: this.tableId,
            size: 20
          }
        });
        this.qualityIssues = res.data?.content || [];
      } catch (e) {
        this.$message.error('获取质量问题失败');
      } finally {
        this.loadingQualityIssues = false;
      }
    },
    async fetchQuality() {
      await Promise.all([
        this.fetchQualityRules(),
        this.fetchQualityIssues()
      ]);
    },
    async fetchTasks() {
      this.loadingTasks = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/governance-tasks`);
        this.taskData = res.data;
      } catch (e) {
        console.error('Fetch tasks failed', e);
        this.$message.error('获取治理评价失败');
      } finally {
        this.loadingTasks = false;
      }
    },
    async fetchSchedulerEndpoints() {
      try {
        const res = await axios.get('/api/clusters');
        const clusters = res.data || [];
        const currentClusterCode = this.tableInfo.clusterCode;
        const endpointTypes = ['AZKABAN_DB', 'AZKABAN_WEB', 'DOLPHINSCHEDULER_DB'];
        this.schedulerEndpoints = clusters
          .filter(cluster => !currentClusterCode || cluster.clusterCode === currentClusterCode || cluster.clusterName === currentClusterCode)
          .flatMap(cluster => (cluster.endpoints || []).map(endpoint => ({
            ...endpoint,
            clusterCode: endpoint.clusterCode || cluster.clusterCode,
            clusterName: cluster.clusterName
          })))
          .filter(endpoint => endpointTypes.includes(endpoint.endpointType) && endpoint.status !== 'INACTIVE');
        if (this.activeTabKey === 'lineage') {
          this.fetchLineageTasks();
        }
      } catch (e) {
        this.schedulerEndpoints = [];
        console.error('Fetch scheduler endpoints failed', e);
      }
    },
    async fetchLineageTasks() {
      this.lineageTasksLoading = true;
      try {
        const params = {
          dataSourceId: this.tableInfo.dataSourceId
        };
        if (this.lineageFilters.sourceEndpointId) {
          params.sourceEndpointId = this.lineageFilters.sourceEndpointId;
        }
        const res = await axios.get('/api/lineage/tasks', { params });
        this.lineageTasks = res.data || [];
      } catch (e) {
        this.lineageTasks = [];
        console.error('Fetch lineage tasks failed', e);
      } finally {
        this.lineageTasksLoading = false;
      }
    },
    async fetchContextSuggestions() {
      this.loadingContextSuggestions = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/context-suggestions`);
        this.contextSuggestions = res.data || [];
      } catch (e) {
        this.contextSuggestions = [];
        console.error('Fetch context suggestions failed', e);
      } finally {
        this.loadingContextSuggestions = false;
      }
    },
    handleBack() {
      this.$router.push('/metadata');
    },
    onTabChange(key, type) {
      this[type] = key;
      if (key === 'lineage') {
        this.fetchSchedulerEndpoints();
        this.fetchLineageTasks();
        this.fetchContextSuggestions();
        if (!this.lineageData) {
            this.fetchLineage();
        } else {
             this.$nextTick(() => {
                if (this.lineageChart) {
                    this.lineageChart.resize();
                } else {
                    this.initChart();
                }
            });
        }
      } else if (key === 'partitions' && this.partitionData.length === 0) {
        this.fetchPartitions();
      } else if (key === 'schema' && Number(this.tableInfo.partitionCount || 0) > 0 && this.partitionData.length === 0) {
        this.fetchPartitions();
      } else if (key === 'permissions' && this.permissionsData.length === 0) {
        this.fetchPermissions();
      } else if (key === 'quality') {
        this.fetchQuality();
      }
    },
    async fetchLineage() {
        this.lineageLoading = true;
        try {
            const params = {};
            if (this.lineageFilters.sourceType) {
                params.sourceType = this.lineageFilters.sourceType;
            }
            if (this.lineageFilters.sourceEndpointId) {
                params.sourceEndpointId = this.lineageFilters.sourceEndpointId;
            }
            const res = await axios.get(`/api/metadata/table/${this.tableId}/lineage`, { params });
            this.lineageData = res.data;
            if (this.lineageData && this.lineageData.nodes && this.lineageData.nodes.length > 0) {
                this.$nextTick(() => {
                    this.initChart();
                });
            } else if (this.lineageChart) {
                this.lineageChart.dispose();
                this.lineageChart = null;
            }
        } catch (e) {
            this.$message.error('获取血缘信息失败');
        } finally {
            this.lineageLoading = false;
        }
    },
    async refreshLineage() {
        await Promise.all([
          this.fetchLineage(),
          this.fetchLineageTasks(),
          this.fetchContextSuggestions()
        ]);
    },
    onLineageEndpointChange(endpointId) {
        const endpoint = this.schedulerEndpoints.find(item => item.id === endpointId);
        if (endpoint) {
          this.lineageFilters.sourceType = endpoint.endpointType;
        } else {
          this.lineageFilters.sourceType = undefined;
        }
        this.fetchLineage();
        this.fetchLineageTasks();
    },
    async collectLineage() {
        if (!this.lineageFilters.sourceEndpointId) {
          this.$message.warning('请选择调度源端点');
          return;
        }
        if (!this.tableInfo.dataSourceId) {
          this.$message.warning('当前表缺少 Hive 数据源 ID，无法隔离匹配血缘');
          return;
        }
        this.lineageCollecting = true;
        try {
          const res = await axios.post(`/api/lineage/collect/${this.lineageFilters.sourceEndpointId}`, null, {
            params: { dataSourceId: this.tableInfo.dataSourceId }
          });
          const task = res.data || {};
          if (task.status === 'SUCCESS') {
            this.$message.success(`血缘解析完成，写入 ${task.successEdgeCount || 0} 条边`);
          } else {
            this.$message.warning(task.message || '血缘解析结束，请查看任务状态');
          }
          await this.refreshLineage();
        } catch (e) {
          this.$message.error(e.response?.data?.message || '血缘解析失败');
          this.fetchLineageTasks();
        } finally {
          this.lineageCollecting = false;
        }
    },
    async applyContextSuggestion(record) {
      try {
        await axios.put(`/api/metadata/context-suggestions/${record.id}/apply`);
        this.$message.success('已应用上下文建议');
        await Promise.all([this.fetchContextSuggestions(), this.fetchColumns(), this.fetchBusiness()]);
      } catch (e) {
        this.$message.error(e.response?.data?.message || '应用建议失败');
      }
    },
    async rejectContextSuggestion(record) {
      try {
        await axios.put(`/api/metadata/context-suggestions/${record.id}/reject`);
        this.$message.success('已忽略上下文建议');
        this.fetchContextSuggestions();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '忽略建议失败');
      }
    },
    suggestionTypeLabel(type) {
      return {
        COLUMN_COMMENT: '字段备注',
        SCHEDULER_CONTEXT: '调度上下文'
      }[type] || type || '-';
    },
    suggestionStatusLabel(status) {
      return {
        PENDING: '待确认',
        APPLIED: '已应用',
        REJECTED: '已忽略'
      }[status] || status || '-';
    },
    suggestionStatusColor(status) {
      return {
        PENDING: 'orange',
        APPLIED: 'green',
        REJECTED: 'default'
      }[status] || 'default';
    },
    initChart() {
        if (!this.$refs.lineageChart) return;
        
        // Dispose existing instance if any
        if (this.lineageChart) {
             this.lineageChart.dispose();
        }

        this.lineageChart = echarts.init(this.$refs.lineageChart);
        const option = {
            title: { text: '' },
            tooltip: {
                formatter: params => {
                    if (params.dataType === 'edge') {
                        const sources = params.data.sources || [];
                        if (!sources.length) {
                            return `${params.data.source} -> ${params.data.target}`;
                        }
                        const sourceLines = sources.map(source => {
                            const task = source.sourceTask || source.sourceWorkflow || source.sourceProject || '-';
                            return `${source.sourceType || 'UNKNOWN'}：${task}`;
                        });
                        return [
                            `${params.data.source} -> ${params.data.target}`,
                            `来源数：${sources.length}`,
                            ...sourceLines
                        ].join('<br/>');
                    }
                    return params.name;
                }
            },
            legend: [{
                data: this.lineageData.categories.map(function (a) {
                    return a.name;
                })
            }],
            series: [{
                type: 'graph',
                layout: 'force',
                symbolSize: 50,
                roam: true,
                label: { show: true, position: 'right' },
                edgeSymbol: ['circle', 'arrow'],
                edgeSymbolSize: [4, 10],
                data: this.lineageData.nodes.map(node => ({
                    name: node.name,
                    category: node.category,
                    symbolSize: node.symbolSize || 50,
                    itemStyle: node.itemStyle
                })),
                links: (this.lineageData.links || []).map(link => ({
                    ...link,
                    lineStyle: {
                        width: Math.min(4, Math.max(1, link.sourceCount || 1))
                    }
                })),
                categories: this.lineageData.categories,
                force: {
                    repulsion: 2000,
                    edgeLength: [100, 200]
                },
                lineStyle: {
                    color: 'source',
                    curveness: 0.3
                }
            }]
        };
        this.lineageChart.setOption(option);
        
        // Resize observer
        window.addEventListener('resize', () => {
             this.lineageChart && this.lineageChart.resize();
        });
    },
    formatSize(value) {
      const bytes = Number(value);
      if (!Number.isFinite(bytes)) return '-';
      if (bytes === 0) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
      let unitIndex = 0;
      let displayValue = Math.abs(bytes);
      while (displayValue >= 1024 && unitIndex < units.length - 1) {
        displayValue /= 1024;
        unitIndex += 1;
      }
      const signedValue = bytes < 0 ? -displayValue : displayValue;
      return `${parseFloat(signedValue.toFixed(2))} ${units[unitIndex]}`;
    },
    formatNumber(value) {
      const number = Number(value || 0);
      return number.toLocaleString();
    },
    isZeroSize(bytes) {
      return Number(bytes) === 0;
    },
    getLifecycleColor(status) {
      if (status === 'DEPRECATED') return 'orange';
      if (status === 'OFFLINE') return 'red';
      return 'green';
    },
    getLifecycleText(status) {
      if (status === 'DEPRECATED') return '已废弃';
      if (status === 'OFFLINE') return '已下线';
      return '在线';
    },
    getLineageTaskStatusColor(status) {
      if (status === 'SUCCESS') return 'green';
      if (status === 'PARTIAL_SUCCESS') return 'orange';
      if (status === 'RUNNING') return 'blue';
      if (status === 'FAILED') return 'red';
      return 'default';
    },
    async syncMetadata() {
      this.syncing = true;
      try {
        await axios.post(`/api/metadata/table/${this.tableId}/sync`);
        this.$message.success('同步成功');
        this.fetchTableInfo();
        this.fetchColumns();
        this.fetchPartitions();
      } catch (e) {
        this.$message.error('同步失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.syncing = false;
      }
    },
    async saveBusiness() {
      this.businessSaving = true;
      try {
        const res = await axios.put(`/api/metadata/table/${this.tableId}/business`, this.businessInfo);
        this.businessInfo = {
          themeId: res.data.themeId || undefined,
          businessOwner: res.data.businessOwner || '',
          businessDefinition: res.data.businessDefinition || '',
          businessDescription: res.data.businessDescription || ''
        };
        this.$message.success('业务元数据已保存');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '业务元数据保存失败');
      } finally {
        this.businessSaving = false;
      }
    },
    async saveManagement() {
      this.managementSaving = true;
      try {
        const res = await axios.put(`/api/metadata/table/${this.tableId}/management`, this.managementForm);
        this.tableInfo = res.data.table || this.tableInfo;
        this.managementForm.tagNames = (res.data.tags || []).map(tag => tag.tagName);
        this.$message.success('管理元数据已保存');
        this.fetchTags();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '管理元数据保存失败');
      } finally {
        this.managementSaving = false;
      }
    },
    showMetricModal(record) {
      this.metricEditing = record || null;
      this.metricForm = record ? {
        metricName: record.metricName || '',
        metricCode: record.metricCode || '',
        businessDefinition: record.businessDefinition || '',
        calculationLogic: record.calculationLogic || '',
        owner: record.owner || '',
        status: record.status || 'ACTIVE'
      } : {
        metricName: '',
        metricCode: '',
        businessDefinition: '',
        calculationLogic: '',
        owner: '',
        status: 'ACTIVE'
      };
      this.metricModalVisible = true;
    },
    async saveMetric() {
      if (!this.metricForm.metricName || !this.metricForm.metricCode) {
        this.$message.warning('请填写指标名称和指标编码');
        return;
      }
      this.metricSaving = true;
      try {
        const payload = {
          ...this.metricForm,
          tableId: Number(this.tableId)
        };
        if (this.metricEditing) {
          await axios.put(`/api/metadata/metrics/${this.metricEditing.id}`, payload);
        } else {
          await axios.post('/api/metadata/metrics', payload);
        }
        this.$message.success('指标定义已保存');
        this.metricModalVisible = false;
        this.fetchMetrics();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '指标定义保存失败');
      } finally {
        this.metricSaving = false;
      }
    },
    async executeTableQuality() {
      this.qualityExecuting = true;
      try {
        const res = await axios.post(`/api/quality/execute/table/${this.tableId}`);
        this.$message.success(`已执行 ${res.data?.length || 0} 条质量规则`);
        this.fetchQuality();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '执行质量规则失败');
      } finally {
        this.qualityExecuting = false;
      }
    },
    qualityRuleTypeLabel(type) {
      const labels = {
        NULL_RATE: '空值率',
        UNIQUE_RATE: '重复率',
        VALUE_RANGE: '数值范围',
        ROW_COUNT: '行数检查',
        FRESHNESS: '新鲜度',
        REGEX_MATCH: '正则匹配'
      };
      return labels[type] || type || '-';
    },
    buildHiveCreatePayload() {
      const databaseName = this.tableInfo.dbName || '';
      const tableName = this.tableInfo.tableName || '';
      const columns = (this.columnData || []).map(column => ({
        name: column.columnName || '',
        type: column.columnType || column.column_type || column.dataType || 'string',
        comment: column.comment || '',
        primaryKey: Boolean(column.isPrimaryKey),
        securityLevel: column.securityLevel || ''
      }));
      return {
        database: databaseName,
        table: tableName,
        tableComment: this.tableInfo.tableComment || '',
        storageFormat: this.tableInfo.storageFormat || '',
        location: this.tableInfo.locationPath || '',
        columns,
        partitionColumns: this.inferPartitionColumns(),
        createTableSql: this.buildHiveCreateSql(databaseName, tableName, columns)
      };
    },
    buildHiveCreateSql(databaseName, tableName, columns) {
      const qualifiedName = databaseName
        ? `${this.quoteHiveIdentifier(databaseName)}.${this.quoteHiveIdentifier(tableName)}`
        : this.quoteHiveIdentifier(tableName || 'table_name');
      const partitionColumns = this.inferPartitionColumns();
      const columnLines = columns.length
        ? columns.map(column => {
          const comment = column.comment ? ` COMMENT '${this.escapeHiveString(column.comment)}'` : '';
          return `  ${this.quoteHiveIdentifier(column.name)} ${column.type || 'string'}${comment}`;
        }).join(',\n')
        : '  `column_name` string';
      const createKeyword = this.shouldUseExternalTable() ? 'CREATE EXTERNAL TABLE' : 'CREATE TABLE';
      const sql = [
        '-- Generated from collected Hive Metastore metadata.',
        '-- For an exact statement, prefer SHOW CREATE TABLE in Hive when available.',
        `${createKeyword} IF NOT EXISTS ${qualifiedName} (`,
        columnLines,
        ')'
      ];
      if (this.tableInfo.tableComment) {
        sql.push(`COMMENT '${this.escapeHiveString(this.tableInfo.tableComment)}'`);
      }
      if (partitionColumns.length) {
        sql.push('PARTITIONED BY (');
        sql.push(partitionColumns.map(column => `  ${this.quoteHiveIdentifier(column.name)} ${column.type}`).join(',\n'));
        sql.push(')');
      } else if (Number(this.tableInfo.partitionCount || 0) > 0) {
        sql.push('-- PARTITIONED BY (...) -- 分区存在，但当前采集结果无法推断分区字段名');
      }
      if (this.tableInfo.storageFormat) {
        sql.push(`STORED AS ${String(this.tableInfo.storageFormat).toUpperCase()}`);
      }
      if (this.tableInfo.locationPath) {
        sql.push(`LOCATION '${this.escapeHiveString(this.tableInfo.locationPath)}'`);
      }
      return `${sql.join('\n')};`;
    },
    inferPartitionColumns() {
      const specs = (this.partitionData || [])
        .map(item => item.partitionSpec || item.partitionName || '')
        .filter(Boolean);
      if (!specs.length) {
        return [];
      }
      const names = [];
      specs[0].split('/').forEach(part => {
        const eqIndex = part.indexOf('=');
        const name = eqIndex > 0 ? part.slice(0, eqIndex) : '';
        if (name && !names.includes(name)) {
          names.push(name);
        }
      });
      return names.map(name => ({
        name,
        type: this.inferPartitionType(name, specs)
      }));
    },
    inferPartitionType(name, specs) {
      const values = specs
        .map(spec => (spec.split('/').find(part => part.startsWith(`${name}=`)) || '').slice(name.length + 1))
        .filter(Boolean);
      if (!values.length) {
        return 'string';
      }
      if (values.every(value => /^\d{4}-\d{2}-\d{2}$/.test(value))) {
        return 'date';
      }
      if (values.every(value => /^-?\d+$/.test(value))) {
        return 'bigint';
      }
      return 'string';
    },
    shouldUseExternalTable() {
      const location = String(this.tableInfo.locationPath || '').toLowerCase();
      return location.includes('/external/') || location.includes('/external_') || location.includes('/external.');
    },
    getSchemaSqlLineClass(line, previousLine) {
      if (/^\s*`[^`]+`\s+/.test(line)) {
        return 'schema-field-line';
      }
      if (/^(COMMENT|PARTITIONED|STORED|LOCATION)\b/.test(line)) {
        return 'schema-ddl-line';
      }
      return '';
    },
    highlightSqlLine(line) {
      const placeholders = [];
      const token = html => {
        const key = `__SQL_TOKEN_${placeholders.length}__`;
        placeholders.push({ key, html });
        return key;
      };
      let html = this.escapeHtml(line)
        .replace(/(`[^`]+`)/g, match => token(`<span class="sql-identifier">${match}</span>`))
        .replace(/'([^']*)'/g, match => token(`<span class="sql-string">${match}</span>`))
        .replace(/\b(CREATE|EXTERNAL|TABLE|IF|NOT|EXISTS|COMMENT|PARTITIONED|BY|STORED|AS|LOCATION)\b/g, '<span class="sql-keyword">$1</span>')
        .replace(/\b(string|timestamp|bigint|int|double|float|decimal|date|boolean|array|map|struct)\b/gi, '<span class="sql-type">$1</span>');
      placeholders.forEach(item => {
        html = html.replace(item.key, item.html);
      });
      return html;
    },
    escapeHtml(value) {
      return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    },
    resolveErrorMessage(error) {
      const data = error && error.response && error.response.data;
      if (typeof data === 'string' && data.trim()) {
        return data;
      }
      if (data && data.message && data.message !== 'An unexpected error occurred') {
        return data.message;
      }
      if (data && data.error) {
        return data.error;
      }
      if (error && error.message) {
        return error.message;
      }
      return '未知错误';
    },
    quoteHiveIdentifier(value) {
      const raw = String(value || '').replace(/`/g, '``');
      return `\`${raw}\``;
    },
    escapeHiveString(value) {
      return String(value || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
    },
    async copyHiveCreateSql() {
      const text = this.hiveCreateSql;
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
        } else {
          const textarea = document.createElement('textarea');
          textarea.value = text;
          textarea.setAttribute('readonly', 'readonly');
          textarea.style.position = 'fixed';
          textarea.style.left = '-9999px';
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand('copy');
          document.body.removeChild(textarea);
        }
        this.$message.success('已复制 Hive 建表 SQL');
      } catch (e) {
        this.$message.error('复制失败，请手动选择复制');
      }
    },
    showOwnerModal() {
      this.ownerForm.owner = this.tableInfo.owner || '';
      this.ownerModalVisible = true;
    },
    async saveOwner() {
      this.ownerSaving = true;
      try {
        const res = await axios.put(`/api/metadata/table/${this.tableId}/owner`, {
          owner: this.ownerForm.owner
        });
        this.tableInfo = res.data || this.tableInfo;
        this.managementForm.owner = this.tableInfo.owner || '';
        this.$message.success('负责人已更新');
        this.ownerModalVisible = false;
      } catch (e) {
        this.$message.error(e.response?.data?.message || '负责人更新失败');
      } finally {
        this.ownerSaving = false;
      }
    },
  }
};
</script>

<style scoped>
.metadata-detail {
  background: #fff;
  min-height: 100%;
}
.metadata-detail >>> .ant-descriptions-bordered .ant-descriptions-item-label {
  background: #fafafa;
  color: #344054;
  text-align: right;
  white-space: nowrap;
  width: 116px;
}
.metadata-detail >>> .ant-descriptions-bordered .ant-descriptions-item-content {
  color: #475467;
  min-width: 180px;
  text-align: left;
}
.break-text {
  word-break: break-all;
}
.size-value {
  align-items: center;
  display: inline-flex;
  gap: 6px;
}
.size-help-icon {
  color: #98a2b3;
  font-size: 12px;
}
.size-inline-note {
  color: #98a2b3;
  font-size: 12px;
  margin-left: 8px;
}
.partition-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.partition-summary-item {
  min-height: 78px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.partition-summary-item span,
.partition-summary-item small {
  display: block;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}
.partition-summary-item strong {
  display: block;
  margin: 4px 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}
.path-cell {
  display: inline-block;
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}
.metadata-form {
  max-width: 980px;
}
.table-toolbar {
  margin-bottom: 12px;
  text-align: right;
}
.schema-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.schema-source-alert {
  flex: 1;
  min-width: 0;
}
.schema-json {
  min-height: 420px;
  max-height: 680px;
  margin: 0;
  padding: 18px 20px;
  overflow: auto;
  color: #dbeafe;
  font-size: 13px;
  line-height: 1.7;
  text-align: left;
  background: #0f172a;
  border: 1px solid #26344f;
  border-radius: 10px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.18);
  white-space: pre;
}
.schema-code-line {
  display: block;
  min-height: 22px;
  padding: 0 10px;
  border-left: 3px solid transparent;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
}
.schema-code-line:hover {
  background: rgba(148, 163, 184, 0.12);
}
.schema-field-line {
  margin: 2px 0;
  background: rgba(14, 165, 233, 0.12);
  border-left-color: #38bdf8;
  border-radius: 6px;
}
.schema-field-detail-line {
  background: rgba(14, 165, 233, 0.07);
  border-left-color: rgba(56, 189, 248, 0.6);
}
.schema-ddl-line {
  margin-top: 10px;
  padding-top: 8px;
  color: #fef3c7;
  background: rgba(245, 158, 11, 0.12);
  border-left-color: #f59e0b;
  border-radius: 6px;
}
.schema-json >>> .sql-identifier {
  color: #93c5fd;
  font-weight: 700;
}
.schema-json >>> .sql-string {
  color: #86efac;
}
.schema-json >>> .sql-type {
  color: #c4b5fd;
  font-weight: 600;
}
.schema-json >>> .sql-keyword {
  color: #fbbf24;
  font-weight: 700;
}
.lineage-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.lineage-chart {
  width: 100%;
  height: 600px;
  margin-top: 16px;
}
.lineage-message {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}
@media (max-width: 768px) {
  .partition-summary {
    grid-template-columns: 1fr;
  }
}
</style>
