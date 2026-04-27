import Vue from 'vue'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/antd.css';
import axios from 'axios';
import App from './App.vue'
import router from './router'
import './style.css'
import { getCurrentUsername } from './utils/currentUser'

Vue.config.productionTip = false

Vue.use(Antd);

axios.interceptors.request.use(config => {
  const username = getCurrentUsername();
  if (username) {
    config.headers = config.headers || {};
    config.headers['X-DGA-Username'] = username;
  }
  return config;
});

new Vue({
  router,
  render: (h) => h(App)
}).$mount('#app')
