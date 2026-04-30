<template>
  <div :class="$style['data-map']">
    <section :class="$style['search-stage']">
      <div :class="$style['stage-title']">
        <span :class="$style['stage-icon']"><a-icon type="search" /></span>
        <h1>搜索 Hive 表</h1>
      </div>

      <a-input
        v-model="filters.keyword"
        allow-clear
        size="large"
        :placeholder="searchPlaceholder"
        :class="$style['main-search']"
        @pressEnter="handleSearch"
      >
        <a-icon slot="prefix" type="search" :class="$style['search-prefix']" />
        <a-tooltip slot="suffix" title="按 Enter 搜索">
          <a-icon type="enter" :class="$style['search-suffix']" @click="handleSearch" />
        </a-tooltip>
      </a-input>

      <div :class="$style['shortcut-row']">
        <span :class="$style['shortcut-label']">热门搜索</span>
        <button v-for="word in hotSearches" :key="word" type="button" @click="applyKeyword(word)">
          {{ word }}
        </button>
      </div>
      <div :class="$style['shortcut-row']">
        <span :class="$style['shortcut-label']">我的常用</span>
        <button v-for="word in commonSearches" :key="word" type="button" @click="applyKeyword(word)">
          {{ word }}
        </button>
      </div>

      <div :class="$style['filter-tools']">
        <div :class="$style['filter-line']">
          <span :class="$style['filter-label']">数据源</span>
          <button
            type="button"
            :class="{ [$style['active-chip']]: !filters.dataSourceId }"
            @click="setDataSource(undefined)"
          >
            全部
          </button>
          <button
            v-for="ds in visibleDataSources"
            :key="ds.id"
            type="button"
            :class="{ [$style['active-chip']]: Number(filters.dataSourceId) === Number(ds.id) }"
            :title="formatDataSourceLabel(ds)"
            @click="setDataSource(ds.id)"
          >
            {{ ds.name || ds.clusterCode || ds.id }}
          </button>
        </div>

        <div :class="$style['filter-line']">
          <span :class="$style['filter-label']">数据库</span>
          <button
            type="button"
            :class="{ [$style['active-chip']]: !filters.dbName }"
            @click="setDbName('')"
          >
            全部
          </button>
          <button
            v-for="db in databaseChips"
            :key="db"
            type="button"
            :class="{ [$style['active-chip']]: filters.dbName === db }"
            @click="setDbName(db)"
          >
            {{ db }}
          </button>
        </div>

        <div v-if="dataMapSettings.showSensitiveFields" :class="$style['filter-line']">
          <span :class="$style['filter-label']">负责人</span>
          <button
            type="button"
            :class="{ [$style['active-chip']]: !filters.owner }"
            @click="setOwner('')"
          >
            全部
          </button>
          <button
            v-for="owner in ownerChips"
            :key="owner"
            type="button"
            :class="{ [$style['active-chip']]: filters.owner === owner }"
            @click="setOwner(owner)"
          >
            {{ owner }}
          </button>
        </div>
      </div>

      <div v-if="hasActiveFilters" :class="$style['active-scope']">
        <a-tag v-if="filters.keyword" color="green">关键词: {{ filters.keyword }}</a-tag>
        <a-tag color="blue">{{ selectedDataSourceText }}</a-tag>
        <a-tag :color="filters.dbName ? 'purple' : 'default'">{{ filters.dbName ? `数据库: ${filters.dbName}` : '全部数据库' }}</a-tag>
        <a-tag v-if="filters.owner && dataMapSettings.showSensitiveFields" color="orange">负责人: {{ filters.owner }}</a-tag>
        <a-button type="link" size="small" icon="close-circle" @click="resetFilters">清空筛选</a-button>
      </div>
      <div v-if="loading || hasSearched" :class="[$style['search-feedback'], loading ? $style['searching'] : $style['searched']]">
        <a-icon :type="loading ? 'loading' : 'check-circle'" />
        <span>{{ searchFeedbackText }}</span>
      </div>
    </section>

    <section v-if="!hasSearched" :class="$style['governance-placeholder']">
      <div :class="$style['placeholder-main']">
        <span :class="$style['placeholder-icon']"><a-icon type="dashboard" /></span>
        <div>
          <h2>数据治理看板预留区</h2>
          <p>这里后续可以放资产覆盖率、质量问题、负责人维护率、同步趋势等治理报表；当前先保持为查表入口。</p>
        </div>
      </div>
      <div :class="$style['placeholder-grid']">
        <div v-for="item in governanceCards" :key="item.title" :class="$style['placeholder-card']">
          <a-icon :type="item.icon" />
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </div>
      </div>
    </section>

    <a-card v-else :bordered="false" :class="$style['result-card']">
      <div slot="title" :class="$style['result-title']">
        <span>找到 {{ pagination.total }} 张表</span>
        <em>{{ resultRangeText }}</em>
      </div>
      <div slot="extra" :class="$style['result-actions']">
        <a-button size="small" icon="reload" :loading="loading" @click="fetchAssets()">刷新</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="assets"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900 }"
        row-key="id"
        size="middle"
        :class="$style['asset-table']"
        :locale="{ emptyText: emptyText }"
        @change="handleTableChange"
      >
        <span slot="asset" slot-scope="text, record">
          <div class="asset-cell">
            <span class="asset-icon"><a-icon type="table" /></span>
            <span class="asset-main">
              <a class="asset-link" :title="record.tableName" @click="openAsset(record)">
                {{ record.tableName }}
              </a>
              <span class="asset-subline">
                <a-tag color="purple">{{ record.dbName || '-' }}</a-tag>
                <span :title="record.tableComment || ''">{{ record.tableComment || '暂无表备注' }}</span>
              </span>
              <span class="asset-meta-line">
                <span v-if="dataMapSettings.showSensitiveFields">
                  <a-icon type="user" /> {{ record.owner || record.sourceOwner || '未维护负责人' }}
                </span>
                <a-tag :color="getFormatColor(record.storageFormat)">{{ record.storageFormat || '未知格式' }}</a-tag>
                <span>{{ formatSize(record.totalSize) }}</span>
                <a-tooltip :title="formatDateTime(record.syncTime)">
                  <span><a-icon type="clock-circle" /> {{ formatDate(record.syncTime) }}</span>
                </a-tooltip>
              </span>
            </span>
          </div>
        </span>
        <span slot="location" slot-scope="text, record">
          <div :class="$style['location-cell']">
            <strong :title="getDataSourceName(record)">{{ getDataSourceName(record) }}</strong>
            <span><a-icon type="environment" /> {{ getDataSourceCluster(record) }}</span>
          </div>
        </span>
        <span slot="action" slot-scope="text, record">
          <a-button type="link" size="small" icon="eye" @click="openAsset(record)">查看详情</a-button>
        </span>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'DataMap',
  data() {
    return {
      loading: false,
      hasSearched: false,
      lastSearchSummary: '',
      dataSources: [],
      assets: [],
      hotSearches: ['user', 'order', 'payment'],
      commonSearches: ['user_info', 'order_detail'],
      dataMapSettings: {
        hotKeywords: ['user', 'order', 'payment'],
        defaultSearchScope: 'ALL',
        resultSort: 'RELEVANCE',
        showSensitiveFields: false,
        defaultPageSize: 10
      },
      defaultDatabaseChips: ['default', 'baofoo_fi', 'aggr_prod', 'aggr_pay'],
      defaultOwnerChips: ['yarn', 'hive'],
      governanceCards: [
        { title: '资产覆盖', desc: '展示各集群纳管进度', icon: 'deployment-unit' },
        { title: '质量问题', desc: '汇总待处理异常', icon: 'warning' },
        { title: '负责人维护', desc: '跟踪资产责任人覆盖', icon: 'team' },
        { title: '同步趋势', desc: '观察采集成功率变化', icon: 'line-chart' }
      ],
      filters: {
        keyword: '',
        dataSourceId: undefined,
        dbName: '',
        owner: ''
      },
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: total => `共 ${total} 条`
      },
      columns: [
        { title: '表资产', dataIndex: 'tableName', key: 'asset', scopedSlots: { customRender: 'asset' }, width: 520 },
        { title: '位置', key: 'location', scopedSlots: { customRender: 'location' }, width: 230 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 120 }
      ]
    };
  },
  computed: {
    hasActiveFilters() {
      return Boolean(this.filters.keyword || this.filters.dbName || this.filters.dataSourceId || this.filters.owner);
    },
    visibleDataSources() {
      return this.dataSources.slice(0, 8);
    },
    databaseChips() {
      return this.uniqueFromAssets('dbName', this.filters.dbName, this.defaultDatabaseChips).slice(0, 8);
    },
    ownerChips() {
      if (!this.dataMapSettings.showSensitiveFields) {
        return [];
      }
      const values = this.assets.map(item => item.owner || item.sourceOwner).filter(Boolean);
      const current = this.filters.owner ? [this.filters.owner] : [];
      return Array.from(new Set([...current, ...this.defaultOwnerChips, ...values])).slice(0, 8);
    },
    selectedDataSourceText() {
      if (!this.filters.dataSourceId) return '全部数据源';
      const item = this.dataSources.find(ds => Number(ds.id) === Number(this.filters.dataSourceId));
      return item ? `数据源: ${item.name || item.id}` : '已选数据源';
    },
    resultRangeText() {
      if (!this.pagination.total) return '暂无结果';
      const start = (this.pagination.current - 1) * this.pagination.pageSize + 1;
      const end = Math.min(this.pagination.current * this.pagination.pageSize, this.pagination.total);
      return `当前 ${start}-${end}`;
    },
    emptyText() {
      return this.hasActiveFilters ? '没有找到匹配表，清空筛选后再试' : '暂无表资产，请先采集 Hive 元数据';
    },
    searchPlaceholder() {
      const scopeMap = {
        ALL: '搜索表名 / 字段 / 业务（如：订单、用户）',
        TABLE: '按表名搜索（如：order_detail）',
        COLUMN: '按字段搜索（如：user_id、create_time）',
        BUSINESS: '按备注 / 主题 / 标签搜索'
      };
      return scopeMap[this.dataMapSettings.defaultSearchScope] || scopeMap.ALL;
    },
    searchFeedbackText() {
      if (this.loading) {
        return `正在搜索${this.lastSearchSummary ? `：${this.lastSearchSummary}` : ''}，请稍候...`;
      }
      return `搜索完成，找到 ${this.pagination.total} 张表${this.lastSearchSummary ? `（${this.lastSearchSummary}）` : ''}`;
    }
  },
  created() {
    this.initFiltersFromRoute();
    this.bootstrap();
  },
  watch: {
    '$route.query': {
      handler() {
        this.initFiltersFromRoute();
        if (this.hasActiveFilters) {
          this.fetchAssets(1, this.pagination.pageSize);
        } else {
          this.clearResults();
        }
      }
    }
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.fetchSettings(), this.fetchDataSources()]);
      if (this.hasActiveFilters) {
        await this.fetchAssets(1, this.pagination.pageSize);
      }
    },
    initFiltersFromRoute() {
      const query = this.$route.query || {};
      this.filters.keyword = query.q || query.keyword || '';
      this.filters.dbName = query.dbName || '';
      this.filters.owner = query.owner || '';
      this.filters.dataSourceId = query.dataSourceId ? Number(query.dataSourceId) : undefined;
    },
    async fetchDataSources() {
      try {
        const res = await axios.get('/api/datasource');
        this.dataSources = res.data || [];
      } catch (e) {
        this.$message.error('加载数据源失败');
      }
    },
    async fetchSettings() {
      try {
        const res = await axios.get('/api/settings');
        const dataMap = (res.data && res.data.dataMap) || {};
        const pageSize = Number(dataMap.defaultPageSize) || this.pagination.pageSize;
        this.dataMapSettings = {
          ...this.dataMapSettings,
          ...dataMap,
          hotKeywords: this.normalizeArray(dataMap.hotKeywords || this.dataMapSettings.hotKeywords),
          showSensitiveFields: Boolean(dataMap.showSensitiveFields),
          defaultPageSize: pageSize
        };
        this.hotSearches = this.dataMapSettings.hotKeywords.length
          ? this.dataMapSettings.hotKeywords
          : ['user', 'order', 'payment'];
        this.pagination = {
          ...this.pagination,
          pageSize
        };
        if (!this.dataMapSettings.showSensitiveFields) {
          this.filters.owner = '';
        }
      } catch (e) {
        console.error('Failed to fetch data map settings', e);
      }
    },
    async fetchAssets(page = this.pagination.current, pageSize = this.pagination.pageSize) {
      if (!this.hasActiveFilters) {
        this.$message.info('请输入关键词或选择筛选条件后再搜索');
        this.clearResults();
        return;
      }
      this.hasSearched = true;
      this.lastSearchSummary = this.buildSearchSummary();
      this.loading = true;
      try {
        const params = {
          page: page - 1,
          size: pageSize,
          keyword: this.filters.keyword,
          dataSourceId: this.filters.dataSourceId,
          dbName: this.filters.dbName,
          owner: this.filters.owner
        };
        Object.keys(params).forEach(key => {
          if (params[key] === undefined || params[key] === '') delete params[key];
        });
        const res = await axios.get('/api/metadata/search', { params });
        const pageData = res.data || {};
        this.assets = this.sortAssets(pageData.content || []);
        this.pagination = {
          ...this.pagination,
          current: page,
          pageSize,
          total: pageData.totalElements || 0
        };
      } catch (e) {
        this.$message.error('获取表资产失败');
      } finally {
        this.loading = false;
      }
    },
    handleSearch() {
      this.fetchAssets(1, this.pagination.pageSize);
    },
    resetFilters() {
      this.filters = {
        keyword: '',
        dataSourceId: undefined,
        dbName: '',
        owner: ''
      };
      this.clearResults();
    },
    applyKeyword(keyword) {
      this.filters.keyword = keyword;
      this.handleSearch();
    },
    setDataSource(dataSourceId) {
      this.filters.dataSourceId = dataSourceId;
      if (this.hasSearched) this.handleSearch();
    },
    setDbName(dbName) {
      this.filters.dbName = dbName;
      if (this.hasSearched) this.handleSearch();
    },
    setOwner(owner) {
      if (!this.dataMapSettings.showSensitiveFields) return;
      this.filters.owner = owner;
      if (this.hasSearched) this.handleSearch();
    },
    handleTableChange(pagination) {
      this.fetchAssets(pagination.current, pagination.pageSize);
    },
    openAsset(record) {
      if (!record || !record.id) return;
      this.$router.push(`/metadata/detail/${record.id}`);
    },
    normalizeArray(value) {
      if (Array.isArray(value)) {
        return value.filter(Boolean);
      }
      if (!value) {
        return [];
      }
      return String(value).split(/[,，]/).map(item => item.trim()).filter(Boolean);
    },
    sortAssets(items) {
      const sortType = this.dataMapSettings.resultSort || 'RELEVANCE';
      if (sortType === 'SYNC_TIME_DESC') {
        return [...items].sort((a, b) => new Date(b.syncTime || 0) - new Date(a.syncTime || 0));
      }
      if (sortType === 'NAME_ASC') {
        return [...items].sort((a, b) => String(a.tableName || '').localeCompare(String(b.tableName || '')));
      }
      return items;
    },
    uniqueFromAssets(field, currentValue, defaults = []) {
      const current = currentValue ? [currentValue] : [];
      const values = this.assets.map(item => item[field]).filter(Boolean);
      return Array.from(new Set([...current, ...defaults, ...values]));
    },
    clearResults() {
      this.hasSearched = false;
      this.assets = [];
      this.pagination = {
        ...this.pagination,
        current: 1,
        total: 0
      };
      this.lastSearchSummary = '';
    },
    buildSearchSummary() {
      const parts = [];
      if (this.filters.keyword) parts.push(`关键词 ${this.filters.keyword}`);
      if (this.filters.dataSourceId) parts.push(this.selectedDataSourceText.replace('数据源: ', '数据源 '));
      if (this.filters.dbName) parts.push(`数据库 ${this.filters.dbName}`);
      if (this.filters.owner && this.dataMapSettings.showSensitiveFields) parts.push(`负责人 ${this.filters.owner}`);
      return parts.length ? parts.join(' / ') : '全部条件';
    },
    formatDataSourceLabel(ds) {
      if (!ds) return '-';
      const cluster = ds.clusterCode || ds.clusterName;
      return `${ds.name || '-'}${cluster ? ` / ${cluster}` : ''}${ds.type ? ` (${ds.type})` : ''}`;
    },
    getDataSource(record) {
      if (!record || !record.dataSourceId) return null;
      return this.dataSources.find(ds => Number(ds.id) === Number(record.dataSourceId)) || null;
    },
    getDataSourceName(record) {
      const ds = this.getDataSource(record);
      if (ds && ds.name) return ds.name;
      return record.clusterCode || record.dataSourceId || '-';
    },
    getDataSourceCluster(record) {
      const ds = this.getDataSource(record);
      return record.clusterCode || (ds && (ds.clusterCode || ds.clusterName)) || '未绑定集群';
    },
    formatSize(bytes) {
      if (!bytes && bytes !== 0) return '-';
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
      const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
      return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
    },
    formatDate(value) {
      if (!value) return '-';
      return new Date(value).toLocaleDateString('zh-CN');
    },
    formatDateTime(value) {
      if (!value) return '';
      return new Date(value).toLocaleString('zh-CN');
    },
    getFormatColor(format) {
      const value = String(format || '').toUpperCase();
      if (value.includes('ORC')) return 'blue';
      if (value.includes('PARQUET')) return 'green';
      if (value.includes('TEXT')) return 'orange';
      return 'default';
    }
  }
};
</script>

