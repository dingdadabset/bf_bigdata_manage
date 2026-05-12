<template>
  <div class="access-management">
    <div class="access-container">
      <aside class="access-item left-panel">
        <user-list
          ref="userList"
          @select="onSelectUser"
          @create="createUserVisible = true"
        />
      </aside>

      <main class="access-item right-panel">
        <permission-panel
          ref="permissionPanel"
          :user="selectedUser"
          @delete="handleDeleteUser"
          @grant="onGrant"
          @toggle-protection="handleToggleProtection"
        />
      </main>
    </div>

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
    GrantModal
  },
  data() {
    return {
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
        content: `删除用户 ${username} (${cluster}) 会收回当前集群权限；只有 LDAP 用户会同步删除目录账号。`,
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
  min-height: 0;
  padding: 16px;
  background: #f5f7fb;
}

.access-container {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.left-panel {
  flex: 0 0 336px;
  min-width: 304px;
  max-width: 360px;
  position: sticky;
  top: 16px;
  height: calc(100vh - 112px);
}

.right-panel {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
}

@media (max-width: 960px) {
  .access-management {
    padding: 12px;
  }
  .access-container {
    flex-direction: column;
  }
  .left-panel,
  .right-panel {
    flex: 1 1 100%;
    min-width: 100%;
    max-width: 100%;
    height: auto;
    position: static;
  }
}
</style>
