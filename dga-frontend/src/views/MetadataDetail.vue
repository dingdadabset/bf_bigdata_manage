<template>
  <div class="metadata-detail">
    <a-page-header
      style="border: 1px solid rgb(235, 237, 240)"
      @back="handleBack"
      :title="tableInfo.tableName"
      :sub-title="tableInfo.dbName"
      :breadcrumb="{ props: { routes: breadcrumbRoutes } }"
    >
      <template slot="tags">
        <a-tag color="blue">Hive</a-tag>
        <a-tag color="green">已上线</a-tag>
        <a-tag color="orange">P1 核心</a-tag>
      </template>
      <div class="content">
        <div class="main">
          <a-descriptions size="small" :column="2">
            <a-descriptions-item label="负责人">
              <a-avatar size="small" icon="user" style="margin-right: 8px" />
              {{ tableInfo.owner || '-' }}
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
          </a-descriptions>
        </div>
      </div>
    </a-page-header>

    <a-card style="margin-top: 24px" :bordered="false" :tab-list="tabList" :active-tab-key="activeTabKey" @tabChange="key => onTabChange(key, 'activeTabKey')">
      <div v-if="activeTabKey === 'schema'">
        <a-table :columns="columns" :data-source="columnData" row-key="id" :pagination="false" :loading="loadingColumns">
          <span slot="columnType" slot-scope="text, record">
            <a-tag color="green">{{ text || record.column_type || record.dataType || '-' }}</a-tag>
          </span>
          <span slot="isPrimaryKey" slot-scope="text">
            <a-icon v-if="text" type="key" style="color: #faad14" />
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'tasks'">
        <a-table :columns="taskColumns" :data-source="taskData" row-key="id" :pagination="false" :loading="loadingTasks">
          <span slot="taskStatus" slot-scope="text">
            <a-tag v-if="text === 0" color="red">待处理</a-tag>
            <a-tag v-else-if="text === 1" color="green">已处理</a-tag>
            <a-tag v-else color="grey">豁免</a-tag>
          </span>
          <span slot="createTime" slot-scope="text">
            {{ text ? new Date(text).toLocaleString() : '-' }}
          </span>
        </a-table>
      </div>
      <div v-else-if="activeTabKey === 'lineage'">
        <a-empty description="血缘图谱功能开发中..." image="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg">
           <a-button type="primary">查看 Mock 示例</a-button>
        </a-empty>
      </div>
      <div v-else-if="activeTabKey === 'preview'">
        <a-alert message="数据预览需申请权限" type="info" show-icon style="margin-bottom: 16px" />
        <a-table :columns="columns" :data-source="[]" :pagination="false" size="small" bordered locale="{ emptyText: '暂无预览数据' }" />
      </div>
    </a-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      tableId: this.$route.params.id,
      tableInfo: {},
      columnData: [],
      taskData: [],
      loadingColumns: false,
      loadingTasks: false,
      activeTabKey: 'schema',
      tabList: [
        {
          key: 'schema',
          tab: '字段结构',
        },
        {
          key: 'lineage',
          tab: '血缘关系',
        },
        {
          key: 'preview',
          tab: '数据预览',
        },
        {
          key: 'tasks',
          tab: '治理评价',
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
      ]
    };
  },
  computed: {
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
    this.fetchTasks();
  },
  methods: {
    async fetchTableInfo() {
      try {
        const res = await axios.get(`/api/metadata/table/${this.tableId}`);
        this.tableInfo = res.data;
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
    handleBack() {
      this.$router.push('/metadata');
    },
    onTabChange(key, type) {
      this[type] = key;
    },
    formatSize(bytes) {
      if (!bytes && bytes !== 0) return '-';
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
  }
};
</script>

<style scoped>
.metadata-detail {
  background: #fff;
  min-height: 100%;
}
</style>