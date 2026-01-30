<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-header">
        <h1 class="login-title">DGA 数据治理平台</h1>
        <p class="login-subtitle">Data Governance & Analytics Platform</p>
      </div>
      <a-card :bordered="false" class="login-card">
        <a-tabs v-model="activeTab" :tab-bar-style="{ textAlign: 'center' }">
          <a-tab-pane key="1" tab="账号登录">
            <a-form-model ref="loginForm" :model="form" :rules="rules">
              <a-form-model-item prop="username">
                <a-input v-model="form.username" size="large" placeholder="用户名">
                  <a-icon slot="prefix" type="user" />
                </a-input>
              </a-form-model-item>
              <a-form-model-item prop="password">
                <a-input-password v-model="form.password" size="large" placeholder="密码">
                  <a-icon slot="prefix" type="lock" />
                </a-input-password>
              </a-form-model-item>
              <a-form-model-item>
                <a-button type="primary" size="large" block @click="handleSubmit" :loading="loading">
                  登录
                </a-button>
              </a-form-model-item>
              <a-divider>其他登录方式</a-divider>
              <div class="social-login">
                <div class="social-icon-wrapper" @click="handleSocialLogin('wechat')">
                  <a-icon type="wechat" class="social-icon" />
                  <span class="social-text">微信</span>
                </div>
                <div class="social-icon-wrapper" @click="handleSocialLogin('alipay')">
                  <a-icon type="alipay-circle" class="social-icon" />
                  <span class="social-text">支付宝</span>
                </div>
              </div>
            </a-form-model>
          </a-tab-pane>
          <a-tab-pane key="2" tab="注册账号">
            <a-form-model ref="registerForm" :model="regForm" :rules="regRules">
              <a-form-model-item prop="username">
                <a-input v-model="regForm.username" size="large" placeholder="用户名" />
              </a-form-model-item>
              <a-form-model-item prop="email">
                <a-input v-model="regForm.email" size="large" placeholder="邮箱" />
              </a-form-model-item>
              <a-form-model-item prop="password">
                <a-input-password v-model="regForm.password" size="large" placeholder="密码" />
              </a-form-model-item>
              <a-form-model-item prop="confirmPassword">
                <a-input-password v-model="regForm.confirmPassword" size="large" placeholder="确认密码" />
              </a-form-model-item>
              <a-form-model-item>
                <a-button type="primary" size="large" block @click="handleRegister" :loading="registering">
                  注册
                </a-button>
              </a-form-model-item>
            </a-form-model>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      activeTab: '1',
      loading: false,
      registering: false,
      form: {
        username: '',
        password: ''
      },
      regForm: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      regRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: this.validateConfirmPassword, trigger: 'blur' }]
      }
    };
  },
  mounted() {
    const token = this.$route.query.token;
    const username = this.$route.query.username;
    if (token) {
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify({ username }));
      this.$message.success('登录成功');
      this.$router.push('/');
    }
  },
  methods: {
    handleSocialLogin(provider) {
      this.$message.info('功能还在开发中');
      // window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    },
    validateConfirmPassword(rule, value, callback) {
      if (value && value !== this.regForm.password) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    },
    handleSubmit() {
      this.$refs.loginForm.validate(async valid => {
        if (valid) {
          this.loading = true;
          try {
            const response = await axios.post('/api/auth/login', this.form);
            localStorage.setItem('user', JSON.stringify(response.data));
            this.$message.success('登录成功');
            this.$router.push('/');
          } catch (e) {
            this.$message.error('登录失败: ' + (e.response?.data || '用户名或密码错误'));
          } finally {
            this.loading = false;
          }
        }
      });
    },
    async handleRegister() {
      this.$refs.registerForm.validate(async valid => {
        if (valid) {
          this.registering = true;
          try {
            await axios.post('/api/auth/register', {
              username: this.regForm.username,
              password: this.regForm.password,
              email: this.regForm.email,
              firstName: this.regForm.username,
              lastName: 'User'
            });
            this.$message.success('注册成功，已自动跳转至登录页');
            
            // Auto-fill login form
            this.form.username = this.regForm.username;
            this.form.password = this.regForm.password;
            
            // Clear register form
            this.regForm = { username: '', email: '', password: '', confirmPassword: '' };
            
            // Switch to login tab (assuming key "1" is login)
            this.activeTab = '1'; 
          } catch (e) {
            this.$message.error('注册失败: ' + (e.response?.data || e.message));
          } finally {
            this.registering = false;
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  /* Light gray gradient as base */
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  /* If image exists, it overlays */
  background-image: url('../assets/login-bg.png'), linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.login-content {
  width: 420px;
  margin-bottom: 60px;
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 32px;
  color: #1f2937; /* Darker, sharper text */
  font-weight: 700;
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 16px;
  color: #6b7280; /* Muted tech gray */
  margin-top: 8px;
  font-weight: 400;
}

.login-card {
  background: #ffffff;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08); /* Soft diffused shadow */
  border-radius: 16px; /* Modern rounded corners */
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

/* Enhancing Input Styles */
::v-deep .ant-input-lg {
  font-size: 16px;
  padding: 12px 11px;
  border-radius: 8px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  transition: all 0.3s;
}

::v-deep .ant-input-lg:focus {
  background-color: #fff;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

::v-deep .ant-btn-lg {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(24, 144, 255, 0.3);
  letter-spacing: 0.5px;
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 48px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.social-icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  color: #9ca3af;
  transition: all 0.3s ease;
}

.social-icon-wrapper:hover {
  color: #1890ff;
  transform: translateY(-3px);
}

.social-icon {
  font-size: 32px;
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.05));
}

.social-text {
  font-size: 13px;
  font-weight: 500;
}
</style>
