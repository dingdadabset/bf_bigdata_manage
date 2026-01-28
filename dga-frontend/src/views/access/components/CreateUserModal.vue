<template>
  <a-modal :visible="visible" title="新建用户" @ok="submitUser" @cancel="$emit('cancel')" :confirmLoading="creatingUser">
    <a-form-model :model="userForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="创建方式">
        <a-radio-group v-model="userForm.creationStrategy">
          <a-radio-button value="LDAP">LDAP</a-radio-button>
          <a-radio-button value="IPA_HTTP">IPA(HTTP)</a-radio-button>
          <a-radio-button value="SELF_REG">自注册</a-radio-button>
        </a-radio-group>
      </a-form-model-item>
      <a-form-model-item label="所属集群">
        <a-select v-model="userForm.cluster">
          <a-select-option value="CDH-Cluster-01">CDH-Cluster-01</a-select-option>
          <a-select-option value="HDP-Production">HDP-Production</a-select-option>
          <a-select-option value="StarRocks-Financial">StarRocks-Financial</a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item label="用户名">
        <a-input v-model="userForm.username" @change="autoSplitName(userForm.username)" />
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
      userForm: {
        username: '',
        cluster: 'CDH-Cluster-01',
        creationStrategy: 'LDAP',
        firstName: '',
        lastName: '',
        password: '',
        confirmPassword: ''
      }
    };
  },
  methods: {
    autoSplitName(val) {
      this.userForm.firstName = val;
      this.userForm.lastName = 'User';
    },
    async submitUser() {
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
