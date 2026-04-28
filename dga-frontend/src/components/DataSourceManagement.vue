<template>
  <div class="ds-management">
    <div class="module-header">
      <div>
        <h1>数据源接入</h1>
        <p>展示历史数据源连接，并自动同步环境资源中的 HIVE_METASTORE_DB 采集端点</p>
      </div>
      <div class="header-actions">
        <a-button icon="apartment" @click="$router.push('/environment-resources')">配置环境资源</a-button>
        <a-button icon="reload" :loading="loading" @click="fetchDataSources">刷新</a-button>
        <a-button type="primary" icon="sync" :loading="collectingAll" @click="collectAll">采集全部</a-button>
      </div>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span>数据源总数</span>
        <strong>{{ dataSources.length }}</strong>
      </div>
      <div class="summary-item">
        <span>Hive 数据源</span>
        <strong>{{ hiveCount }}</strong>
      </div>
      <div class="summary-item">
        <span>运行中</span>
        <strong>{{ runningCount }}</strong>
      </div>
      <div class="summary-item">
        <span>采集成功</span>
        <strong>{{ successCount }}</strong>
      </div>
      <div class="summary-item">
        <span>采集失败</span>
        <strong>{{ failedCount }}</strong>
      </div>
    </div>

    <div class="toolbar">
      <a-input-search
        v-model="searchQuery"
        allow-clear
        placeholder="搜索集群、类型、数据源或 JDBC 地址"
        style="max-width: 420px"
      />
    </div>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="filteredDataSources"
      :loading="loading"
      :scroll="{ x: 1320 }"
      :pagination="{ pageSize: 10 }"
      class="datasource-table"
    >
      <template slot="cluster" slot-scope="text, record">
        <div class="primary-text">{{ record.clusterName || record.clusterCode || '-' }}</div>
        <div class="muted-text">{{ record.clusterCode || '-' }}</div>
      </template>
      <template slot="name" slot-scope="text">
        <a-tag :color="typeColor(text)">{{ text || '-' }}</a-tag>
        <span>{{ text }}</span>
      </template>
      <template slot="source" slot-scope="text, record">
        <a-tag :color="record.endpointId ? 'green' : 'default'">
          {{ record.endpointId ? '环境资源' : '历史配置' }}
        </a-tag>
      </template>
      <template slot="url" slot-scope="text">
        <span class="mono" :title="text">{{ text }}</span>
      </template>
      <template slot="status" slot-scope="text">
        <a-tag :color="text === 'ACTIVE' ? 'green' : 'default'">{{ text || 'UNKNOWN' }}</a-tag>
      </template>
      <template slot="lastSyncStatus" slot-scope="text">
        <a-tag :color="syncStatusColor(text)">{{ syncStatusLabel(text) }}</a-tag>
      </template>
      <template slot="lastSyncTime" slot-scope="text">
        {{ text ? new Date(text).toLocaleString() : '-' }}
      </template>
      <template slot="lastSyncMessage" slot-scope="text">
        <span class="message-text" :title="text">{{ text || '-' }}</span>
      </template>
      <template slot="action" slot-scope="text, record">
        <a-space>
          <a-button type="link" size="small" icon="thunderbolt" :loading="record._testing" @click="testConnection(record)">
            测试
          </a-button>
          <a-button type="link" size="small" icon="sync" :loading="record._collecting" @click="collectMetadata(record)">
            采集
          </a-button>
          <a-button type="link" size="small" icon="profile" @click="viewMetadata(record)">
            资产
          </a-button>
        </a-space>
      </template>
    </a-table>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'DataSourceManagement',
  data() {
    return {
      loading: false,
      collectingAll: false,
      dataSources: [],
      searchQuery: '',
      columns: [
        { title: '集群', key: 'cluster', scopedSlots: { customRender: 'cluster' }, width: 180 },
        { title: '数据源', dataIndex: 'name', width: 220 },
        { title: '类型', dataIndex: 'type', scopedSlots: { customRender: 'name' }, width: 120 },
        { title: '来源', key: 'source', scopedSlots: { customRender: 'source' }, width: 120 },
        { title: '端点ID', dataIndex: 'endpointId', width: 90 },
        { title: 'JDBC URL', dataIndex: 'url', scopedSlots: { customRender: 'url' }, width: 320 },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' }, width: 110 },
        { title: '最近采集', dataIndex: 'lastSyncStatus', scopedSlots: { customRender: 'lastSyncStatus' }, width: 120 },
        { title: '采集时间', dataIndex: 'lastSyncTime', scopedSlots: { customRender: 'lastSyncTime' }, width: 210 },
        { title: '结果信息', dataIndex: 'lastSyncMessage', scopedSlots: { customRender: 'lastSyncMessage' }, width: 220 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 190, fixed: 'right' }
      ]
    };
  },
  computed: {
    filteredDataSources() {
      const q = (this.searchQuery || '').trim().toLowerCase();
      if (!q) return this.dataSources;
      return this.dataSources.filter(item => {
        return [item.name, item.type, item.clusterCode, item.clusterName, item.url]
          .filter(Boolean)
          .some(value => String(value).toLowerCase().includes(q));
      });
    },
    runningCount() {
      return this.dataSources.filter(item => item.lastSyncStatus === 'RUNNING').length;
    },
    hiveCount() {
      return this.dataSources.filter(item => (item.type || '').toUpperCase() === 'HIVE').length;
    },
    successCount() {
      return this.dataSources.filter(item => item.lastSyncStatus === 'SUCCESS').length;
    },
    failedCount() {
      return this.dataSources.filter(item => item.lastSyncStatus === 'FAILED').length;
    }
  },
  mounted() {
    this.fetchDataSources();
  },
  methods: {
    async fetchDataSources() {
      this.loading = true;
      try {
        const res = await axios.get('/api/datasource');
        this.dataSources = (res.data || []).map(item => ({
          ...item,
          _testing: false,
          _collecting: false
        }));
      } catch (e) {
        this.$message.error('加载数据源失败');
      } finally {
        this.loading = false;
      }
    },
    async testConnection(record) {
      this.$set(record, '_testing', true);
      try {
        const res = await axios.post(`/api/datasource/test/${record.id}`);
        if (res.data) {
          this.$message.success('连接测试成功');
        } else {
          this.$message.error('连接测试失败');
        }
      } catch (e) {
        this.$message.error('连接测试失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.$set(record, '_testing', false);
      }
    },
    async collectMetadata(record) {
      this.$set(record, '_collecting', true);
      try {
        const res = await axios.post(`/api/metadata/collect/${record.id}`);
        record.lastSyncStatus = 'RUNNING';
        record.lastSyncMessage = res.data?.message || '元数据采集已启动';
        this.$message.success('采集任务已提交');
      } catch (e) {
        this.$message.error('采集任务提交失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.$set(record, '_collecting', false);
      }
    },
    async collectAll() {
      this.collectingAll = true;
      try {
        const res = await axios.post('/api/metadata/collect/all');
        this.$message.success(`已提交 ${res.data?.length || 0} 个采集任务`);
        this.fetchDataSources();
      } catch (e) {
        this.$message.error('采集全部失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.collectingAll = false;
      }
    },
    viewMetadata(record) {
      this.$router.push({
        path: '/metadata',
        query: { dataSourceId: record.id }
      });
    },
    syncStatusColor(status) {
      if (status === 'SUCCESS') return 'green';
      if (status === 'FAILED') return 'red';
      if (status === 'RUNNING') return 'blue';
      return 'default';
    },
    syncStatusLabel(status) {
      if (status === 'SUCCESS') return '成功';
      if (status === 'FAILED') return '失败';
      if (status === 'RUNNING') return '运行中';
      return '未采集';
    },
    typeColor(type) {
      const value = (type || '').toUpperCase();
      if (value === 'HIVE') return 'orange';
      if (value === 'STARROCKS') return 'blue';
      if (value === 'MYSQL') return 'cyan';
      return 'default';
    }
  }
};
</script>

<style scoped>
.ds-management {
  background: #fff;
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
.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.summary-item {
  border: 1px solid #edf0f5;
  border-radius: 4px;
  padding: 16px;
  background: #fafcff;
}
.summary-item span {
  display: block;
  color: #667085;
  margin-bottom: 8px;
}
.summary-item strong {
  font-size: 24px;
}
.toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}
.primary-text {
  font-weight: 500;
}
.mono {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.message-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.datasource-table >>> .ant-table-body {
  overflow-x: auto !important;
}
@media (max-width: 900px) {
  .module-header,
  .header-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .summary-row {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
}
</style>
