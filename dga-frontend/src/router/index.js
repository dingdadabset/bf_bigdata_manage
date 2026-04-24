import Vue from 'vue';
import VueRouter from 'vue-router';
import Login from '../views/Login.vue';
import MainLayout from '../layouts/MainLayout.vue';
import AccessIndex from '../views/access/AccessIndex.vue';
import DataSourceManagement from '../components/DataSourceManagement.vue';
import Metadata from '../views/Metadata.vue';
import MetadataDetail from '../views/MetadataDetail.vue';
import Quality from '../views/Quality.vue';
import DataMap from '../views/DataMap.vue';
import UserProfile from '../views/UserProfile.vue';
import ResourceManagement from '../views/ResourceManagement.vue';
import ClusterManagement from '../views/system/ClusterManagement.vue';
import AuthorizationCenter from '../views/access/AuthorizationCenter.vue';

Vue.use(VueRouter);

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: '/datamap'
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: UserProfile,
        meta: { title: '个人中心' }
      },
      {
        path: 'datamap',
        name: 'DataMap',
        component: DataMap,
        meta: { title: '数据地图' }
      },
      {
        path: 'datasource',
        name: 'DataSourceManagement',
        component: DataSourceManagement,
        meta: { title: '数据源管理' }
      },
      {
        path: 'access',
        name: 'AccessManagement',
        component: AccessIndex,
        meta: { title: '用户与权限管理' }
      },
      {
        path: 'authorization-center',
        name: 'AuthorizationCenter',
        component: AuthorizationCenter,
        meta: { title: '授权中心' }
      },
      {
        path: 'metadata',
        name: 'Metadata',
        component: Metadata,
        meta: { title: '元数据管理' }
      },
      {
        path: 'metadata/detail/:id',
        name: 'MetadataDetail',
        component: MetadataDetail,
        meta: { title: '表详情' }
      },
      {
        path: 'quality',
        name: 'Quality',
        component: Quality,
        meta: { title: '数据质量中心' }
      },
      {
        path: 'resources',
        name: 'ResourceManagement',
        component: ResourceManagement,
        meta: { title: '资源导航管理' }
      },
      {
        path: 'environment-resources',
        name: 'EnvironmentResources',
        component: ClusterManagement,
        meta: { title: '环境资源注册' }
      },
      {
        path: 'cluster-management',
        name: 'ClusterManagement',
        component: ClusterManagement,
        meta: { title: '集群管理' }
      }
    ]
  }
];

const router = new VueRouter({
  mode: 'history',
  base: '/',
  routes
});

// Navigation guard
router.beforeEach((to, from, next) => {
  const publicPages = ['/login'];
  const authRequired = !publicPages.includes(to.path);
  const loggedIn = localStorage.getItem('user');

  if (authRequired && !loggedIn) {
    return next('/login');
  }

  next();
});

export default router;
