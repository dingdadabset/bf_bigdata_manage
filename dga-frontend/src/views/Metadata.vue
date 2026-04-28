<template>
  <div class="metadata-view">
    <!-- Dashboard Stats -->
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="纳管表总数"
            :value="pagination.total"
            style="margin-right: 50px"
          >
            <template #prefix>
              <a-icon type="table" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="存储总量" :value="formatSize(stats.totalSize)">
            <template #prefix>
              <a-icon type="hdd" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日更新" :value="stats.todaySyncCount" value-style="color: #cf1322">
            <template #prefix>
              <a-icon type="arrow-up" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="治理评分" :value="stats.avgScore" suffix="/ 100">
            <template #prefix>
              <a-icon type="safety-certificate" theme="twoTone" two-tone-color="#52c41a" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="filter-card" title="查询筛选">
      <a-form layout="inline">
        <a-form-item label="数据源">
          <a-select v-model="filters.dataSourceId" style="width: 200px" allowClear placeholder="选择数据源">
            <a-select-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">
              <a-icon type="database" /> {{ ds.name }} ({{ ds.type }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据库">
          <a-input v-model="filters.dbName" placeholder="输入数据库名" allowClear>
            <a-icon slot="prefix" type="hdd" />
          </a-input>
        </a-form-item>
        <a-form-item label="主题">
          <a-select v-model="filters.themeId" style="width: 160px" allowClear placeholder="选择主题">
            <a-select-option v-for="theme in themes" :key="theme.id" :value="theme.id">
              {{ theme.themeName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标签">
          <a-select v-model="filters.tagId" style="width: 150px" allowClear placeholder="选择标签">
            <a-select-option v-for="tag in tags" :key="tag.id" :value="tag.id">
              {{ tag.tagName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生命周期">
          <a-select v-model="filters.lifecycleStatus" style="width: 150px" allowClear placeholder="生命周期">
            <a-select-option value="ONLINE">在线</a-select-option>
            <a-select-option value="DEPRECATED">已废弃</a-select-option>
            <a-select-option value="OFFLINE">已下线</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="负责人">
          <a-input v-model="filters.owner" placeholder="负责人" style="width: 140px" allowClear />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input v-model="filters.keyword" placeholder="表名/字段/备注/负责人" style="width: 240px">
            <a-icon slot="prefix" type="search" />
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" icon="search" @click="onSearch">查询</a-button>
          <a-button icon="reload" style="margin-left: 8px" @click="resetFilters">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="6">
        <a-card :bordered="false" title="技术资产目录" class="catalog-card">
          <a-tree
            :tree-data="catalogTree"
            :loading="catalogLoading"
            show-icon
            default-expand-all
            @select="onCatalogSelect"
          >
            <a-icon slot="switcherIcon" type="down" />
          </a-tree>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="18">
        <a-card :bordered="false" title="元数据列表">
          <div slot="extra">
            <a-button icon="sync" :loading="collectingAll" @click="collectAll">采集全部</a-button>
          </div>
          <a-table
            class="metadata-table"
            :columns="columns"
            :data-source="data"
            :loading="loading"
            row-key="id"
            :pagination="pagination"
            :scroll="{ x: 1600 }"
            @change="handleTableChange"
          >
            <span slot="clusterCode" slot-scope="text">
              <a-tag color="blue">{{ text || '-' }}</a-tag>
            </span>
            <span slot="dbName" slot-scope="text">
              <a-tag color="purple" class="db-tag">{{ text }}</a-tag>
            </span>
            <span slot="tableName" slot-scope="text, record">
              <div class="asset-name-cell">
                <a class="table-link" :title="text" @click="$router.push(`/metadata/detail/${record.id}`)">
                   <a-icon type="table" style="margin-right: 4px" />{{ text }}
                </a>
                <div class="table-comment" :title="record.tableComment || ''">{{ record.tableComment || '-' }}</div>
              </div>
            </span>
            <span slot="owner" slot-scope="text, record">
              <span class="owner-text" :title="text">{{ text || '-' }}</span>
              <a-tag v-if="record.ownerSource === 'MANUAL'" color="gold" style="margin-left: 6px">手动</a-tag>
            </span>
            <span slot="lifecycleStatus" slot-scope="text">
              <a-tag :color="getLifecycleColor(text)">{{ getLifecycleText(text) }}</a-tag>
            </span>
            <span slot="storageFormat" slot-scope="text">
              <a-tag :color="getFormatColor(text)">{{ text || '-' }}</a-tag>
            </span>
            <span slot="size" slot-scope="text">
              {{ formatSize(text) }}
            </span>
            <span slot="syncTime" slot-scope="text">
              <a-tooltip :title="text ? new Date(text).toLocaleString() : ''">
                <span>{{ text ? new Date(text).toLocaleDateString() : '-' }}</span>
              </a-tooltip>
            </span>
            <span slot="action" slot-scope="text, record">
              <a-button v-if="canManageOwner" type="link" size="small" icon="user" @click="showOwnerModal(record)">
                负责人
              </a-button>
            </span>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      title="维护资产负责人"
      :visible="ownerModalVisible"
      :confirm-loading="ownerSaving"
      @ok="saveOwner"
      @cancel="ownerModalVisible = false"
    >
      <a-form-model layout="vertical">
        <a-form-model-item label="资产">
          <a-input :value="ownerRecord ? `${ownerRecord.dbName}.${ownerRecord.tableName}` : ''" disabled />
        </a-form-model-item>
        <a-form-model-item label="负责人">
          <a-input v-model="ownerForm.owner" placeholder="请输入负责人账号或姓名" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import { canDelete } from '../utils/currentUser';

export default {
  data() {
    return {
      loading: false,
      collectingAll: false,
      catalogLoading: false,
      data: [],
      dataSources: [],
      themes: [],
      tags: [],
      catalogTree: [],
      filters: {
        dataSourceId: undefined,
        dbName: '',
        themeId: undefined,
        tagId: undefined,
        lifecycleStatus: undefined,
        owner: '',
        keyword: ''
      },
      ownerModalVisible: false,
      ownerSaving: false,
      ownerRecord: null,
      ownerForm: {
        owner: ''
      },
      stats: {
        tableCount: 0,
        totalSize: 0,
        todaySyncCount: 0,
        avgScore: 0
      },
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: total => `共 ${total} 条`
      },
      columns: [
        { title: '集群', dataIndex: 'clusterCode', key: 'clusterCode', scopedSlots: { customRender: 'clusterCode' }, width: 110 },
        { title: '数据库', dataIndex: 'dbName', key: 'dbName', scopedSlots: { customRender: 'dbName' }, width: 170 },
        { title: '表名/备注', dataIndex: 'tableName', key: 'tableName', scopedSlots: { customRender: 'tableName' }, width: 300 },
        { title: '负责人', dataIndex: 'owner', key: 'owner', scopedSlots: { customRender: 'owner' }, width: 150 },
        { title: '生命周期', dataIndex: 'lifecycleStatus', key: 'lifecycleStatus', scopedSlots: { customRender: 'lifecycleStatus' }, width: 120 },
        { title: 'Hive Owner', dataIndex: 'sourceOwner', key: 'sourceOwner', width: 130 },
        { title: '存储格式', dataIndex: 'storageFormat', key: 'storageFormat', scopedSlots: { customRender: 'storageFormat' }, width: 120 },
        { title: '大小', dataIndex: 'totalSize', key: 'totalSize', scopedSlots: { customRender: 'size' }, width: 110 },
        { title: '记录数', dataIndex: 'recordCount', key: 'recordCount', width: 110 },
        { title: '同步时间', dataIndex: 'syncTime', key: 'syncTime', scopedSlots: { customRender: 'syncTime' }, width: 150 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 120, fixed: 'right' }
      ]
    };
  },
  computed: {
    canManageOwner() {
      return canDelete();
    }
  },
  created() {
    this.initFiltersFromRoute();
    this.fetchDataSources();
    this.fetchThemes();
    this.fetchTags();
    this.fetchCatalogTree();
    this.fetchStats();
    this.fetchMetadata(1, this.pagination.pageSize);
  },
  watch: {
    '$route.query': {
      handler() {
        this.initFiltersFromRoute();
        this.fetchMetadata(1, this.pagination.pageSize);
      }
    }
  },
  methods: {
    initFiltersFromRoute() {
      const query = this.$route.query || {};
      this.filters.keyword = query.q || query.keyword || '';
      this.filters.dbName = query.dbName || '';
      this.filters.dataSourceId = query.dataSourceId ? Number(query.dataSourceId) : undefined;
      this.filters.themeId = query.themeId ? Number(query.themeId) : undefined;
      this.filters.tagId = query.tagId ? Number(query.tagId) : undefined;
      this.filters.lifecycleStatus = query.lifecycleStatus || undefined;
      this.filters.owner = query.owner || '';
    },
    async fetchStats() {
      try {
        const res = await axios.get('/api/metadata/stats');
        this.stats = res.data;
        // Sync pagination total
        this.pagination.total = res.data.tableCount;
      } catch (e) {
        console.error('Failed to fetch stats', e);
      }
    },
    getFormatColor(format) {
      if (!format) return 'default';
      const f = format.toUpperCase();
      if (f.includes('ORC')) return 'green';
      if (f.includes('PARQUET')) return 'blue';
      if (f.includes('TEXT')) return 'orange';
      return 'cyan';
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
    async fetchDataSources() {
      try {
        const res = await axios.get('/api/datasource');
        this.dataSources = res.data || [];
      } catch (e) {
        console.error('Fetch data sources failed', e);
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
    onSearch() {
      this.pagination.current = 1;
      this.fetchMetadata(1, this.pagination.pageSize);
    },
    resetFilters() {
      this.filters = {
        dataSourceId: undefined,
        dbName: '',
        themeId: undefined,
        tagId: undefined,
        lifecycleStatus: undefined,
        owner: '',
        keyword: ''
      };
      this.onSearch();
    },
    async fetchCatalogTree() {
      this.catalogLoading = true;
      try {
        const params = {};
        if (this.filters.dataSourceId) params.dataSourceId = this.filters.dataSourceId;
        const res = await axios.get('/api/metadata/catalog/tree', { params });
        this.catalogTree = res.data || [];
      } catch (e) {
        this.$message.error('加载资产目录失败');
      } finally {
        this.catalogLoading = false;
      }
    },
    onCatalogSelect(selectedKeys, info) {
      const node = info && info.node && info.node.dataRef;
      if (!node) return;
      if (node.type === 'table' && node.tableId) {
        this.$router.push(`/metadata/detail/${node.tableId}`);
        return;
      }
      if (node.dataSourceId) {
        this.filters.dataSourceId = node.dataSourceId;
      }
      this.filters.dbName = node.type === 'database' ? node.dbName : '';
      this.onSearch();
    },
    async fetchMetadata(page = 1, pageSize = 10) {
      this.loading = true;
      try {
        const params = {
          page: page - 1,
          size: pageSize,
          ...this.filters
        };
        // Clean up undefined/empty params
        Object.keys(params).forEach(key => {
            if (params[key] === undefined || params[key] === '') {
                delete params[key];
            }
        });

        const response = await axios.get('/api/metadata/search', { params });
        const pageData = response.data || {};
        this.data = pageData.content || [];
        this.pagination = {
          ...this.pagination,
          current: page,
          pageSize,
          total: pageData.totalElements || 0
        };
      } catch (e) {
        this.$message.error('获取元数据失败');
      } finally {
        this.loading = false;
      }
    },
    formatSize(bytes) {
      if (!bytes && bytes !== 0) return '-';
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.fetchMetadata(pagination.current, pagination.pageSize);
    },
    showOwnerModal(record) {
      this.ownerRecord = record;
      this.ownerForm.owner = record.owner || '';
      this.ownerModalVisible = true;
    },
    async saveOwner() {
      if (!this.ownerRecord) return;
      this.ownerSaving = true;
      try {
        const res = await axios.put(`/api/metadata/table/${this.ownerRecord.id}/owner`, {
          owner: this.ownerForm.owner
        });
        Object.assign(this.ownerRecord, res.data || {});
        this.$message.success('负责人已更新');
        this.ownerModalVisible = false;
      } catch (e) {
        this.$message.error(e.response?.data?.message || '负责人更新失败');
      } finally {
        this.ownerSaving = false;
      }
    },
    async collectAll() {
      this.collectingAll = true;
      try {
        const res = await axios.post('/api/metadata/collect/all');
        this.$message.success(`已提交 ${res.data?.length || 0} 个采集任务`);
      } catch (e) {
        this.$message.error(e.response?.data?.message || '采集全部失败');
      } finally {
        this.collectingAll = false;
      }
    }
  }
};
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.table-operations {
  margin-bottom: 16px;
}
.catalog-card {
  margin-bottom: 16px;
}
.metadata-table >>> .ant-table-body {
  overflow-x: auto !important;
}
.metadata-table >>> .ant-table-thead > tr > th,
.metadata-table >>> .ant-table-tbody > tr > td {
  white-space: nowrap;
  vertical-align: middle;
}
.metadata-table >>> .ant-table-thead > tr > th {
  word-break: keep-all;
}
.db-tag {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}
.asset-name-cell {
  min-width: 0;
}
.table-link {
  display: block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-comment {
  color: #667085;
  font-size: 12px;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 20px;
}
.owner-text {
  display: inline-block;
  max-width: 92px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}
</style>
