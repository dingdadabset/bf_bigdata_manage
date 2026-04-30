<template>
  <div class="access-management">
    <a-tabs :active-key="activeTab" class="access-tabs" @change="activeTab = $event">
      <a-tab-pane key="permissions" tab="用户权限">
        <div class="access-container">
          <!-- Left Panel: User List -->
          <div class="access-item left-panel">
            <user-list
              ref="userList"
              @select="onSelectUser"
              @create="createUserVisible = true"
            />
          </div>

          <!-- Right Panel: Control Panel -->
          <div class="access-item right-panel">
            <permission-panel
              ref="permissionPanel"
              :user="selectedUser"
              @delete="handleDeleteUser"
              @grant="onGrant"
              @toggle-protection="handleToggleProtection"
            />
          </div>
        </div>
      </a-tab-pane>
      <a-tab-pane key="governance" tab="治理风险">
        <access-governance-panel />
      </a-tab-pane>
    </a-tabs>

    <!-- Modals -->
    <create-user-modal 
      :visible="createUserVisible" 
      @cancel="createUserVisible = false"
      @ok="onUserCreated"
    />

    <grant-modal
      :visible="grantModalVisible"
      :username="currentGrantUser"
      :cluster="currentGrantCluster"
      @cancel="grantModalVisible = false"
      @ok="onGrantSuccess"
    />
  </div>
</template>

<script>
import UserList from './components/UserList.vue';
import PermissionPanel from './components/PermissionPanel.vue';
import CreateUserModal from './components/CreateUserModal.vue';
import GrantModal from './components/GrantModal.vue';
import AccessGovernancePanel from './components/AccessGovernancePanel.vue';
import axios from 'axios';
import { canDelete, deleteForbiddenMessage, isRootAdmin } from '../../utils/currentUser';

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
  name: 'AccessIndex',
  components: {
    UserList,
    PermissionPanel,
    CreateUserModal,
    GrantModal,
    AccessGovernancePanel
  },
  data() {
    return {
      activeTab: 'permissions',
      selectedUser: null,
      createUserVisible: false,
      grantModalVisible: false,
      currentGrantUser: '',
      currentGrantCluster: ''
    };
  },
  methods: {
    onSelectUser(user) {
      this.selectedUser = user;
    },
    onUserCreated() {
      this.createUserVisible = false;
      this.$refs.userList.fetchUsers();
    },
    onGrant(username, cluster) {
      this.currentGrantUser = username;
      this.currentGrantCluster = cluster;
      this.grantModalVisible = true;
    },
    onGrantSuccess() {
      this.grantModalVisible = false;
      if (this.$refs.permissionPanel) {
        this.$refs.permissionPanel.refresh();
      }
    },
    async handleToggleProtection(user) {
      if (!isRootAdmin()) {
        this.$message.warning('仅 admin 用户可设置保护用户');
        return;
      }
      if (!user || !user.username) return;
      const cluster = user.clusterName || user.cluster;
      if (!cluster) {
        this.$message.warning('设置保护用户必须指定所属集群');
        return;
      }
      const nextProtected = !this.isProtectedBigDataUser(user);
      try {
        const res = await axios.put(`/api/access/user/${encodeURIComponent(user.username)}/protection`, null, {
          params: { cluster, protected: nextProtected }
        });
        const updatedUser = res.data || { ...user, protectedUser: nextProtected };
        this.selectedUser = updatedUser;
        if (this.$refs.userList) {
          this.$refs.userList.updateUser(updatedUser);
        }
        this.$message.success(nextProtected ? '已设为保护用户' : '已取消保护');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '更新保护状态失败');
      }
    },
    handleDeleteUser(userOrUsername) {
      if (!canDelete()) {
        this.$message.warning(deleteForbiddenMessage());
        return;
      }
      const user = typeof userOrUsername === 'object'
        ? userOrUsername
        : (this.selectedUser || { username: userOrUsername });
      const username = user.username;
      const cluster = user.clusterName || user.cluster;
      if (this.isProtectedBigDataUser(user)) {
        this.$message.warning('大数据重要角色禁止删除');
        return;
      }
      if (!cluster) {
        this.$message.warning('删除用户必须指定所属集群');
        return;
      }
      const that = this;
      this.$confirm({
        title: '确认删除用户?',
        content: `删除用户 ${username} (${cluster}) 会收回 Hive 权限，同时会删除 OpenLDAP 上的用户`,
        okText: 'Yes',
        okType: 'danger',
        cancelText: 'No',
        async onOk() {
          try {
            await axios.delete(`/api/access/user/${encodeURIComponent(username)}`, { params: { cluster } });
            that.$message.success('已删除');
            that.selectedUser = null;
            that.$refs.userList.fetchUsers();
          } catch (e) {
            that.$message.error(e.response?.data?.message || '删除失败');
          }
        },
      });
    },
    isProtectedBigDataUser(user) {
      if (!user || !user.username) return false;
      if (user.protectedUser !== undefined && user.protectedUser !== null) {
        return Boolean(user.protectedUser);
      }
      const userName = String(user.username).toLowerCase();
      const role = String(user.role || user.userRole || '').toLowerCase();
      return PROTECTED_BIGDATA_USERS.includes(userName)
        || role.includes('important')
        || role.includes('protected')
        || role.includes('bigdata');
    }
  }
};
</script>

<style scoped>
.access-management {
  height: 100%;
}

.access-tabs {
  height: 100%;
}

.access-tabs ::v-deep .ant-tabs-content {
  height: calc(100% - 44px);
}

.access-tabs ::v-deep .ant-tabs-tabpane {
  height: 100%;
}

.access-container {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  height: 100%;
}

.left-panel {
  flex: 1 1 280px; /* Grow 1, Shrink 1, Basis 280px */
  min-width: 280px;
  /* Optional: Limit width on very wide screens so list doesn't get too wide */
  max-width: 400px; 
}

.right-panel {
  flex: 999 1 600px; /* Take remaining space, wrap if < 600px */
  min-width: 600px;
}

/* Adjust for smaller screens */
@media (max-width: 768px) {
  .left-panel, .right-panel {
    flex: 1 1 100%;
    min-width: 100%;
    max-width: 100%;
  }
}
</style>
