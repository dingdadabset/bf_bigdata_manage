<template>
  <div class="access-management">
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
        />
      </div>
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
      currentGrantUser: ''
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
    onGrant(username) {
      this.currentGrantUser = username;
      this.grantModalVisible = true;
    },
    onGrantSuccess() {
      this.grantModalVisible = false;
      if (this.$refs.permissionPanel) {
        this.$refs.permissionPanel.refresh();
      }
    },
    handleDeleteUser(username) {
      const that = this;
      this.$confirm({
        title: '确认删除用户?',
        content: `删除用户 ${username} 会收回Hive权限，同时会删除Linux上面的用户`,
        okText: 'Yes',
        okType: 'danger',
        cancelText: 'No',
        async onOk() {
          try {
            await axios.delete(`/api/access/user/${username}`);
            that.$message.success('已删除');
            that.selectedUser = null;
            that.$refs.userList.fetchUsers();
          } catch (e) {
            that.$message.error('删除失败 (Mock)');
            that.selectedUser = null;
             // that.$refs.userList.fetchUsers();
          }
        },
      });
    }
  }
};
</script>

<style scoped>
.access-management {
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