<style module>
.data-map {
  min-height: 100vh;
  padding: 24px;
  background: #f5f6f8;
}

.search-stage {
  margin-bottom: 16px;
  padding: 24px;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.stage-title {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  color: #102a43;
}

.stage-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: #eef6ff;
  color: #1677ff;
  font-size: 18px;
}

.stage-title h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  letter-spacing: 0;
}

.main-search {
  display: block;
  max-width: 860px;
  margin: 0 auto;
}

.main-search :global(.ant-input) {
  height: 54px;
  padding-right: 44px;
  padding-left: 42px;
  border-color: #dce8f5;
  border-radius: 12px;
  font-size: 16px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.main-search :global(.ant-input:hover),
.main-search :global(.ant-input:focus) {
  border-color: #85bfff;
  box-shadow: 0 8px 22px rgba(22, 119, 255, 0.12);
}

.search-prefix {
  color: #8a94a6;
  font-size: 17px;
}

.search-suffix {
  color: #98a2b3;
  cursor: pointer;
  font-size: 17px;
}

.search-suffix:hover {
  color: #1677ff;
}

.shortcut-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
}

.shortcut-label {
  color: #667085;
  font-size: 13px;
}

.shortcut-row button,
.filter-line button {
  height: 28px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  color: #475467;
  cursor: pointer;
  font-size: 13px;
  line-height: 26px;
  transition: all 0.18s ease;
}

