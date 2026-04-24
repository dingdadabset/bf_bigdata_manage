<template>
  <div v-if="user" class="control-panel">
    <!-- User Header -->
    <a-card :bordered="false" class="header-card">
      <div class="user-header">
        <div class="user-info">
          <a-avatar :size="64" icon="user" :style="{ backgroundColor: getAvatarColor(user.username) }" />
          <div class="info-content">
            <div class="info-title">
              {{ user.username }}
              <a-tag color="green" v-if="user.status !== 'DISABLED'">Active</a-tag>
              <a-tag color="red" v-else>Disabled</a-tag>
              <a-tag v-if="isProtectedBigDataUser(user)" color="orange">大数据重要角色</a-tag>
            </div>
            <div class="info-desc">
              {{ user.firstName }} {{ user.lastName }} | {{ user.email || 'No Email' }}
            </div>
            <div class="info-meta">
              <span class="meta-item"><a-icon type="clock-circle" /> 创建于 {{ formatDate(user.createTime) }}</span>
              <span class="meta-item">
                <a-icon type="safety" /> 认证方式:
                <span :class="['source-badge', getStrategyClass(user.creationStrategy)]">
                  {{ getStrategyLabel(user.creationStrategy) }}
                </span>
              </span>
              <span class="meta-item"><a-icon type="cluster" /> 所属集群: <a-tag color="blue">{{ effectiveCluster || '未选择' }}</a-tag></span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <a-tooltip :title="isProtectedBigDataUser(user) ? '大数据重要角色禁止删除' : '删除用户'">
            <a-button
              type="danger"
              ghost
              icon="delete"
              :disabled="isProtectedBigDataUser(user)"
              @click="$emit('delete', user.username)"
            >
              删除用户
            </a-button>
          </a-tooltip>
        </div>
      </div>
    </a-card>

    <!-- Permission Cards (No Tabs) -->
    <div style="margin-top: 16px">
      <a-alert
        v-if="capability"
        class="capability-banner"
        :type="capability.status === 'READY' ? 'info' : 'warning'"
        show-icon
      >
        <template slot="message">
          授权适配器：{{ capability.engineType || '-' }} / {{ capability.authBackend || '-' }}
          <span class="capability-extra">
            {{ capability.requiresLdap ? '身份侧依赖 LDAP' : '授权不依赖 LDAP' }}
          </span>
        </template>
        <template slot="description" v-if="capability.warnings && capability.warnings.length">
          {{ capability.warnings.join('；') }}
        </template>
      </a-alert>
      <ranger-card 
        ref="rangerCard"
        :username="user.username" 
        :cluster="effectiveCluster"
        :title="permissionCardTitle"
        @edit="$emit('grant', user.username, effectiveCluster)" 
      />
    </div>
  </div>
  
  <!-- Empty State -->
  <div v-else class="empty-state">
    <a-empty description="请选择左侧用户查看详情" />
  </div>
</template>

<script>
import moment from 'moment';
import { store } from '../../../store';
import RangerCard from './RangerCard.vue';
import axios from 'axios';

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
  name: 'PermissionPanel',
  components: { RangerCard },
  props: {
    user: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      capability: null
    };
  },
  watch: {
    user: {
      immediate: true,
      handler() {
        this.loadCapability();
      }
    },
    effectiveCluster() {
      this.loadCapability();
    }
  },
  computed: {
    currentCluster() {
      return store.currentCluster;
    },
    effectiveCluster() {
      if (this.currentCluster) return this.currentCluster;
      if (this.user && this.user.clusterName) return this.user.clusterName;
      if (this.user && this.user.cluster) return this.user.cluster;
      return '';
    },
    permissionCardTitle() {
      if (this.capability && this.capability.engineType) {
        return `${this.capability.engineType} 数据权限 (${this.capability.authBackend || 'UNKNOWN'})`;
      }
      return '数据权限';
    }
  },
  methods: {
    async loadCapability() {
      if (!this.user) {
        this.capability = null;
        return;
      }
      try {
        const params = {};
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const res = await axios.get('/api/access/capabilities', { params });
        this.capability = res.data || null;
      } catch (e) {
        this.capability = null;
      }
    },
    formatDate(date) {
      return date ? moment(date).format('YYYY-MM-DD HH:mm') : 'N/A';
    },
    getAvatarColor(username) {
      const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#1890ff'];
      let hash = 0;
      for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      return colors[Math.abs(hash) % colors.length];
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
    refresh() {
      this.loadCapability();
      if (this.$refs.rangerCard) {
        this.$refs.rangerCard.loadUserAccess();
      }
    }
  }
};
</script>

<style scoped>
.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
}
.info-content {
  margin-left: 24px;
}
.info-title {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 8px;
}
.info-desc {
  color: #666;
  margin-bottom: 8px;
}
.info-meta {
  color: #999;
  font-size: 12px;
}
.meta-item {
  margin-right: 16px;
}
.source-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  margin-left: 4px;
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
.capability-banner {
  margin-bottom: 12px;
}
.capability-extra {
  margin-left: 12px;
  color: #667085;
}
</style>
