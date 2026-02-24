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
          <a-statistic title="存储总量" value="1.2" suffix="PB">
            <template #prefix>
              <a-icon type="hdd" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日新增" :value="12" value-style="color: #cf1322">
            <template #prefix>
              <a-icon type="arrow-up" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="治理评分" :value="88.5" suffix="/ 100">
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
        <a-form-item label="关键词">
          <a-input v-model="filters.keyword" placeholder="表名/描述" style="width: 200px">
            <a-icon slot="prefix" type="search" />
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" icon="search" @click="onSearch">查询</a-button>
          <a-button icon="reload" style="margin-left: 8px" @click="resetFilters">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false" title="元数据列表">
      <div slot="extra">
        <a-radio-group default-value="all">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="mine">我的关注</a-radio-button>
          <a-radio-button value="recent">最近访问</a-radio-button>
        </a-radio-group>
      </div>
      
      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <span slot="dbName" slot-scope="text">
          <a-tag color="purple">{{ text }}</a-tag>
        </span>
        <span slot="tableName" slot-scope="text, record">
          <a @click="$router.push(`/metadata/detail/${record.id}`)">
             <a-icon type="table" style="margin-right: 4px" />{{ text }}
          </a>
        </span>
        <span slot="storageFormat" slot-scope="text">
          <a-tag :color="getFormatColor(text)">{{ text }}</a-tag>
        </span>
        <span slot="size" slot-scope="text">
          {{ formatSize(text) }}
        </span>
        <span slot="updatedAt" slot-scope="text">
          <a-tooltip :title="text ? new Date(text).toLocaleString() : ''">
            <span>{{ text ? new Date(text).toLocaleDateString() : '-' }}</span>
          </a-tooltip>
        </span>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      loading: false,
      data: [],
      dataSources: [],
      filters: {
        dataSourceId: undefined,
        dbName: '',
        keyword: ''
      },
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: total => `共 ${total} 条`
      },
      columns: [
        { title: '数据库', dataIndex: 'dbName', key: 'dbName', scopedSlots: { customRender: 'dbName' } },
        { title: '表名', dataIndex: 'tableName', key: 'tableName', scopedSlots: { customRender: 'tableName' } },
        { title: '负责人', dataIndex: 'owner', key: 'owner' },
        { title: '存储格式', dataIndex: 'storageFormat', key: 'storageFormat', scopedSlots: { customRender: 'storageFormat' } },
        { title: '大小', dataIndex: 'totalSize', key: 'totalSize', scopedSlots: { customRender: 'size' } },
        { title: '记录数', dataIndex: 'recordCount', key: 'recordCount' },
        { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', scopedSlots: { customRender: 'updatedAt' } }
      ]
    };
  },
  mounted() {
    this.fetchDataSources();
    this.fetchMetadata(1, this.pagination.pageSize);
  },
  methods: {
    getFormatColor(format) {
      if (!format) return 'default';
      const f = format.toUpperCase();
      if (f.includes('ORC')) return 'green';
      if (f.includes('PARQUET')) return 'blue';
      if (f.includes('TEXT')) return 'orange';
      return 'cyan';
    },
    async fetchDataSources() {
      try {
        const res = await axios.get('/api/datasource');
        this.dataSources = res.data || [];
      } catch (e) {
        console.error('Fetch data sources failed', e);
      }
    },
    onSearch() {
      this.pagination.current = 1;
      this.fetchMetadata(1, this.pagination.pageSize);
    },
    resetFilters() {
      this.filters = { dataSourceId: undefined, dbName: '', keyword: '' };
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

        const response = await axios.get('/api/metadata/tables/page', { params });
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
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.fetchMetadata(pagination.current, pagination.pageSize);
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
</style>
