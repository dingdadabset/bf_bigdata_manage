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
  height: 100vh;
  background: #f0f2f5;
  background-image: url('https://gw.alipayobjects.com/zos/rmsportal/TVYTbAXWheQpRcWDaDMu.svg');
  background-repeat: no-repeat;
  background-position: center 110px;
  background-size: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

.login-content {
  width: 368px;
  margin-bottom: 100px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 33px;
  color: rgba(0, 0, 0, 0.85);
  font-family: 'Myriad Pro', 'Helvetica Neue', Arial, Helvetica, sans-serif;
  font-weight: 600;
}

.login-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 12px;
}

.login-card {
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
</style>
