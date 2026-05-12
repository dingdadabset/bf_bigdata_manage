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
            <span :class="['source-badge', getRenderedStrategyClass(item)]">
              {{ getRenderedStrategyLabel(item) }}
            </span>
          </div>
          <span slot="title" class="user-list-title">
            {{ item.username }}
            <a-tag v-if="item.role" color="blue" style="margin-left: 8px; font-size: 10px; line-height: 18px; height: 20px;">{{ item.role }}</a-tag>
            <a-tag v-if="isProtectedBigDataUser(item)" color="orange" class="compact-tag">保护</a-tag>
            <a-tag v-if="userTypeLabel(item.userType)" :color="userTypeColor(item.userType)" class="compact-tag">
              {{ userTypeLabel(item.userType) }}
            </a-tag>
            <a-tag v-if="item.expiresAt" color="geekblue" class="compact-tag">
              至 {{ formatDate(item.expiresAt) }}
            </a-tag>
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
import moment from 'moment';
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
      capability: null,
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
      this.fetchCapability();
      this.fetchUsers();
    },
    'store.headerAction'(val) {
      if (val && val.type === 'import') {
        this.handleImport(val.cluster);
      } else if (val && val.type === 'create') {
        this.$emit('create');
      }
    }
  },
  async mounted() {
    this.fetchCapability();
    this.fetchUsers();
  },
  methods: {
    async handleImport(selectedCluster) {
      const cluster = store.headerSelectedCluster;
      if (!cluster) {
        this.$message.warning('请先选择具体集群后再导入用户');
        return;
      }
      const clusterType = String(selectedCluster && selectedCluster.type ? selectedCluster.type : '').toUpperCase();
      const capability = this.capability || await this.fetchCapability();
      const sqlEngine = this.sqlEngineFromCapability(capability) || this.sqlEngineFromClusterType(clusterType);
      const isSqlAuthBackend = Boolean(sqlEngine);
      this.loadingUsers = true;
      try {
        const url = isSqlAuthBackend ? '/api/access/import-auth-backend' : '/api/access/import';
        const res = await axios.post(url, null, { params: { cluster } });
        const data = res.data || {};
        const repairedText = data.repaired ? `，历史修复 ${data.repaired}` : '';
        if ((data.total || 0) === 0) {
          this.$message.warning(data.message || (isSqlAuthBackend ? '授权后端查询成功，但没有找到用户' : 'LDAP 查询成功，但没有找到用户，请检查 User Base DN 是否为用户所在目录'));
        } else if (data.failed) {
          const firstFailure = data.failures && data.failures.length
            ? `，首个失败: ${data.failures[0].message}`
            : '';
          this.$message.warning(
            `${data.message || `导入完成：新增 ${data.inserted || 0}，更新 ${data.updated || 0}${repairedText}，失败 ${data.failed}`}${firstFailure}`
          );
        } else {
          this.$message.success(
            data.message || `导入完成：新增 ${data.inserted || 0}，更新 ${data.updated || 0}${repairedText}，失败 0`
          );
        }
        await this.fetchUsers();
      } catch (e) {
        console.error(e);
        this.$message.error(e.response?.data?.message || (isSqlAuthBackend ? '授权后端用户导入失败' : 'OpenLDAP 导入失败'));
      } finally {
        this.loadingUsers = false;
      }
    },
    async fetchCapability() {
      const cluster = store.headerSelectedCluster;
      if (!cluster) {
        this.capability = null;
        return null;
      }
      try {
        const res = await axios.get('/api/access/capabilities', { params: { cluster } });
        this.capability = res.data || null;
        return this.capability;
      } catch (e) {
        this.capability = null;
        return null;
      }
    },
    sqlEngineFromClusterType(type) {
      const value = String(type || '').toUpperCase();
      if (value.includes('DORIS')) return 'DORIS';
      if (value === 'SR' || value.includes('STARROCKS') || value.includes('STAR_ROCKS') || value.includes('STAR')) return 'STARROCKS';
      return '';
    },
    sqlEngineFromCapability(capability) {
      const backend = String(capability && capability.authBackend ? capability.authBackend : '').toUpperCase();
      const engine = String(capability && capability.engineType ? capability.engineType : '').toUpperCase();
      const combined = `${backend} ${engine}`;
      if (combined.includes('DORIS')) return 'DORIS';
      if (combined.includes('STARROCKS') || combined.includes('STAR_ROCKS') || combined.includes('STAR')) return 'STARROCKS';
      return '';
    },
    currentSqlEngine() {
      return this.sqlEngineFromCapability(this.capability);
    },
    getRenderedStrategyLabel(item) {
      const engine = this.currentSqlEngine();
      if (engine === 'DORIS') return 'Doris 用户';
      if (engine === 'STARROCKS') return 'StarRocks 用户';
      return this.getStrategyLabel(item && item.creationStrategy);
    },
    getRenderedStrategyClass(item) {
      if (this.currentSqlEngine()) return 'source-sql';
      return this.getStrategyClass(item && item.creationStrategy);
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
        INIT_USER: '初始化用户',
        STARROCKS: 'StarRocks 用户',
        STARROCKS_IMPORT: 'StarRocks 导入',
        DORIS: 'Doris 用户',
        DORIS_IMPORT: 'Doris 导入',
        LIVE_AUTH_BACKEND: '授权后端用户'
      };
      return labels[s] || (strategy || '未知来源');
    },
    getStrategyClass(strategy) {
      const s = (strategy || '').toUpperCase();
      if (s.includes('LDAP')) return 'source-ldap';
      if (s.includes('IPA')) return 'source-ipa';
      if (s.includes('STAR') || s.includes('DORIS') || s.includes('LIVE_AUTH')) return 'source-sql';
      if (s.includes('SELF')) return 'source-self';
      if (s.includes('INIT')) return 'source-init';
      return 'source-default';
    },
    userTypeLabel(userType) {
      const value = (userType || 'INTERNAL').toUpperCase();
      const labels = {
        OUTSOURCER: '外包',
        TEMPORARY: '临时',
        SERVICE: '服务'
      };
      return labels[value] || '';
    },
    userTypeColor(userType) {
      const value = (userType || '').toUpperCase();
      const colors = {
        OUTSOURCER: 'volcano',
        TEMPORARY: 'orange',
        SERVICE: 'purple'
      };
      return colors[value] || 'default';
    },
    formatDate(value) {
      return value ? moment(value).format('YYYY-MM-DD') : '';
    },
    isProtectedBigDataUser(user) {
      if (!user || !user.username) return false;
      if (user.protectedUser !== undefined && user.protectedUser !== null) {
        return Boolean(user.protectedUser);
      }
      const username = String(user.username).toLowerCase();
      const role = String(user.role || user.userRole || '').toLowerCase();
      return PROTECTED_BIGDATA_USERS.includes(username)
        || role.includes('important')
        || role.includes('protected')
        || role.includes('bigdata');
    },
    updateUser(updatedUser) {
      if (!updatedUser) return;
      const users = this.userList.map(item => this.sameUser(item, updatedUser) ? updatedUser : item);
      mutations.setUsers(users);
      if (this.selectedUser && this.sameUser(this.selectedUser, updatedUser)) {
        this.selectedUser = updatedUser;
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
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(16, 24, 40, 0.04);
}
.user-list-item {
  cursor: pointer;
  padding: 14px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.3s;
}
.user-list-item:hover {
  background-color: #f8fafc;
}
.user-list-item.active {
  background-color: #eef6ff;
  border-color: #91caff;
}
.user-list-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  color: #101828;
  font-weight: 600;
}
.compact-tag {
  margin-left: 8px;
  font-size: 10px;
  line-height: 18px;
  height: 20px;
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
.source-sql {
  color: #155eef;
  background: #eff4ff;
  border-color: #b2ccff;
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
