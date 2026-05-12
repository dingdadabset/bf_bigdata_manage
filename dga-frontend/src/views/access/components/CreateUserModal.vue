<template>
  <a-modal :visible="visible" title="新建用户" @ok="submitUser" @cancel="$emit('cancel')" :confirmLoading="creatingUser">
    <a-form-model :model="userForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="创建方式">
        <a-tag :color="isSqlAuthCluster ? 'blue' : 'green'">{{ createBackendLabel }}</a-tag>
        <span class="form-hint">{{ createBackendHint }}</span>
      </a-form-model-item>
      <a-form-model-item label="所属集群">
        <a-select v-model="userForm.cluster" placeholder="请选择集群">
          <a-select-option v-for="cluster in clusters" :key="cluster.id" :value="cluster.clusterCode || cluster.clusterName">
            {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
          </a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item label="用户名">
        <a-input v-model="userForm.username" :placeholder="usernamePlaceholder" @change="autoSplitName(userForm.username)" />
        <div class="form-hint">{{ usernameHint }}</div>
      </a-form-model-item>
      <a-form-model-item label="邮箱">
        <a-input v-model="userForm.email" placeholder="可选，用于写入 LDAP mail 属性" />
      </a-form-model-item>
      <a-form-model-item label="用户类型">
        <a-select v-model="userForm.userType">
          <a-select-option value="INTERNAL">内部用户</a-select-option>
          <a-select-option value="OUTSOURCER">外包用户</a-select-option>
          <a-select-option value="TEMPORARY">临时用户</a-select-option>
          <a-select-option value="SERVICE">服务账号</a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item v-if="!isSqlAuthCluster" label="账号能力">
        <a-radio-group v-model="userForm.accountMode" button-style="solid">
          <a-radio-button value="LDAP_ONLY">仅 LDAP 身份</a-radio-button>
          <a-radio-button value="POSIX_ACCOUNT">Linux/POSIX 账号</a-radio-button>
        </a-radio-group>
        <div class="form-hint">
          仅 LDAP 身份用于应用认证；Linux/POSIX 账号会写入 uidNumber、gidNumber、homeDirectory、loginShell。
        </div>
      </a-form-model-item>
      <a-form-model-item v-if="!isSqlAuthCluster && isPosixAccount" label="所属组">
        <a-select
          v-model="userForm.groupName"
          placeholder="请选择 LDAP 组"
          @change="onGroupChange"
        >
          <a-select-option v-for="group in ldapGroups" :key="group.name" :value="group.name">
            {{ group.name }} (gid={{ group.gidNumber }})
          </a-select-option>
        </a-select>
        <div class="form-hint">创建系统账号时会写入所选组的 gidNumber。</div>
      </a-form-model-item>
      <a-form-model-item label="过期时间" :required="requiresExpiry">
        <a-input
          v-model="userForm.expiresAt"
          type="datetime-local"
          style="width: 100%"
          :disabled="!expiryEnabled"
          :min="minExpiryInput()"
          :placeholder="requiresExpiry ? '外包/临时用户必填' : '内部用户默认长期有效'"
        />
        <div class="form-hint">外包和临时用户必须设置过期时间；内部用户默认长期有效。</div>
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
import { store } from '../../../store';

export default {
  name: 'CreateUserModal',
  props: {
    visible: Boolean
  },
  data() {
    return {
      creatingUser: false,
      clusters: [],
      ldapGroups: [],
      userForm: {
        username: '',
        email: '',
        cluster: '',
        creationStrategy: 'OPENLDAP',
        accountMode: 'POSIX_ACCOUNT',
        groupName: '',
        gidNumber: null,
        userType: 'INTERNAL',
        expiresAt: '',
        firstName: '',
        lastName: '',
        password: '',
        confirmPassword: ''
      }
    };
  },
  computed: {
    requiresExpiry() {
      return ['OUTSOURCER', 'TEMPORARY'].includes(this.userForm.userType);
    },
    expiryEnabled() {
      return this.requiresExpiry || this.userForm.userType === 'SERVICE';
    },
    selectedCluster() {
      return this.clusters.find(cluster => (cluster.clusterCode || cluster.clusterName) === this.userForm.cluster) || null;
    },
    selectedClusterType() {
      return String(this.selectedCluster && this.selectedCluster.type ? this.selectedCluster.type : '').toUpperCase();
    },
    selectedSqlEngine() {
      return this.sqlEngineFromClusterType(this.selectedClusterType);
    },
    isSqlAuthCluster() {
      return Boolean(this.selectedSqlEngine);
    },
    createBackendLabel() {
      if (this.selectedSqlEngine === 'DORIS') return 'Doris SQL';
      if (this.selectedSqlEngine === 'STARROCKS') return 'StarRocks SQL';
      return 'OpenLDAP';
    },
    createBackendHint() {
      if (this.isSqlAuthCluster) {
        return '用户将直接写入当前 StarRocks/Doris 授权后端，不走 OpenLDAP。';
      }
      return '生产环境用户将写入所选集群配置的 LDAP endpoint';
    },
    usernamePlaceholder() {
      return this.isSqlAuthCluster ? '例如 analyst 或 analyst@%' : '请输入 LDAP 用户名';
    },
    usernameHint() {
      if (this.isSqlAuthCluster) {
        return 'StarRocks/Doris 用户名需以字母开头，仅支持字母、数字、下划线；可使用 user@host 指定 host，默认 @%。';
      }
      return 'LDAP 用户名用于目录账号创建。';
    },
    isPosixAccount() {
      return this.userForm.accountMode === 'POSIX_ACCOUNT';
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.syncClusterFromHeader();
      }
    },
    'userForm.cluster'(value) {
      if (value && !this.isSqlAuthCluster) {
        this.fetchLdapGroups();
      } else {
        this.ldapGroups = [];
        this.userForm.groupName = '';
        this.userForm.gidNumber = null;
        this.userForm.accountMode = 'LDAP_ONLY';
      }
    },
    'userForm.userType'() {
      if (!this.expiryEnabled) {
        this.userForm.expiresAt = '';
      }
    },
    'userForm.accountMode'(value) {
      if (value !== 'POSIX_ACCOUNT') {
        this.userForm.groupName = '';
        this.userForm.gidNumber = null;
      } else if (this.ldapGroups.length > 0) {
        const selected = this.ldapGroups[0];
        this.userForm.groupName = selected.name;
        this.userForm.gidNumber = selected.gidNumber;
      }
    }
  },
  mounted() {
    this.fetchClusters();
  },
  methods: {
    sqlEngineFromClusterType(type) {
      const value = String(type || '').toUpperCase();
      if (value.includes('DORIS')) return 'DORIS';
      if (value === 'SR' || value.includes('STARROCKS') || value.includes('STAR_ROCKS') || value.includes('STAR')) return 'STARROCKS';
      return '';
    },
    syncClusterFromHeader() {
      const preferred = store.headerSelectedCluster;
      if (preferred && this.clusters.some(cluster => (cluster.clusterCode || cluster.clusterName) === preferred)) {
        this.userForm.cluster = preferred;
      }
    },
    async fetchClusters() {
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = res.data;
        const preferred = store.headerSelectedCluster;
        const preferredExists = preferred && this.clusters.some(cluster => (cluster.clusterCode || cluster.clusterName) === preferred);
        if (preferredExists) {
          this.userForm.cluster = preferred;
        } else if (this.clusters.length > 0) {
          this.userForm.cluster = this.clusters[0].clusterCode || this.clusters[0].clusterName;
        }
      } catch (e) {
        console.error('Failed to fetch clusters', e);
        // Fallback or empty
      }
    },
    async fetchLdapGroups() {
      try {
        const res = await axios.get('/api/access/ldap-groups', {
          params: { cluster: this.userForm.cluster }
        });
        this.ldapGroups = Array.isArray(res.data) ? res.data : [];
        if (this.ldapGroups.length > 0) {
          const current = this.ldapGroups.find(group => group.name === this.userForm.groupName);
          const selected = current || this.ldapGroups[0];
          this.userForm.groupName = selected.name;
          this.userForm.gidNumber = selected.gidNumber;
        } else {
          this.userForm.groupName = '';
          this.userForm.gidNumber = null;
        }
      } catch (e) {
        console.error('Failed to fetch LDAP groups', e);
        this.ldapGroups = [];
        this.userForm.groupName = '';
        this.userForm.gidNumber = null;
      }
    },
    onGroupChange(value) {
      const selected = this.ldapGroups.find(group => group.name === value);
      this.userForm.groupName = value;
      this.userForm.gidNumber = selected ? selected.gidNumber : null;
    },
    minExpiryInput() {
      return this.formatDateTimeInput(new Date(Date.now() + 60 * 1000));
    },
    formatDateTimeInput(date) {
      const pad = value => String(value).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
    normalizeExpiry(value) {
      if (!value) return null;
      return value.length === 16 ? `${value}:00` : value;
    },
    autoSplitName(val) {
      const username = (val || '').split('@')[0];
      this.userForm.firstName = username;
      this.userForm.lastName = 'User';
    },
    validateSqlAuthUsername(value) {
      const text = String(value || '').trim();
      const atIndex = text.indexOf('@');
      const user = atIndex > 0 ? text.substring(0, atIndex).trim() : text;
      const host = atIndex > 0 ? text.substring(atIndex + 1).trim() : '%';
      return /^[A-Za-z][A-Za-z0-9_]{1,63}$/.test(user) && (host === '%' || /^[A-Za-z0-9_.%-]+$/.test(host));
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
      if (this.isSqlAuthCluster && !this.validateSqlAuthUsername(this.userForm.username)) {
        this.$message.warning('StarRocks/Doris 用户名需以字母开头，只能包含字母、数字、下划线；可选 user@host');
        return;
      }
      if (!this.userForm.password) {
        this.$message.warning('请输入密码');
        return;
      }
      if (this.isPosixAccount && (!this.userForm.groupName || !this.userForm.gidNumber)) {
        this.$message.warning('请选择所属 LDAP 组');
        return;
      }
      if (this.requiresExpiry && !this.userForm.expiresAt) {
        this.$message.warning('外包/临时用户必须设置过期时间');
        return;
      }
      if (this.userForm.password !== this.userForm.confirmPassword) {
        this.$message.warning('密码不一致');
        return;
      }
      this.creatingUser = true;
      try {
        const payload = {
          ...this.userForm,
          username: this.userForm.username.trim(),
          firstName: this.userForm.firstName || this.userForm.username.split('@')[0],
          lastName: this.userForm.lastName || 'User',
          accountMode: this.isSqlAuthCluster ? 'SQL_USER' : this.userForm.accountMode,
          creationStrategy: this.isSqlAuthCluster ? this.selectedSqlEngine : this.userForm.creationStrategy,
          userType: this.userForm.userType || 'EMPLOYEE',
          expiresAt: this.normalizeExpiry(this.userForm.expiresAt)
        };
        await axios.post('/api/access/user', payload);
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
