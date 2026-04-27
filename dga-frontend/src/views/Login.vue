<template>
  <div class="login-container">
    <div class="login-shell-bg"></div>
    <div class="login-content">
      <section class="brand-panel">
        <div class="brand-mark">
          <span class="mark-node"><a-icon type="cluster" /></span>
          <span>DGA</span>
        </div>
        <div class="brand-copy">
          <h1 class="login-title">DGA 数据治理平台</h1>
          <p class="login-subtitle">面向数据地图、元数据治理与统一授权的企业级工作台</p>
        </div>
        <div class="signal-board" aria-hidden="true">
          <div class="signal-row strong">
            <span>Metadata</span>
            <i></i>
            <b>Synced</b>
          </div>
          <div class="signal-row">
            <span>OpenLDAP</span>
            <i></i>
            <b>Ready</b>
          </div>
          <div class="signal-row">
            <span>Hive / Sentry</span>
            <i></i>
            <b>Active</b>
          </div>
          <div class="signal-row">
            <span>StarRocks</span>
            <i></i>
            <b>Online</b>
          </div>
        </div>
        <div class="brand-footer">
          <div>
            <strong>统一入口</strong>
            <span>权限、资产、质量一站管理</span>
          </div>
          <div>
            <strong>安全可控</strong>
            <span>按集群隔离身份与授权</span>
          </div>
        </div>
      </section>
      <section class="login-panel">
        <div class="panel-header">
          <span class="panel-kicker">Secure Workspace</span>
          <h2>欢迎回来</h2>
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
      </section>
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
  min-height: 100vh;
  background: #eef3f8;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 48px;
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.login-shell-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(31, 54, 82, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(31, 54, 82, 0.06) 1px, transparent 1px),
    linear-gradient(110deg, #f7fafc 0%, #edf4fa 45%, #e9f1ed 100%);
  background-size: 44px 44px, 44px 44px, cover;
}

.login-content {
  width: min(1100px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 430px;
  align-items: stretch;
  background: #ffffff;
  border: 1px solid #dce5ee;
  border-radius: 8px;
  box-shadow: 0 24px 70px rgba(32, 52, 74, 0.16);
  overflow: hidden;
  position: relative;
  z-index: 1;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-panel {
  background:
    linear-gradient(135deg, rgba(13, 35, 58, 0.92), rgba(21, 72, 96, 0.9)),
    url('../assets/login-bg.png');
  background-size: cover;
  background-position: center;
  color: #f7fbff;
  padding: 54px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
  background-size: 36px 36px;
  pointer-events: none;
}

.brand-mark,
.brand-copy,
.signal-board,
.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 700;
}

.mark-node {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #0f8f91;
  color: #fff;
}

.login-title {
  font-size: 40px;
  line-height: 1.2;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 0;
  margin: 86px 0 14px;
}

.login-subtitle {
  font-size: 17px;
  line-height: 1.8;
  color: rgba(240, 247, 255, 0.78);
  max-width: 460px;
  margin: 0;
  font-weight: 400;
}

.signal-board {
  width: min(430px, 100%);
  margin-top: 46px;
  border: 1px solid rgba(225, 239, 247, 0.2);
  border-radius: 8px;
  background: rgba(7, 20, 34, 0.38);
  backdrop-filter: blur(8px);
  padding: 12px;
}

.signal-row {
  display: grid;
  grid-template-columns: 130px 1fr 68px;
  gap: 12px;
  align-items: center;
  min-height: 40px;
  color: rgba(240, 247, 255, 0.72);
  font-size: 13px;
}

.signal-row + .signal-row {
  border-top: 1px solid rgba(229, 239, 247, 0.12);
}

.signal-row i {
  height: 2px;
  background: linear-gradient(90deg, #2db7b5, rgba(145, 213, 255, 0.2));
}

.signal-row b {
  color: #cbf3e2;
  font-weight: 600;
}

.signal-row.strong span,
.signal-row.strong b {
  color: #fff;
}

.brand-footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 46px;
}

.brand-footer div {
  border-top: 1px solid rgba(224, 241, 247, 0.22);
  padding-top: 14px;
}

.brand-footer strong,
.brand-footer span {
  display: block;
}

.brand-footer strong {
  color: #ffffff;
  font-size: 15px;
  margin-bottom: 6px;
}

.brand-footer span {
  color: rgba(240, 247, 255, 0.62);
  font-size: 13px;
}

.login-panel {
  background: #f8fafc;
  padding: 74px 46px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.panel-header {
  margin-bottom: 26px;
}

.panel-kicker {
  color: #0f8f91;
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.panel-header h2 {
  margin: 0;
  font-size: 28px;
  color: #172033;
  font-weight: 700;
}

.login-card {
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(30, 50, 72, 0.08);
  border-radius: 8px;
  border: 1px solid #e6edf5;
}

::v-deep .login-card .ant-card-body {
  padding: 30px;
}

::v-deep .ant-tabs-nav {
  width: 100%;
}

::v-deep .ant-tabs-nav .ant-tabs-tab {
  width: 50%;
  margin: 0;
  text-align: center;
  color: #667085;
}

::v-deep .ant-tabs-nav .ant-tabs-tab-active {
  color: #1677c8;
  font-weight: 600;
}

::v-deep .ant-tabs-bar {
  margin-bottom: 24px;
}

/* Enhancing Input Styles */
::v-deep .ant-input-lg {
  font-size: 16px;
  padding: 12px 11px;
  border-radius: 8px;
  background-color: #f7f9fc;
  border: 1px solid #dce5ee;
  transition: all 0.2s;
}

::v-deep .ant-input-lg:focus {
  background-color: #fff;
  border-color: #1677c8;
  box-shadow: 0 0 0 2px rgba(22, 119, 200, 0.1);
}

::v-deep .ant-input-prefix {
  color: #7b8794;
}

::v-deep .ant-btn-lg {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 10px 18px rgba(22, 119, 200, 0.22);
  letter-spacing: 0;
}

::v-deep .ant-btn-primary {
  background: #1677c8;
  border-color: #1677c8;
}

@media (max-width: 960px) {
  .login-container {
    padding: 24px;
  }

  .login-content {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    padding: 34px;
  }

  .login-title {
    margin-top: 48px;
    font-size: 32px;
  }

  .signal-board {
    display: none;
  }

  .brand-footer {
    margin-top: 32px;
  }

  .login-panel {
    padding: 34px;
  }
}

@media (max-width: 560px) {
  .login-container {
    padding: 14px;
    align-items: flex-start;
  }

  .brand-panel {
    padding: 26px;
  }

  .login-title {
    font-size: 27px;
  }

  .login-subtitle {
    font-size: 14px;
  }

  .brand-footer {
    grid-template-columns: 1fr;
  }

  .login-panel {
    padding: 24px;
  }

  ::v-deep .login-card .ant-card-body {
    padding: 22px;
  }
}

</style>