.shortcut-row button:hover,
.filter-line button:hover {
  border-color: #91caff;
  color: #1677ff;
}

.filter-tools {
  display: grid;
  gap: 10px;
  max-width: 960px;
  margin: 18px auto 0;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.filter-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.filter-label {
  width: 54px;
  color: #667085;
  font-size: 13px;
  font-weight: 600;
}

.filter-line .active-chip {
  border-color: #1677ff;
  background: #e8f4ff;
  color: #0f6fdc;
  font-weight: 600;
}

.active-scope {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: center;
  max-width: 960px;
  margin: 14px auto 0;
}

.search-feedback {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  max-width: 860px;
  margin: 14px auto 0;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.searching {
  border: 1px solid #d8e9ff;
  background: #f0f7ff;
  color: #0f6fdc;
}

.searched {
  border: 1px solid #d9f7be;
  background: #f6ffed;
  color: #389e0d;
}

.governance-placeholder {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 1fr);
  gap: 16px;
  align-items: stretch;
  padding: 24px;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.placeholder-main {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  min-height: 150px;
  padding: 22px;
  border: 1px dashed #d9e8ff;
  border-radius: 8px;
  background: #f8fbff;
}

.placeholder-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #e8f4ff;
  color: #1677ff;
  font-size: 20px;
}

.placeholder-main h2 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 650;
  letter-spacing: 0;
}

