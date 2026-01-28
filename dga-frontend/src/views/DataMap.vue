<template>
  <div :class="$style['data-map']">
    <!-- Search Header Section -->
    <div :class="$style['search-header']">
      <div :class="$style['search-container']">
        <a-space :size="16" align="center" :class="$style['title-row']">
          <a-icon type="cloud-server" :class="$style['title-icon']" />
          <h1 :class="$style['page-title']">数据地图</h1>
        </a-space>
        
        <div :class="$style['search-wrapper']">
          <a-input
            v-model="searchQuery"
            :placeholder="searchPlaceholder"
            size="large"
            :class="$style['search-input']"
            @pressEnter="handleSearch"
          >
            <a-select
              slot="addonBefore"
              v-model="selectedDataSource"
              :class="$style['datasource-select']"
              style="width: 140px"
            >
              <a-icon slot="suffixIcon" type="database" />
              <a-select-option value="emr_hive">
                <a-icon type="hdd" style="color: #fa8c16" />
                EMR Hive
              </a-select-option>
            </a-select>
            
            <a-space slot="suffix" :size="8">
              <a-tooltip title="AI搜索">
                <a-icon 
                  type="robot" 
                  :class="$style['search-icon']"
                  @click="toggleAISearch"
                />
              </a-tooltip>
              <a-tooltip title="大小写">
                <span :class="$style['case-toggle']" @click="toggleCase">
                  {{ caseSensitive ? 'AB' : 'ab' }}
                </span>
              </a-tooltip>
            </a-space>
          </a-input>
          
          <a-button 
            type="primary" 
            size="large" 
            :class="$style['search-button']"
            @click="handleSearch"
          >
            搜索
          </a-button>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div :class="$style['content-wrapper']">
      <a-row :gutter="24">
        <!-- Left Section: Data Usage -->
        <a-col :xs="24" :lg="16">
          <a-card 
            :bordered="false" 
            :class="$style['content-card']"
          >
            <div slot="title" :class="$style['card-header']">
              <span :class="$style['card-title']">数据使用</span>
              <a-space :size="16">
                <a href="#" :class="$style['action-link']">
                  <a-icon type="setting" />
                  设置
                </a>
                <a href="#" :class="$style['action-link']" @click.prevent="refreshRecentViews">
                  <a-icon type="reload" />
                  刷新
                </a>
              </a-space>
            </div>
            
            <div :class="$style['filters-section']">
              <a-space :size="16">
                <span>类型:</span>
                <a-select v-model="viewType" style="width: 120px">
                  <a-icon slot="suffixIcon" type="table" />
                  <a-select-option value="table">
                    <a-icon type="table" />
                    表
                  </a-select-option>
                  <a-select-option value="datasource">
                    <a-icon type="database" />
                    数据源
                  </a-select-option>
                </a-select>
                
                <span style="margin-left: 16px">数据源:</span>
                <a-select v-model="filterDataSource" style="width: 140px">
                  <a-icon slot="suffixIcon" type="database" />
                  <a-select-option value="emr_hive">
                    <a-icon type="hdd" style="color: #fa8c16" />
                    EMR Hive
                  </a-select-option>
                </a-select>
              </a-space>
              
              <span :class="$style['section-subtitle']">最近浏览</span>
            </div>

            <div :class="$style['recent-views']">
              <a-empty 
                v-if="recentViews.length === 0"
                :class="$style['empty-state']"
                :image="emptyImage"
              >
                <span slot="description" :class="$style['empty-description']">
                  暂无最近浏览数据
                  <br />
                  请前往搜索并查看感兴趣的数据
                </span>
              </a-empty>
              
              <a-list 
                v-else
                :data-source="recentViews"
                :class="$style['recent-list']"
              >
                <a-list-item 
                  slot="renderItem" 
                  slot-scope="item"
                  :class="$style['list-item']"
                >
                  <a-list-item-meta>
                    <a-avatar 
                      slot="avatar" 
                      :style="{ backgroundColor: '#1890ff' }"
                    >
                      <a-icon type="table" />
                    </a-avatar>
                    <a 
                      slot="title" 
                      href="#"
                      :class="$style['item-title']"
                      @click.prevent="viewItem(item)"
                    >
                      {{ item.viewContent }}
                    </a>
                    <template slot="description">
                      <span :class="$style['item-meta']">
                        {{ formatViewTime(item.viewedAt) }}
                      </span>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </a-list>
            </div>
          </a-card>
        </a-col>

        <!-- Right Section: Data Management Stats -->
        <a-col :xs="24" :lg="8">
          <a-card 
            :bordered="false"
            :class="$style['content-card']"
          >
            <div slot="title" :class="$style['card-header']">
              <span :class="$style['card-title']">数据管理</span>
              <a-space :size="16">
                <a href="#" :class="$style['action-link']">
                  <a-icon type="setting" />
                  设置
                </a>
                <a href="#" :class="$style['action-link']" @click.prevent="refreshStats">
                  <a-icon type="reload" />
                  刷新
                </a>
              </a-space>
            </div>

            <a-tabs 
              v-model="activeTab" 
              :class="$style['management-tabs']"
              :tabBarStyle="{ marginBottom: '16px' }"
            >
              <a-tab-pane key="overview" tab="我的数据">
                <!-- Category Navigation -->
                <div :class="$style['stats-section']">
                  <h3 :class="$style['section-title']">类目导航</h3>
                  <a-row :gutter="[8, 16]">
                    <a-col :span="24">
                      <a-statistic 
                        title="总采集表数量" 
                        :value="stats.tableCount"
                        :class="$style['stat-item']"
                      />
                    </a-col>
                    <a-col :span="24">
                      <a-statistic 
                        title="关目管理表数量" 
                        :value="stats.managedCount"
                        :class="$style['stat-item']"
                      />
                    </a-col>
                    <a-col :span="24">
                      <a-statistic 
                        title="纳管比例" 
                        :value="stats.coverage"
                        :class="$style['stat-item']"
                      />
                    </a-col>
                  </a-row>
                </div>

                <a-divider />

                <!-- Metadata Collection -->
                <div :class="$style['stats-section']">
                  <a-space :class="$style['section-header']">
                    <h3 :class="$style['section-title']">元数据采集</h3>
                    <a-space :size="8">
                      <a href="#" :class="$style['action-link-sm']">
                        <a-icon type="setting" />
                        设置
                      </a>
                      <a href="#" :class="$style['action-link-sm']" @click.prevent="refreshStats">
                        <a-icon type="reload" />
                        刷新
                      </a>
                      <a href="#" :class="$style['action-link-sm']">
                        数据总览
                      </a>
                    </a-space>
                  </a-space>

                  <div :class="$style['datasource-stats']">
                    <div :class="$style['datasource-header']">
                      <a-icon type="hdd" style="color: #fa8c16" />
                      <span :class="$style['datasource-name']">EMR Hive</span>
                    </div>
                    
                    <a-row :gutter="[8, 16]">
                      <a-col :span="12">
                        <a-statistic 
                          title="实例数量" 
                          :value="stats.instanceCount"
                          :class="$style['stat-item-sm']"
                        />
                      </a-col>
                      <a-col :span="12">
                        <a-statistic 
                          title="数据库数量" 
                          :value="stats.databaseCount"
                          :class="$style['stat-item-sm']"
                        />
                      </a-col>
                      <a-col :span="12">
                        <a-statistic 
                          title="表数量" 
                          :value="stats.tableCount"
                          :class="$style['stat-item-sm']"
                        />
                      </a-col>
                      <a-col :span="12">
                        <a-statistic 
                          title="API数量" 
                          :value="stats.apiCount"
                          :class="$style['stat-item-sm']"
                        />
                      </a-col>
                      <a-col :span="24">
                        <a-statistic 
                          title="采集器数量" 
                          :value="stats.collectorCount"
                          :class="$style['stat-item-sm']"
                        />
                      </a-col>
                    </a-row>
                  </div>
                </div>
              </a-tab-pane>
              
              <a-tab-pane key="permission" tab="权限管理">
                <a-empty description="暂无权限管理数据" />
              </a-tab-pane>
            </a-tabs>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'DataMap',
  data() {
    return {
      // Search
      searchQuery: '',
      searchPlaceholder: '请输入关键字或描述，按"Enter"触发搜索，使用"Tab"切换AI搜索',
      selectedDataSource: 'emr_hive',
      aiSearchEnabled: false,
      caseSensitive: false,
      
      // Filters
      viewType: 'table',
      filterDataSource: 'emr_hive',
      
      // Data
      recentViews: [],
      stats: {
        instanceCount: 0,
        databaseCount: 0,
        tableCount: 0,
        apiCount: 0,
        collectorCount: 0,
        managedCount: 0,
        coverage: '0%'
      },
      
      // UI State
      activeTab: 'overview',
      currentUser: '',
      emptyImage: 'https://gw.alipayobjects.com/mdn/miniapp_social/afts/img/A*pevERLJC9v0AAAAAAAAAAABjAQAAAQ/original'
    };
  },
  
  mounted() {
    this.getCurrentUser();
    this.fetchStats();
    this.fetchRecentViews();
  },
  
  methods: {
    getCurrentUser() {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        this.currentUser = user.user?.username || user.username;
      }
    },
    
    async fetchStats() {
      try {
        const response = await axios.get('/api/datamap/stats');
        this.stats = response.data;
      } catch (error) {
        console.error('Failed to fetch stats:', error);
      }
    },
    
    async fetchRecentViews() {
      if (!this.currentUser) return;
      try {
        const response = await axios.get('/api/datamap/recent', {
          params: { username: this.currentUser }
        });
        this.recentViews = response.data;
      } catch (error) {
        console.error('Failed to fetch recent views:', error);
      }
    },
    
    refreshStats() {
      this.fetchStats();
    },
    
    refreshRecentViews() {
      this.fetchRecentViews();
    },
    
    handleSearch() {
      if (this.searchQuery.trim()) {
        this.$router.push({ 
          path: '/metadata', 
          query: { 
            q: this.searchQuery,
            datasource: this.selectedDataSource,
            ai: this.aiSearchEnabled
          } 
        });
      }
    },
    
    toggleAISearch() {
      this.aiSearchEnabled = !this.aiSearchEnabled;
      if (this.aiSearchEnabled) {
        this.$message.info('AI搜索已启用');
      }
    },
    
    toggleCase() {
      this.caseSensitive = !this.caseSensitive;
    },
    
    viewItem(item) {
      // Navigate to detail view
      this.$router.push({
        path: '/metadata',
        query: { resource: item.viewContent }
      });
    },
    
    formatViewTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      const now = new Date();
      const diff = Math.floor((now - date) / 1000); // seconds
      
      if (diff < 60) return '刚刚';
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`;
      if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`;
      if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`;
      
      return date.toLocaleDateString('zh-CN');
    }
  }
};
</script>

<style module>
.data-map {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.search-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 24px 64px;
  position: relative;
}

.search-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(to bottom, transparent, #f0f2f5);
}

.search-container {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.title-row {
  margin-bottom: 24px;
  justify-content: center;
}

.title-icon {
  font-size: 32px;
  color: #fff;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.search-input {
  flex: 1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.datasource-select {
  background-color: transparent;
  border-right: 1px solid #d9d9d9;
}

.search-icon {
  font-size: 18px;
  color: #8c8c8c;
  cursor: pointer;
  transition: color 0.3s;
}

.search-icon:hover {
  color: #1890ff;
}

.case-toggle {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  user-select: none;
}

.case-toggle:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.search-button {
  min-width: 100px;
  height: 40px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.content-wrapper {
  max-width: 1400px;
  margin: -40px auto 0;
  padding: 0 24px 24px;
  position: relative;
  z-index: 2;
}

.content-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.action-link {
  color: #1890ff;
  font-size: 14px;
  transition: color 0.3s;
}

.action-link:hover {
  color: #40a9ff;
}

.action-link-sm {
  color: #1890ff;
  font-size: 12px;
  transition: color 0.3s;
}

.action-link-sm:hover {
  color: #40a9ff;
}

.filters-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-subtitle {
  display: block;
  margin-top: 16px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
}

.recent-views {
  min-height: 300px;
}

.empty-state {
  padding: 60px 20px;
}

.empty-description {
  color: #8c8c8c;
  line-height: 1.6;
}

.recent-list {
}

.list-item {
  padding: 12px 0;
  transition: background-color 0.3s;
}

.list-item:hover {
  background-color: #fafafa;
}

.item-title {
  font-weight: 500;
  color: #262626;
}

.item-title:hover {
  color: #1890ff;
}

.item-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.management-tabs {
}

.stats-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 16px;
}

.stat-item {
}

.stat-item-sm {
}

.datasource-stats {
  margin-top: 16px;
}

.datasource-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background-color: #fff7e6;
  border-radius: 4px;
  border-left: 3px solid #fa8c16;
}

.datasource-name {
  font-weight: 500;
  color: #262626;
}
</style>
