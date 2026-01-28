<template>
  <a-card :bordered="false" class="user-list-card" :body-style="{ padding: '12px' }">
    <div style="margin-bottom: 12px;">
      <a-select
        v-model="selectedCluster"
        placeholder="选择集群(默认全部)"
        style="width: 100%;"
        @change="onClusterChange"
      >
        <a-select-option :value="''">全部</a-select-option>
        <a-select-option v-for="cluster in clusters" :key="cluster" :value="cluster">
          {{ cluster }}
        </a-select-option>
      </a-select>
    </div>
    <div class="user-search-wrapper">
      <a-input-search
        placeholder="搜索用户/角色"
        v-model="searchText"
        @search="onSearch"
        style="margin-bottom: 16px"
      />
    </div>
    <a-list
      item-layout="horizontal"
      :data-source="filteredUsers"
      :loading="loadingUsers"
      class="user-list"
    >
      <a-list-item 
        slot="renderItem" 
        slot-scope="item" 
        class="user-list-item"
        :class="{ 'active': selectedUser && selectedUser.username === item.username }"
        @click="selectUser(item)"
      >
        <a-list-item-meta>
          <div slot="description">
            <a-tag :color="getStrategyColor(item.creationStrategy)" style="font-size: 10px; line-height: 18px; height: 20px;">
              {{ item.creationStrategy }}
            </a-tag>
          </div>
          <span slot="title" class="user-list-title">
            {{ item.username }}
            <a-tag v-if="item.role" color="blue" style="margin-left: 8px; font-size: 10px; line-height: 18px; height: 20px;">{{ item.role }}</a-tag>
          </span>
          <a-avatar slot="avatar" icon="user" :style="{ backgroundColor: getAvatarColor(item.username) }" />
        </a-list-item-meta>
      </a-list-item>
      <div slot="footer" v-if="pagination.total > pagination.pageSize" style="text-align: center">
        <a-button type="link" size="small" @click="loadMoreUsers">加载更多</a-button>
      </div>
    </a-list>
    <div style="margin-top: 12px; text-align: center;">
       <a-button type="dashed" block icon="plus" @click="$emit('create')">新建用户</a-button>
    </div>
  </a-card>
</template>

<script>
import axios from 'axios';
import { store, mutations } from '../../../store';

export default {
  name: 'UserList',
  data() {
    return {
      // userList: [], // Use computed instead
      loadingUsers: false,
      searchText: '',
      selectedCluster: '',
      clusters: [],
      pagination: { current: 1, pageSize: 20, total: 0 },
      selectedUser: null,
    };
  },
  computed: {
    userList() {
      // For now, let's just use the store users as the source of truth for this demo
      return store.users;
    },
    filteredUsers() {
      let users = this.userList;
      // Filter out system/login users
      users = users.filter(u => !['SELF_REGISTER', 'SELF_REG'].includes(u.creationStrategy));
      
      if (!this.searchText) return users;
      return users.filter(u => u.username.toLowerCase().includes(this.searchText.toLowerCase()));
    }
  },
  async mounted() {
    await this.fetchClusters();
    this.fetchUsers();
  },
  methods: {
    async fetchClusters() {
      try {
        const res = await axios.get('/api/access/clusters');
        if (res.data && Array.isArray(res.data)) {
          const unique = new Set(res.data.filter(c => c));
          this.clusters = Array.from(unique).sort();
        }
      } catch (e) {
        console.error('Failed to fetch clusters', e);
        this.clusters = [];
      }
    },
    getAvatarColor(username) {
      const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#1890ff'];
      let hash = 0;
      for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      return colors[Math.abs(hash) % colors.length];
    },
    getStrategyColor(strategy) {
      switch (strategy) {
        case 'SELF_REG': return 'green';
        case 'IPA_HTTP': return 'purple';
        case 'LDAP': return 'cyan';
        default: return 'default';
      }
    },
    onClusterChange() {
      mutations.setCluster(this.selectedCluster || '');
      this.fetchUsers();
    },
    async fetchUsers(params = {}) {
      this.loadingUsers = true;
      try {
        // Merge params with cluster filter
        const requestParams = { ...params };
        if (this.selectedCluster) {
          requestParams.cluster = this.selectedCluster;
        }
        
        const res = await axios.get('/api/access/users', { params: requestParams });
        if (res.data && res.data.content) {
             mutations.setUsers(res.data.content);
             this.pagination.total = res.data.totalElements;
        } else {
             // Fallback or empty
        }
        
        // Auto select first user if none selected
        if (!this.selectedUser && this.userList.length > 0) {
          this.selectUser(this.userList[0]);
        }
      } catch (e) {
          console.error("Failed to fetch users", e);
          // Fallback to store/mock if needed, but for now let's rely on API
      } finally {
        this.loadingUsers = false;
      }
    },
    selectUser(user) {
      this.selectedUser = user;
      this.$emit('select', user);
    },
    onSearch() {
      // Client side filter
    },
    loadMoreUsers() {
      // Pagination logic
    }
  }
};
</script>

<style scoped>
.user-list-card {
  height: 100%;
  overflow-y: auto;
}
.user-list-item {
  cursor: pointer;
  padding: 12px;
  border-radius: 4px;
  transition: all 0.3s;
}
.user-list-item:hover {
  background-color: #f5f5f5;
}
.user-list-item.active {
  background-color: #e6f7ff;
}
</style>
