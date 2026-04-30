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
        <a-button style="margin-left: 16px;" icon="lock" @click="openPasswordModal">修改密码</a-button>
      </div>
    </a-card>

    <a-modal
      title="修改密码"
      :visible="passwordModalVisible"
      :confirm-loading="passwordSaving"
      ok-text="保存新密码"
      @ok="changePassword"
      @cancel="passwordModalVisible = false"
    >
      <a-alert
        message="修改成功后需要重新登录。仅支持 DGA 平台本地账号，LDAP / OAuth 账号请在对应认证系统修改密码。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form-model ref="passwordForm" :model="passwordForm" :rules="passwordRules" layout="vertical">
        <a-form-model-item label="用户名">
          <a-input :value="userInfo.username || '-'" disabled />
        </a-form-model-item>
        <a-form-model-item label="原密码" prop="oldPassword">
          <a-input-password v-model="passwordForm.oldPassword" placeholder="请输入原密码" />
        </a-form-model-item>
        <a-form-model-item label="新密码" prop="newPassword">
          <a-input-password v-model="passwordForm.newPassword" placeholder="至少 6 位" />
        </a-form-model-item>
        <a-form-model-item label="确认新密码" prop="confirmPassword">
          <a-input-password v-model="passwordForm.confirmPassword" placeholder="再次输入新密码" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import { clearAuthCache, getCurrentUser } from '../utils/currentUser';

export default {
  name: 'UserProfile',
  data() {
    return {
      userInfo: {},
      passwordModalVisible: false,
      passwordSaving: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.loadUserInfo();
  },
  methods: {
    loadUserInfo() {
      this.userInfo = getCurrentUser();
    },
    validateConfirmPassword(rule, value, callback) {
      if (value && value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'));
      } else {
        callback();
      }
    },
    openPasswordModal() {
      this.passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      };
      this.passwordModalVisible = true;
      this.$nextTick(() => this.$refs.passwordForm && this.$refs.passwordForm.clearValidate());
    },
    changePassword() {
      this.$refs.passwordForm.validate(async valid => {
        if (!valid) return;
        this.passwordSaving = true;
        try {
          await axios.post('/api/auth/change-password', {
            username: this.userInfo.username,
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword
          });
          this.$message.success('密码已修改，请重新登录');
          this.passwordModalVisible = false;
          clearAuthCache();
          this.$router.replace('/login');
        } catch (e) {
          this.$message.error(e.response?.data?.message || e.response?.data || '密码修改失败');
        } finally {
          this.passwordSaving = false;
        }
      });
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
