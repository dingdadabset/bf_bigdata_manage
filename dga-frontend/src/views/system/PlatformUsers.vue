<template>
  <div class="platform-users">
    <div class="page-header">
      <div>
        <h1>平台用户</h1>
        <p>管理 DGA 平台登录账号与超级管理员权限</p>
      </div>
      <a-button icon="reload" :loading="loading" @click="fetchUsers">刷新</a-button>
    </div>

    <a-alert
      class="page-alert"
      type="info"
      show-icon
      message="超级管理员可执行删除操作"
      description="这里管理的是 DGA 平台登录账号 users 表，不是 LDAP 导入的业务用户。"
    />

    <a-table
      row-key="username"
      :columns="columns"
      :data-source="users"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      :scroll="{ x: 1250 }"
    >
      <template slot="status" slot-scope="text">
        <a-tag :color="text === 1 ? 'green' : 'red'">{{ text === 1 ? '启用' : '禁用' }}</a-tag>
      </template>
      <template slot="isAdmin" slot-scope="text, record">
        <a-tag :color="isSuperAdmin(record) ? 'purple' : 'default'">
          {{ isSuperAdmin(record) ? '超级管理员' : '普通用户' }}
        </a-tag>
      </template>
      <template slot="action" slot-scope="text, record">
        <a-button
          type="link"
          :disabled="record.username === 'admin'"
          :loading="record._saving"
          @click="toggleSuperAdmin(record)"
        >
          {{ isSuperAdmin(record) ? '取消超管' : '设为超管' }}
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script>
import axios from 'axios';
import { isRootAdmin } from '../../utils/currentUser';

export default {
  name: 'PlatformUsers',
  data() {
    return {
      loading: false,
      users: [],
      columns: [
        { title: '用户名', dataIndex: 'username', width: 170 },
        { title: '昵称', dataIndex: 'nickname', width: 180 },
        { title: '邮箱', dataIndex: 'email', width: 260 },
        { title: '认证方式', dataIndex: 'authType', width: 130 },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' }, width: 110 },
        { title: '角色', dataIndex: 'isAdmin', scopedSlots: { customRender: 'isAdmin' }, width: 150 },
        { title: '创建时间', dataIndex: 'createTime', width: 230 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 150, fixed: 'right' }
      ]
    };
  },
  mounted() {
    if (!isRootAdmin()) {
      this.$message.warning('仅 admin 用户可管理平台用户');
      this.$router.replace('/datamap');
      return;
    }
    this.fetchUsers();
  },
  methods: {
    isSuperAdmin(record) {
      return record.username === 'admin' || Number(record.isAdmin) === 1;
    },
    async fetchUsers() {
      this.loading = true;
      try {
        const res = await axios.get('/api/auth/platform-users');
        this.users = (res.data || []).map(item => ({ ...item, _saving: false }));
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载平台用户失败');
      } finally {
        this.loading = false;
      }
    },
    async toggleSuperAdmin(record) {
      if (record.username === 'admin') {
        this.$message.info('admin 默认拥有超级管理员权限');
        return;
      }
      const enabled = !this.isSuperAdmin(record);
      this.$set(record, '_saving', true);
      try {
        const res = await axios.put(
          `/api/auth/platform-users/${encodeURIComponent(record.username)}/super-admin`,
          null,
          { params: { enabled } }
        );
        Object.assign(record, res.data || {});
        this.$message.success(enabled ? '已设置为超级管理员' : '已取消超级管理员');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '超级管理员设置失败');
      } finally {
        this.$set(record, '_saving', false);
      }
    }
  }
};
</script>

<style scoped>
.platform-users {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h1 {
  margin: 0 0 6px;
}
.page-header p {
  color: #667085;
  margin: 0;
}
.page-alert {
  margin-bottom: 16px;
}

.platform-users >>> .ant-table-wrapper {
  overflow: hidden;
}

.platform-users >>> .ant-table-body {
  overflow-x: auto !important;
}
</style>
