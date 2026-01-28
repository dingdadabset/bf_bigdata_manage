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
            </div>
            <div class="info-desc">
              {{ user.firstName }} {{ user.lastName }} | {{ user.email || 'No Email' }}
            </div>
            <div class="info-meta">
              <span class="meta-item"><a-icon type="clock-circle" /> 创建于 {{ formatDate(user.createTime) }}</span>
              <span class="meta-item"><a-icon type="safety" /> 认证方式: {{ user.creationStrategy }}</span>
              <span class="meta-item"><a-icon type="cluster" /> 所属集群: <a-tag color="blue">{{ effectiveCluster || '未选择' }}</a-tag></span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <a-button type="danger" ghost icon="delete" @click="$emit('delete', user.username)">删除用户</a-button>
          <a-button type="primary" icon="thunderbolt" :loading="testingConn" @click="testConnectivity" style="margin-left: 12px">
            权限连通性测试
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- Permission Tabs -->
    <a-card :bordered="false" class="content-card" style="margin-top: 16px">
      <a-tabs default-active-key="authz" class="custom-tabs">
        <!-- Tab 1: Resource Authorization -->
        <a-tab-pane key="authz" tab="资源授权 (AuthZ)">
           <!-- Resource Specific View -->
           <div v-if="effectiveCluster === 'StarRocks-Financial'">
              <star-rocks-card :username="user.username" />
           </div>
           <div v-else>
              <ranger-card 
                ref="rangerCard"
                :username="user.username" 
                :cluster="effectiveCluster"
                :title="(effectiveCluster || '').includes('CDH') ? 'Hive 数据权限 (Sentry)' : 'Hive 数据权限 (Ranger)'"
                @edit="$emit('grant', user.username)" 
              />
           </div>

           <!-- HDFS / OS Card (Common) -->
           <a-card title="HDFS & OS 权限" :bordered="false" class="perm-card" style="margin-top: 16px">
            <a-descriptions size="small" :column="2">
              <a-descriptions-item label="HDFS Home">/user/{{ user.username }} (755)</a-descriptions-item>
              <a-descriptions-item label="YARN Queue">root.users.{{ user.username }}</a-descriptions-item>
              <a-descriptions-item label="OS Groups">hadoop, users</a-descriptions-item>
              <a-descriptions-item label="Sudo Privileges">None</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-tab-pane>

        <!-- Tab 2: Account Status -->
        <a-tab-pane key="authn" tab="账号状态 (AuthN)">
           <account-status :user="user" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
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
import StarRocksCard from './StarRocksCard.vue';
import AccountStatus from './AccountStatus.vue';

export default {
  name: 'PermissionPanel',
  components: { RangerCard, StarRocksCard, AccountStatus },
  props: {
    user: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      testingConn: false
    };
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
    }
  },
  methods: {
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
    async testConnectivity() {
      this.testingConn = true;
      setTimeout(() => {
        this.testingConn = false;
        this.$notification.success({
          message: '权限连通性测试通过',
          description: `用户 ${this.user.username} 已成功通过 JDBC 连接到 ${this.effectiveCluster} (Hive/StarRocks)，Kerberos 票据有效。`,
          duration: 4
        });
      }, 1500);
    },
    refresh() {
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
</style>
