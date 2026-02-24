<template>
  <a-layout id="components-layout-demo-top-side" style="min-height: 100vh;">
    <!-- HEADER (Full Width) -->
    <a-layout-header class="main-header">
      <!-- Logo in Header -->
      <div class="header-logo" @click="$router.push('/access')">
        <a-icon type="deployment-unit" style="font-size: 30px; color: #1890ff;" />
        <span class="logo-text">DGA</span>
      </div>

      <!-- Header Content -->
      <div class="header-action-bar" v-if="$route.path.startsWith('/access')">
          <a-input-search
            placeholder="搜索用户/角色"
            style="width: 300px; margin-right: 16px;"
            class="custom-header-search"
            v-model="searchText"
            @search="onSearch"
            @change="onSearchChange"
          />
          
          <a-select
            v-model="selectedCluster"
            placeholder="选择集群"
            style="width: 150px; margin-right: 16px;"
            @change="onClusterChange"
            class="custom-header-select"
          >
            <a-select-option :value="''">全部集群</a-select-option>
            <a-select-option v-for="cluster in clusters" :key="cluster" :value="cluster">
              {{ cluster }}
            </a-select-option>
          </a-select>

          <a-button-group class="header-btn-group">
            <a-button icon="cloud-download" @click="triggerAction('import')" title="一键导入" class="rounded-btn-left">导入</a-button>
            <a-button type="primary" icon="plus" @click="triggerAction('create')" title="新建用户" class="rounded-btn-right">新建</a-button>
          </a-button-group>
      </div>
      <div v-else style="flex: 1"></div>

      <div class="header-right">
        <a-dropdown>
          <a class="ant-dropdown-link" @click="e => e.preventDefault()">
            <a-avatar style="background-color: #1890ff" icon="user" />
            <span style="margin-left: 8px; color: rgba(0,0,0,0.65)">{{ username }}</span>
          </a>
          <a-menu slot="overlay">
            <a-menu-item key="profile" @click="$router.push('/profile')">
              <a-icon type="user" /> 个人中心
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" @click="handleLogout">
              <a-icon type="logout" /> 退出登录
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </div>
    </a-layout-header>

    <!-- BODY (Sider + Content) -->
    <a-layout>
      <a-layout-sider
        v-model="collapsed"
        collapsible
        class="custom-sider"
        :width="260"
        :collapsedWidth="80"
        theme="light"
        :trigger="null"
        breakpoint="md"
      >
        <div class="menu-container">
          <a-menu
            theme="light"
            :default-selected-keys="[$route.path]"
            mode="inline"
            @click="handleMenuClick"
            class="custom-menu"
          >
            <a-menu-item key="/datamap">
              <a-icon type="compass" />
              <span>数据地图</span>
            </a-menu-item>
            <a-menu-item key="/datasource">
              <a-icon type="database" />
              <span>数据源管理</span>
            </a-menu-item>
            <a-menu-item key="/access">
              <a-icon type="user" />
              <span>权限管理</span>
            </a-menu-item>
            <a-menu-item key="/metadata">
              <a-icon type="table" />
              <span>元数据管理</span>
            </a-menu-item>
            <a-menu-item key="/quality">
              <a-icon type="dashboard" />
              <span>数据质量</span>
            </a-menu-item>
            <a-menu-item key="/resources">
              <a-icon type="link" />
              <span>资源导航</span>
            </a-menu-item>
          </a-menu>

          <div class="sider-bottom-actions">
             <a-menu
              theme="light"
              mode="inline"
              class="custom-menu bottom-menu"
              :selectable="false"
            >
              <a-menu-item key="settings">
                <a-icon type="setting" />
                <span>设置</span>
              </a-menu-item>
              <a-menu-item key="logout" @click="handleLogout">
                <a-icon type="logout" />
                <span>退出</span>
              </a-menu-item>
            </a-menu>
          </div>
        </div>
        
        <!-- Custom Trigger -->
        <div class="sider-trigger" @click="() => (collapsed = !collapsed)">
          <a-icon :type="collapsed ? 'menu-unfold' : 'menu-fold'" />
        </div>
      </a-layout-sider>

      <a-layout style="flex: 1; min-width: 0;">
        <a-layout-content style="margin: 0; background: #f4f7f9;">
          <div :style="{ padding: '24px', background: 'transparent', minHeight: '360px' }">
            <router-view />
          </div>
        </a-layout-content>
        <a-layout-footer style="text-align: center">
          DGA Platform ©2026 Created by Data Engineering Team
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script>
import { store, mutations } from '../store';
import axios from 'axios';