.placeholder-main p {
  max-width: 680px;
  margin: 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.8;
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.placeholder-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 112px;
  padding: 16px;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #fff;
}

.placeholder-card :global(.anticon) {
  color: #1677ff;
  font-size: 18px;
}

.placeholder-card strong {
  color: #1f2937;
  font-size: 15px;
}

.placeholder-card span {
  color: #98a2b3;
  font-size: 13px;
  line-height: 1.6;
}

.result-card {
  border: 1px solid #eeeeee;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.result-card :global(.ant-card-head) {
  border-bottom-color: #eeeeee;
}

.result-title {
  display: flex;
  gap: 8px;
  align-items: center;
}

.result-title em {
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
}

.result-actions {
  display: flex;
  align-items: center;
}

.asset-table :global(.ant-table-thead > tr > th) {
  color: #52606d;
  font-weight: 600;
  background: #fafafa;
}

.asset-table :global(.ant-table-tbody > tr > td) {
  transition: background 0.2s ease;
}

.asset-table :global(.ant-table-tbody > tr:hover > td) {
  background: #f7fbff;
}

.asset-table :global(.asset-cell) {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
}

.asset-table :global(.asset-icon) {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid #d8e9ff;
  border-radius: 10px;
  background: #f0f7ff;
  color: #1677ff;
  font-size: 16px;
}

