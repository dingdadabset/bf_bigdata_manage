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
        :class="{ 'active': isSelected(item) }"
        @click="selectUser(item)"
      >
        <a-list-item-meta>
          <div slot="description">
            <span :class="['source-badge', getStrategyClass(item.creationStrategy)]">
              {{ getStrategyLabel(item.creationStrategy) }}
            </span>
          </div>
          <span slot="title" class="user-list-title">
            {{ item.username }}
            <a-tag v-if="item.role" color="blue" style="margin-left: 8px; font-size: 10px; line-height: 18px; height: 20px;">{{ item.role }}</a-tag>
            <a-tag v-if="isProtectedBigDataUser(item)" color="orange" style="margin-left: 8px; font-size: 10px; line-height: 18px; height: 20px;">保护</a-tag>
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

const PROTECTED_BIGDATA_USERS = [
  'alading',
  'bf_hpt',
  'bf_hpt1',
  'md_bf',
  'hdfs',
  'hive',
  'yarn',
  'spark',
  'hbase',
  'impala',
  'sentry',
  'ranger'
];

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
      const cluster = store.headerSelectedCluster;
      if (!cluster) {
        this.$message.warning('请先选择具体集群后再导入 OpenLDAP 用户');
        return;
      }
      this.loadingUsers = true; // Use loadingUsers to indicate busy
      try {
        const res = await axios.post('/api/access/import', null, { params: { cluster } });
        const data = res.data || {};
        const repairedText = data.repaired ? `，历史修复 ${data.repaired}` : '';
        if (data.failed) {
          const firstFailure = data.failures && data.failures.length
            ? `，首个失败: ${data.failures[0].message}`
            : '';
          this.$message.warning(
            `导入完成：新增 ${data.inserted || 0}，更新 ${data.updated || 0}${repairedText}，失败 ${data.failed}${firstFailure}`
          );
        } else {
          this.$message.success(
            `导入完成：新增 ${data.inserted || 0}，更新 ${data.updated || 0}${repairedText}，失败 0`
          );
        }
        await this.fetchUsers();
      } catch (e) {
        console.error(e);
        this.$message.error(e.response?.data?.message || 'OpenLDAP 导入失败');
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
        case 'IPA_IMPORT': return 'orange';
        case 'LDAP': return 'cyan';
        case 'INIT_USER': return 'orange';
        default: return 'default';
      }
    },
    getStrategyLabel(strategy) {
      const s = (strategy || '').toUpperCase();
      const labels = {
        OPENLDAP: 'OpenLDAP 创建',
        LDAP: 'LDAP 创建',
        LDAP_IMPORT: 'LDAP 导入',
        IPA_HTTP: 'IPA 创建',
        IPA_IMPORT: 'IPA 导入',
        IPA_SSH: 'IPA SSH',
        INIT_USER: '初始化用户'
      };
      return labels[s] || (strategy || '未知来源');
    },
    getStrategyClass(strategy) {
      const s = (strategy || '').toUpperCase();
      if (s.includes('LDAP')) return 'source-ldap';
      if (s.includes('IPA')) return 'source-ipa';
      if (s.includes('SELF')) return 'source-self';
      if (s.includes('INIT')) return 'source-init';
      return 'source-default';
    },
    isProtectedBigDataUser(user) {
      if (!user || !user.username) return false;
      const username = String(user.username).toLowerCase();
      const role = String(user.role || user.userRole || '').toLowerCase();
      return PROTECTED_BIGDATA_USERS.includes(username)
        || role.includes('important')
        || role.includes('protected')
        || role.includes('bigdata');
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
        
        // Auto select first user if none selected or selected user is not in the current page/filter
        if (this.selectedUser && !this.userList.some(u => this.sameUser(u, this.selectedUser))) {
          this.selectedUser = null;
          this.$emit('select', null);
        }
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
    },
    sameUser(a, b) {
      if (!a || !b) return false;
      if (a.id && b.id) return a.id === b.id;
      return a.username === b.username && (a.clusterName || a.cluster) === (b.clusterName || b.cluster);
    },
    isSelected(item) {
      return this.sameUser(item, this.selectedUser);
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
.source-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .2px;
  line-height: 22px;
  border: 1px solid transparent;
}
.source-ldap {
  color: #067647;
  background: #ecfdf3;
  border-color: #abefc6;
}
.source-ipa {
  color: #6941c6;
  background: #f4f3ff;
  border-color: #d9d6fe;
}
.source-self {
  color: #175cd3;
  background: #eff8ff;
  border-color: #b2ddff;
}
.source-init {
  color: #b54708;
  background: #fffaeb;
  border-color: #fedf89;
}
.source-default {
  color: #344054;
  background: #f2f4f7;
  border-color: #d0d5dd;
}
</style>