export default {
  data() {
    return {
      collapsed: false,
      username: 'Admin',
      searchText: '',
      selectedCluster: '',
      clusters: []
    };
  },
  watch: {
    '$route.path': {
      handler(val) {
        if (val.startsWith('/access')) {
          this.fetchClusters();
        }
      },
      immediate: true
    }
  },
  methods: {
    async fetchClusters() {
      try {
        const res = await axios.get('/api/access/clusters');
        if (res.data) {
          this.clusters = res.data;
        }
      } catch (e) {
        console.error("Failed to fetch clusters", e);
      }
    },
    onSearch(value) {
      mutations.setHeaderSearchText(value);
    },
    onSearchChange(e) {
       // Optional: live search
       mutations.setHeaderSearchText(this.searchText);
    },
    onClusterChange(value) {
      mutations.setHeaderSelectedCluster(value);
    },
    triggerAction(type) {
      mutations.triggerHeaderAction(type);
    },
    handleMenuClick({ key }) {
      if (['settings', 'logout'].includes(key)) return;
      
      if (key !== this.$route.path) {
        this.$router.push(key);
      }
    },
    handleLogout() {
      localStorage.removeItem('user');
      this.$router.push('/login');
    }
  },
  created() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.username = user.user?.username || user.username || 'Admin';
      } catch (e) {
        // ignore
      }
    }
  }
};
</script>

<style>
/* Global Layout Overrides */
.custom-sider {
  background: #fff !important;
  box-shadow: 1px 0 6px rgba(0,21,41,.06);
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.custom-sider .ant-layout-sider-children {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.main-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  z-index: 20;
  box-shadow: 0 1px 4px rgba(0,21,41,.03);
}

.header-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-right: 48px;
  margin-left: 60px; /* Moved right ~2cm */
  min-width: 180px;
  transition: all 0.2s;
  overflow: hidden;
  cursor: pointer;
}

.logo-text {
  font-size: 24px; /* Scaled 1.2x from 20px */
  font-weight: 700;
  color: #1890ff;
  margin-left: 12px;
  white-space: nowrap;
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
}

.menu-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px 8px; /* Added horizontal padding for floating menu feel */
}

.custom-menu {
  border-right: none !important;
}

.custom-menu .ant-menu-item {
  margin: 4px 0 !important;
  width: 100% !important;
  border-radius: 8px; /* Rounded menu items */
  height: 44px !important;
  line-height: 44px !important;
  color: #666;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.custom-menu .ant-menu-item:hover {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.08); /* Softer hover */
}

.custom-menu .ant-menu-item-selected {
  background-color: #e6f7ff !important; /* Lighter background for selection */
  color: #1890ff !important;
  font-weight: 500;
  box-shadow: none; /* Remove shadow for flatter look */
}

.custom-menu .ant-menu-item-selected::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  border-right: 3px solid #1890ff;
  border-radius: 2px 0 0 2px;
  opacity: 1;
  transform: scaleY(1);
  transition: transform 0.15s cubic-bezier(0.215, 0.61, 0.355, 1), opacity 0.15s cubic-bezier(0.215, 0.61, 0.355, 1);
}

.custom-menu .ant-menu-item-selected > a,
.custom-menu .ant-menu-item-selected > a:hover {
  color: #1890ff;
}

.custom-menu .ant-menu-item-selected .anticon {
  color: #1890ff;
}

.custom-menu .anticon {
  font-size: 16px;
  transition: color 0.3s;
}

/* Bottom Actions */
.sider-bottom-actions {
  margin-top: auto;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}

.sider-trigger {
  height: 48px;
  line-height: 48px;
  text-align: center;
  cursor: pointer;
  transition: color 0.3s;
  color: #666;
}
.sider-trigger:hover {
  color: #1890ff;
}

.ant-breadcrumb-separator {
  color: #999;
}
</style>

<style scoped>
.header-branding {
  display: none; 
}

.header-trigger {
  padding: 0 16px 0 24px;
  font-size: 16px;
  line-height: 64px;
  cursor: pointer;
  transition: color 0.3s;
}
.header-trigger:hover {
  color: #1890ff; 
}

/* Header Styles */
.header-action-bar {
  display: flex;
  align-items: center;
  flex: 1;
}

.custom-header-search .ant-input {
  border-radius: 20px;
  background-color: #f8f9fa;
  border: 1px solid #e8e8e8;
  transition: all 0.3s;
}

.custom-header-search .ant-input:focus,
.custom-header-search .ant-input:hover {
  background-color: #fff;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.custom-header-select .ant-select-selection {
  border-radius: 20px;
  background-color: #f8f9fa;
  border: 1px solid #e8e8e8;
}

.custom-header-select .ant-select-selection:hover,
.custom-header-select .ant-select-selection:focus-within {
  background-color: #fff;
  border-color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
}

/* Responsive Sider: Force collapsed look on small screens */
@media (max-width: 768px) {
  .custom-sider {
    flex: 0 0 80px !important;
    max-width: 80px !important;
    min-width: 80px !important;
    width: 80px !important;
  }
  
  .custom-menu .ant-menu-item span,
  .sider-bottom-actions .ant-menu-item span {
    display: none !important;
  }

  .ant-menu-item {
    padding: 0 24px !important; /* Center icons */
    text-align: center;
  }
  
  .ant-menu-item .anticon {
    margin-right: 0 !important;
  }
}

/* Rounded Button Styles */
.header-btn-group .ant-btn {
  border-radius: 0;
}

.header-btn-group .rounded-btn-left {
  border-top-left-radius: 8px !important;
  border-bottom-left-radius: 8px !important;
}

.header-btn-group .rounded-btn-right {
  border-top-right-radius: 8px !important;
  border-bottom-right-radius: 8px !important;
}

/* Ensure no double borders or weird overlaps */
.header-btn-group .ant-btn:not(:first-child) {
  margin-left: -1px;
}
</style>
