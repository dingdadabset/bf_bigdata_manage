<template>
  <a-card :bordered="false" class="user-list-card" :body-style="{ padding: '12px' }">
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
      <div slot="footer" style="text-align: center; padding-top: 8px;">
        <a-pagination
          :current="pagination.current"
          :pageSize="pagination.pageSize"
          :total="pagination.total"
          size="small"
          show-less-items
          @change="onPageChange"
        />
      </div>
    </a-list>
  </a-card>
</template>

<script>
import axios from 'axios';
import { store, mutations } from '../../../store';

export default {
  name: 'UserList',
  data() {
    return {
      store,
      // userList: [], // Use computed instead
      loadingUsers: false,
      pagination: { current: 1, pageSize: 8, total: 0 },
      selectedUser: null,
    };
  },
  computed: {
    userList() {
      // For now, let's just use the store users as the source of truth for this demo
      return store.users;
    },
    filteredUsers() {
      // Server-side filtering; return current page content
      return this.userList.filter(u => !['SELF_REGISTER', 'SELF_REG'].includes(u.creationStrategy));
    }
  },
  watch: {
    'store.headerSearchText'(val) {
      this.pagination.current = 1;
      this.fetchUsers();
    },
    'store.headerSelectedCluster'(val) {
      this.pagination.current = 1;
      this.fetchUsers();
    },
    'store.headerAction'(val) {
      if (val) {
        if (val.type === 'import') {
          this.handleImport();
        } else if (val.type === 'create') {
          this.$emit('create');
        }
      }
    }
  },
  async mounted() {
    // Initial fetch
    this.fetchUsers();
  },
  methods: {
    async handleImport() {
      this.loadingUsers = true; // Use loadingUsers to indicate busy
      try {
        const res = await axios.post('/api/access/import');
        this.$message.success(res.data || 'Import successful');
        this.fetchUsers();
      } catch (e) {
        console.error(e);
        this.$message.error('Import failed');
      } finally {
        this.loadingUsers = false;
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
      const s = (strategy || '').toUpperCase();
      switch (s) {
        case 'SELF_REG': 
        case 'SELF_REGISTER': return 'green';
        case 'IPA_HTTP': return 'purple';
        case 'LDAP': return 'cyan';
        case 'INIT_USER': return 'orange';
        default: return 'default';
      }
    },
    async fetchUsers(params = {}) {
      this.loadingUsers = true;
      try {
        // Merge params with cluster filter
        const requestParams = { ...params };
        
        const cluster = store.headerSelectedCluster;
        if (cluster) {
          requestParams.cluster = cluster;
        }
        
        requestParams.page = this.pagination.current - 1;
        requestParams.size = this.pagination.pageSize;
        
        const searchText = store.headerSearchText;
        if (searchText && searchText.trim()) {
          requestParams.q = searchText.trim();
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
    onPageChange(page) {
      this.pagination.current = page;
      this.fetchUsers();
    },
    selectUser(user) {
      this.selectedUser = user;
      this.$emit('select', user);
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
