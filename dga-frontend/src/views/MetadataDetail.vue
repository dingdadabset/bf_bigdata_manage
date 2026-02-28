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
        <a-button key="1" type="primary" icon="sync" :loading="syncing" @click="syncMetadata">同步元数据</a-button>
      </template>
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
        <div ref="lineageChart" style="width: 100%; height: 600px; margin-top: 16px;"></div>
        <a-empty v-if="!lineageData || !lineageData.nodes || lineageData.nodes.length === 0" description="暂无血缘数据" />
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
import * as echarts from 'echarts';

export default {
  data() {
    return {
      tableId: this.$route.params.id,
      tableInfo: {},
      columnData: [],
      taskData: [],
      lineageData: null,
      lineageChart: null,
      loadingColumns: false,
      loadingTasks: false,
      syncing: false,
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
      if (key === 'lineage') {
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
      }
    },
    async fetchLineage() {
        try {
            const res = await axios.get(`/api/lineage/table/${this.tableId}`);
            this.lineageData = res.data;
            if (this.lineageData && this.lineageData.nodes && this.lineageData.nodes.length > 0) {
                this.$nextTick(() => {
                    this.initChart();
                });
            }
        } catch (e) {
            this.$message.error('获取血缘信息失败');
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
            tooltip: {},
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
                links: this.lineageData.links,
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
    async syncMetadata() {
      this.syncing = true;
      try {
        await axios.post(`/api/metadata/table/${this.tableId}/sync`);
        this.$message.success('同步成功');
        this.fetchTableInfo();
        this.fetchColumns();
      } catch (e) {
        this.$message.error('同步失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.syncing = false;
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
</style>