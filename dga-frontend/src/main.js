import Vue from 'vue'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/antd.css';
import axios from 'axios';
import App from './App.vue'
import router from './router'
import './style.css'
import { clearAuthCache, getAuthToken } from './utils/currentUser'

Vue.config.productionTip = false

Vue.use(Antd);

axios.interceptors.request.use(config => {
  const token = getAuthToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401 && router.currentRoute.path !== '/login') {
      clearAuthCache();
      router.push('/login');
    }
    return Promise.reject(error);
  }
);

new Vue({
  router,
  render: (h) => h(App)
}).$mount('#app')
