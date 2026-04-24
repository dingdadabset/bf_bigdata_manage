<template>
  <a-modal :visible="visible" title="新建用户" @ok="submitUser" @cancel="$emit('cancel')" :confirmLoading="creatingUser">
    <a-form-model :model="userForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="创建方式">
        <a-tag color="green">OpenLDAP</a-tag>
        <span class="form-hint">生产环境用户将写入所选集群配置的 LDAP endpoint</span>
      </a-form-model-item>
      <a-form-model-item label="所属集群">
        <a-select v-model="userForm.cluster" placeholder="请选择集群">
          <a-select-option v-for="cluster in clusters" :key="cluster.id" :value="cluster.clusterCode || cluster.clusterName">
            {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
          </a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item label="用户名">
        <a-input v-model="userForm.username" @change="autoSplitName(userForm.username)" />
      </a-form-model-item>
      <a-form-model-item label="邮箱">
        <a-input v-model="userForm.email" placeholder="可选，用于写入 LDAP mail 属性" />
      </a-form-model-item>
      <a-form-model-item label="密码">
        <a-input-password v-model="userForm.password" />
      </a-form-model-item>
      <a-form-model-item label="确认密码">
        <a-input-password v-model="userForm.confirmPassword" />
      </a-form-model-item>
    </a-form-model>
  </a-modal>
</template>

<script>
import axios from 'axios';
import { mutations } from '../../../store';

export default {
  name: 'CreateUserModal',
  props: {
    visible: Boolean
  },
  data() {
    return {
      creatingUser: false,
      clusters: [],
      userForm: {
        username: '',
        email: '',
        cluster: '',
        creationStrategy: 'OPENLDAP',
        firstName: '',
        lastName: '',
        password: '',
        confirmPassword: ''
      }
    };
  },
  mounted() {
    this.fetchClusters();
  },
  methods: {
    async fetchClusters() {
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = res.data;
        if (this.clusters.length > 0) {
          this.userForm.cluster = this.clusters[0].clusterCode || this.clusters[0].clusterName;
        }
      } catch (e) {
        console.error('Failed to fetch clusters', e);
        // Fallback or empty
      }
    },
    autoSplitName(val) {
      this.userForm.firstName = val;
      this.userForm.lastName = 'User';
    },
    async submitUser() {
      if (!this.userForm.cluster) {
        this.$message.warning('请选择所属集群');
        return;
      }
      if (!this.userForm.username || !this.userForm.username.trim()) {
        this.$message.warning('请输入用户名');
        return;
      }
      if (!this.userForm.password) {
        this.$message.warning('请输入密码');
        return;
      }
      if (this.userForm.password !== this.userForm.confirmPassword) {
        this.$message.warning('密码不一致');
        return;
      }
      this.creatingUser = true;
      try {
        await axios.post('/api/access/user', this.userForm);
        this.$message.success('创建成功');
        this.$emit('ok');
      } catch (e) {
        console.error(e);
        const msg = e.response?.data?.message || e.message || '未知错误';
        if (msg.toLowerCase().includes('already exists') || (e.response && e.response.status === 409)) {
          this.$warning({
            title: '用户重复',
            content: `用户 ${this.userForm.username} 已存在，请勿重复创建。`,
            okText: '确认',
            maskClosable: true
          });
        } else {
          this.$message.error('创建失败: ' + msg);
        }
      } finally {
        this.creatingUser = false;
      }
    }
  }
};
</script>

<style scoped>
.form-hint {
  margin-left: 8px;
  color: #667085;
  font-size: 12px;
}
</style>
