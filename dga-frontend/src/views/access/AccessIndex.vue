<template>
  <div class="access-management">
    <a-row :gutter="24">
      <!-- Left Panel: User List -->
      <a-col :span="5">
        <user-list 
          ref="userList"
          @select="onSelectUser"
          @create="createUserVisible = true"
        />
      </a-col>

      <!-- Right Panel: Control Panel -->
      <a-col :span="19">
        <permission-panel 
          ref="permissionPanel"
          :user="selectedUser" 
          @delete="handleDeleteUser"
          @grant="onGrant"
        />
      </a-col>
    </a-row>

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
        content: `这将删除用户 ${username} 及其关联权限`,
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
  /* height: 100%; */
}
</style>
