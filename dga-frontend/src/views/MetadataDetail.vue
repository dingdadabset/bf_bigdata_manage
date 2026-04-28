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
            <a-descriptions-item label="大小">
              {{ formatSize(tableInfo.totalSize) }}
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
          <a-descriptions-item label="大小">{{ formatSize(tableInfo.totalSize) }}</a-descriptions-item>
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
        <a-table :columns="columns" :data-source="columnData" row-key="id" :pagination="false" :loading="loadingColumns" :scroll="{ x: 900 }">
          <span slot="columnType" slot-scope="text, record">
            <a-tag color="green">{{ text || record.column_type || record.dataType || '-' }}</a-tag>
          </span>
          <span slot="isPrimaryKey" slot-scope="text">
            <a-icon v-if="text" type="key" style="color: #faad14" />
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'partitions'">
        <a-alert
          :message="`当前表分区总数：${partitionCount || 0}，页面展示最近采集的最新分区`"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-table :columns="partitionColumns" :data-source="partitionData" row-key="id" :pagination="{ pageSize: 10 }" :loading="loadingPartitions" :scroll="{ x: 1100 }">
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
      partitionData: [],
      partitionCount: 0,
      taskData: [],
      permissionsData: [],
      metricsData: [],
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
      loadingColumns: false,
      loadingPartitions: false,
      loadingTasks: false,
      lineageLoading: false,
      lineageCollecting: false,
      lineageTasksLoading: false,
      loadingPermissions: false,
      loadingMetrics: false,
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
      ]
    };
  },
  computed: {
    canManageOwner() {
      return canDelete();
    },
    breadcrumbRoutes() {
      return [
        { path: '/metadata', breadcrumbName: '元数据列表' },
        { path: '', breadcrumbName: this.tableInfo.dbName || '...' },
        { path: '', breadcrumbName: this.tableInfo.tableName || '...' },
      ];
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
        this.fetchSchedulerEndpoints();
      } catch (e) {
        this.$message.error('获取表详情失败');
      }
    },
    async fetchColumns() {
      this.loadingColumns = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/columns`);
        this.columnData = res.data;
      } catch (e) {
        this.$message.error('获取字段信息失败');
      } finally {
        this.loadingColumns = false;
      }
    },
    async fetchPartitions() {
      this.loadingPartitions = true;
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}/partitions`);
        this.partitionCount = res.data?.partitionCount || 0;
        this.partitionData = res.data?.items || [];
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
        const endpointTypes = ['AZKABAN_DB', 'DOLPHINSCHEDULER_DB'];
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
    handleBack() {
      this.$router.push('/metadata');
    },
    onTabChange(key, type) {
      this[type] = key;
      if (key === 'lineage') {
        this.fetchSchedulerEndpoints();
        this.fetchLineageTasks();
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
      } else if (key === 'permissions' && this.permissionsData.length === 0) {
        this.fetchPermissions();
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
          this.fetchLineageTasks()
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
    formatSize(bytes) {
      if (!bytes && bytes !== 0) return '-';
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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
.break-text {
  word-break: break-all;
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
</style>