.asset-table :global(.asset-main) {
  min-width: 0;
}

.asset-table :global(.asset-link) {
  display: block;
  max-width: 390px;
  overflow: hidden;
  color: #0f6fdc;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-table :global(.asset-subline) {
  display: flex;
  gap: 6px;
  align-items: center;
  max-width: 390px;
  margin-top: 5px;
  color: #667085;
  font-size: 12px;
}

.asset-table :global(.asset-meta-line) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
  max-width: 460px;
  margin-top: 6px;
  color: #98a2b3;
  font-size: 12px;
}

.asset-table :global(.asset-meta-line span) {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.asset-table :global(.asset-subline > span:last-child) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 200px;
}

.location-cell strong {
  max-width: 200px;
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-cell span {
  max-width: 200px;
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner-chip,
.owner-empty {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner-chip {
  color: #1f2937;
}

.owner-empty {
  color: #98a2b3;
}

.tech-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
  color: #667085;
  font-size: 12px;
}

.tech-cell span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 768px) {
  .data-map {
    padding: 14px;
  }

  .search-stage {
    padding: 18px;
  }

  .stage-title h1 {
    font-size: 24px;
  }

  .main-search :global(.ant-input) {
    height: 50px;
    font-size: 14px;
  }

  .filter-label {
    width: 100%;
  }

  .governance-placeholder {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .placeholder-grid {
    grid-template-columns: 1fr;
  }
}
</style>
