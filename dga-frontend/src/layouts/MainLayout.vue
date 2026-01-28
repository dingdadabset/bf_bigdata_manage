<template>
  <a-layout id="components-layout-demo-side" style="min-height: 100vh">
    <a-layout-sider v-model="collapsed" collapsible class="custom-sider">
      <div class="logo">DGA 平台</div>
      <a-menu theme="dark" :default-selected-keys="[$route.path]" mode="inline" @click="handleMenuClick" class="custom-menu">
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
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0; display: flex; justify-content: space-between; align-items: center; padding-right: 24px;">
        <div class="header-trigger" @click="() => (collapsed = !collapsed)">
          <a-icon :type="collapsed ? 'menu-unfold' : 'menu-fold'" />
        </div>
        <div class="header-right">
          <a-dropdown>
            <a class="ant-dropdown-link" @click="e => e.preventDefault()">
              <a-avatar style="background-color: #87d068" icon="user" />
              <span style="margin-left: 8px; color: rgba(0,0,0,0.65)">{{ username }}</span>
            </a>
            <a-menu slot="overlay">
              <a-menu-item key="logout" @click="handleLogout">
                <a-icon type="logout" /> 退出登录
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content style="margin: 0 16px; background: #f4f7f9;">
        <a-breadcrumb style="margin: 16px 0; font-size: 13px; color: #555;">
          <a-breadcrumb-item>首页</a-breadcrumb-item>
          <a-breadcrumb-item style="color: #333; font-weight: 500">{{ $route.meta.title }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div :style="{ padding: '24px', background: 'transparent', minHeight: '360px' }">
          <router-view />
        </div>
      </a-layout-content>
      <a-layout-footer style="text-align: center">
        DGA Platform ©2026 Created by Data Engineering Team
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script>
import { store, mutations } from '../store';

export default {
  data() {
    return {
      collapsed: false,
      username: 'Admin'
    };
  },
  methods: {
    handleMenuClick({ key }) {
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
  background: #1e1e1e !important;
  box-shadow: 2px 0 6px rgba(0,0,0,0.2);
}

.custom-sider .ant-layout-sider-trigger {
  background: #1e1e1e;
  color: #a6adb4;
}

.custom-menu.ant-menu-dark {
  background: #1e1e1e;
}

.custom-menu.ant-menu-dark .ant-menu-item {
  margin: 4px 0;
  width: 100%;
}

.custom-menu.ant-menu-dark .ant-menu-item-selected {
  background-color: rgba(0, 47, 167, 0.1) !important; /* Soft Klein Blue background */
  border-left: 3px solid #002FA7; /* Klein Blue border */
  color: #fff;
}

.custom-menu.ant-menu-dark .ant-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.ant-breadcrumb-separator {
  color: #999;
}
</style>

<style scoped>
.logo {
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
  margin: 16px;
  color: white;
  text-align: center;
  line-height: 32px;
  font-weight: bold;
  font-size: 18px;
  border-radius: 4px;
}
.header-trigger {
  padding: 0 24px;
  font-size: 18px;
  line-height: 64px;
  cursor: pointer;
  transition: color 0.3s;
}
.header-trigger:hover {
  color: #002FA7; /* Klein Blue */
}
</style>
