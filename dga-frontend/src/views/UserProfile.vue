<template>
  <div class="user-profile-container">
    <a-card :bordered="false" class="profile-card">
      <div class="profile-header">
        <a-avatar :size="100" icon="user" style="background-color: #1890ff; font-size: 48px;" />
        <div class="profile-info">
          <h2 class="username">{{ userInfo.username || 'Admin' }}</h2>
          <p class="role-tag"><a-tag color="blue">管理员</a-tag></p> <!-- Placeholder role -->
        </div>
      </div>
      
      <a-divider />
      
      <a-descriptions title="基本信息" bordered>
        <a-descriptions-item label="用户名">
          {{ userInfo.username || 'Admin' }}
        </a-descriptions-item>
        <a-descriptions-item label="手机号" v-if="userInfo.mobile">
          {{ userInfo.mobile }}
        </a-descriptions-item>
        <a-descriptions-item label="账号来源">
          <a-tag v-if="userInfo.provider === 'wechat'" color="green">微信</a-tag>
          <a-tag v-else-if="userInfo.provider === 'alipay'" color="blue">支付宝</a-tag>
          <a-tag v-else color="cyan">系统账号</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="注册时间" v-if="userInfo.createTime">
          {{ userInfo.createTime }}
        </a-descriptions-item>
        <a-descriptions-item label="最后登录" v-if="userInfo.lastLoginTime">
          {{ userInfo.lastLoginTime }}
        </a-descriptions-item>
      </a-descriptions>

      <div class="action-buttons" style="margin-top: 24px; text-align: center;">
        <a-button type="primary" icon="edit" @click="$message.info('功能开发中...')">编辑资料</a-button>
        <a-button style="margin-left: 16px;" icon="lock" @click="$message.info('功能开发中...')">修改密码</a-button>
      </div>
    </a-card>
  </div>
</template>

<script>
export default {
  name: 'UserProfile',
  data() {
    return {
      userInfo: {}
    };
  },
  created() {
    this.loadUserInfo();
  },
  methods: {
    loadUserInfo() {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          const userData = JSON.parse(userStr);
          // Handle different structures if user is nested or flat
          this.userInfo = userData.user || userData;
        } catch (e) {
          console.error('Failed to parse user info', e);
        }
      }
    }
  }
};
</script>

<style scoped>
.user-profile-container {
  padding: 24px;
  display: flex;
  justify-content: center;
}

.profile-card {
  width: 100%;
  max-width: 800px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  border-radius: 4px;
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
}

.profile-info {
  margin-top: 16px;
  text-align: center;
}

.username {
  font-size: 24px;
  font-weight: 600;
  color: rgba(0,0,0,0.85);
  margin-bottom: 8px;
}
</style>
