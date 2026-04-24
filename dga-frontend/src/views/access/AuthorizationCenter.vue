<template>
  <div class="authorization-center">
    <div class="hero">
      <div>
        <p class="eyebrow">Authorization Center</p>
        <h1>授权中心</h1>
        <p>根据环境资源自动识别授权适配器，统一管理 Hive/Sentry、StarRocks SQL 和未来 Doris SQL 授权。</p>
      </div>
      <a-button type="primary" icon="reload" :loading="loading" @click="reloadAll">刷新能力</a-button>
    </div>

    <a-row :gutter="16" class="top-row">
      <a-col :xs="24" :lg="8">
        <a-card title="选择环境" :bordered="false" class="panel-card">
          <a-select
            v-model="selectedCluster"
            show-search
            style="width: 100%"
            placeholder="请选择环境"
            option-filter-prop="children"
            @change="onClusterChange"
          >
            <a-select-option
              v-for="cluster in clusters"
              :key="cluster.id"
              :value="cluster.clusterCode || cluster.clusterName"
            >
              {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
            </a-select-option>
          </a-select>
          <div class="hint">环境来自“环境资源注册”，授权能力由后端自动推导。</div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="16">
        <a-card :bordered="false" class="capability-card">
          <div v-if="capability" class="capability-grid">
            <div class="capability-item">
              <span>引擎</span>
              <strong>{{ capability.engineType || '-' }}</strong>
            </div>
            <div class="capability-item">
              <span>授权适配器</span>
              <strong>{{ capability.authBackend || '-' }}</strong>
            </div>
            <div class="capability-item">
              <span>授权通道</span>
              <strong>{{ capability.endpointType || '-' }}</strong>
              <small>{{ capability.endpointUrl || '未配置端点 URL' }}</small>
            </div>
            <div class="capability-item">
              <span>状态</span>
              <a-tag :color="statusColor(capability.status)">{{ capability.status || 'UNKNOWN' }}</a-tag>
            </div>
            <div class="capability-item">
              <span>LDAP 依赖</span>
              <strong>{{ capability.requiresLdap ? '需要' : '不需要' }}</strong>
            </div>
          </div>
          <a-empty v-else description="请选择环境查看授权能力" />
          <a-alert
            v-if="capability && capability.warnings && capability.warnings.length"
            class="capability-alert"
            type="warning"
            show-icon
            :message="capability.warnings.join('；')"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="统一授权操作" :bordered="false" class="operation-card">
      <a-form-model :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
            <a-form-model-item label="目标用户">
              <a-select
                v-model="form.username"
                show-search
                placeholder="请选择用户"
                option-filter-prop="children"
                :loading="loadingUsers"
              >
                <a-select-option v-for="user in users" :key="user" :value="user">
                  {{ user }}
                </a-select-option>
              </a-select>
              <div class="field-hint">{{ principalHint }}</div>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-model-item label="操作类型">
              <a-radio-group v-model="form.actionType" button-style="solid" @change="onActionChange">
                <a-radio-button value="GRANT">授权</a-radio-button>
                <a-radio-button value="REVOKE">回收</a-radio-button>
              </a-radio-group>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-model-item label="资源维度">
              <a-radio-group v-model="form.level" button-style="solid" @change="resetResourceSelection">
                <a-radio-button
                  v-for="type in resourceTypes"
                  :key="type"
                  :value="type"
                >
                  {{ type === 'DATABASE' ? '库级' : '表级' }}
                </a-radio-button>
              </a-radio-group>
            </a-form-model-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :xs="24" :md="form.level === 'TABLE' ? 8 : 12">
            <a-form-model-item label="数据库">
              <a-select
                v-if="form.level === 'DATABASE'"
                v-model="form.databases"
                mode="multiple"
                placeholder="请选择数据库"
                :loading="loadingResources"
              >
                <a-select-option v-for="db in databases" :key="db" :value="db">
                  {{ db }}
                </a-select-option>
              </a-select>
              <a-select
                v-else
                v-model="form.database"
                placeholder="请选择数据库"
                :loading="loadingResources"
                @change="onDatabaseChange"
              >
                <a-select-option v-for="db in databases" :key="db" :value="db">
                  {{ db }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col v-if="form.level === 'TABLE'" :xs="24" :md="8">
            <a-form-model-item label="表">
              <a-select
                v-model="form.tables"
                mode="multiple"
                placeholder="请选择表"
                :disabled="!form.database"
                :loading="loadingTables"
              >
                <a-select-option v-for="table in tables" :key="table" :value="table">
                  {{ table }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :xs="24" :md="form.level === 'TABLE' ? 8 : 12">
            <a-form-model-item label="权限类型">
              <a-select v-model="form.permission" placeholder="请选择权限">
                <a-select-option v-for="permission in permissions" :key="permission" :value="permission">
                  {{ permission }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>

        <div class="operation-footer">
          <div class="operation-summary">
            当前将通过 <strong>{{ capability ? capability.authBackend : '-' }}</strong>
            对 <strong>{{ selectedCluster || '-' }}</strong> 执行 {{ form.actionType === 'GRANT' ? '授权' : '回收' }}。
          </div>
          <a-button
            type="primary"
            icon="safety"
            :disabled="!canSubmit"
            :loading="submitting"
            @click="submit"
          >
            执行{{ form.actionType === 'GRANT' ? '授权' : '回收' }}
          </a-button>
        </div>
      </a-form-model>
    </a-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AuthorizationCenter',
  data() {
    return {
      loading: false,
      loadingUsers: false,
      loadingResources: false,
      loadingTables: false,
      submitting: false,
      clusters: [],
      users: [],
      capability: null,
      databases: [],
      tables: [],
      selectedCluster: '',
      form: {
        username: '',
        actionType: 'GRANT',
        level: 'DATABASE',
        databases: [],
        database: '',
        tables: [],
        permission: 'SELECT'
      }
    };
  },
  computed: {
    permissions() {
      return this.capability && this.capability.permissions && this.capability.permissions.length
        ? this.capability.permissions
        : ['SELECT'];
    },
    resourceTypes() {
      return this.capability && this.capability.resourceTypes && this.capability.resourceTypes.length
        ? this.capability.resourceTypes
        : ['DATABASE', 'TABLE'];
    },
    canSubmit() {
      if (!this.capability || this.capability.status !== 'READY') return false;
      if (!this.selectedCluster || !this.form.username || !this.form.permission) return false;
      if (this.form.level === 'DATABASE') {
        return this.form.databases && this.form.databases.length > 0;
      }
      return !!this.form.database && this.form.tables && this.form.tables.length > 0;
    },
    principalHint() {
      if (!this.capability) {
        return '请选择环境后加载授权用户';
      }
      if (this.capability.authBackend === 'STARROCKS_SQL') {
        return '用户列表来自 StarRocks：通过 SHOW USERS 查询，不依赖 LDAP。';
      }
      return '用户列表来自 DGA/LDAP 身份侧。';
    }
  },
  created() {
    this.reloadAll();
  },
  methods: {
    async reloadAll() {
      this.loading = true;
      try {
        await this.loadClusters();
      } finally {
        this.loading = false;
      }
    },
    async loadClusters() {
      const res = await axios.get('/api/clusters');
      this.clusters = (res.data || []).filter(item => item.status !== 'DELETED');
      if (!this.selectedCluster && this.clusters.length) {
        this.selectedCluster = this.clusters[0].clusterCode || this.clusters[0].clusterName;
        await this.onClusterChange();
      }
    },
    async loadUsers() {
      if (!this.selectedCluster) {
        this.users = [];
        return;
      }
      this.loadingUsers = true;
      try {
        const res = await axios.get('/api/access/resources/principals', {
          params: { cluster: this.selectedCluster }
        });
        this.users = res.data || [];
        if (!this.users.includes(this.form.username)) {
          this.form.username = '';
        }
      } catch (e) {
        this.users = [];
        this.$message.error(e.response?.data?.message || '加载授权用户失败，请检查授权端点配置');
      } finally {
        this.loadingUsers = false;
      }
    },
    async onClusterChange() {
      this.capability = null;
      this.resetResourceSelection();
      if (!this.selectedCluster) return;
      const res = await axios.get('/api/access/capabilities', { params: { cluster: this.selectedCluster } });
      this.capability = res.data || null;
      if (this.permissions.length) {
        this.form.permission = this.permissions[0];
      }
      if (this.capability && this.capability.status === 'READY') {
        this.loadDatabases();
        this.loadUsers();
      }
    },
    async loadDatabases() {
      this.loadingResources = true;
      try {
        const res = await axios.get('/api/access/resources/databases', { params: { cluster: this.selectedCluster } });
        this.databases = res.data || [];
      } catch (e) {
        this.databases = [];
        this.$message.error('加载数据库失败，请检查授权端点配置');
      } finally {
        this.loadingResources = false;
      }
    },
    async loadTables(database) {
      if (!database) return;
      this.loadingTables = true;
      try {
        const res = await axios.get('/api/access/resources/tables', {
          params: { cluster: this.selectedCluster, database }
        });
        this.tables = res.data || [];
      } catch (e) {
        this.tables = [];
        this.$message.error('加载表失败，请检查授权端点配置');
      } finally {
        this.loadingTables = false;
      }
    },
    onDatabaseChange(value) {
      if (this.form.level === 'TABLE') {
        this.form.database = value;
        this.form.tables = [];
        this.loadTables(value);
      }
    },
    onActionChange() {
      this.resetResourceSelection();
    },
    resetResourceSelection() {
      this.databases = [];
      this.tables = [];
      this.form.databases = [];
      this.form.database = '';
      this.form.tables = [];
      if (this.selectedCluster && this.capability && this.capability.status === 'READY') {
        this.loadDatabases();
      }
    },
    async submit() {
      const payload = {
        username: this.form.username,
        permission: this.form.permission,
        level: this.form.level,
        cluster: this.selectedCluster
      };
      if (this.form.level === 'DATABASE') {
        payload.databases = this.form.databases;
      } else {
        payload.tables = this.form.tables.map(table => ({
          database: this.form.database,
          table
        }));
      }

      this.submitting = true;
      try {
        const url = this.form.actionType === 'GRANT'
          ? '/api/access/grants/batch'
          : '/api/access/revokes/batch';
        await axios.post(url, payload);
        this.$message.success(this.form.actionType === 'GRANT' ? '授权成功' : '回收成功');
      } catch (e) {
        this.$message.error((this.form.actionType === 'GRANT' ? '授权失败: ' : '回收失败: ')
          + (e.response?.data?.message || e.message));
      } finally {
        this.submitting = false;
      }
    },
    statusColor(status) {
      switch (status) {
        case 'READY': return 'green';
        case 'PLANNED': return 'gold';
        case 'UNCONFIGURED': return 'red';
        default: return 'default';
      }
    }
  }
};
</script>

<style scoped>
.authorization-center {
  min-height: 100%;
}
.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  padding: 24px;
  border-radius: 10px;
  background: linear-gradient(135deg, #102a43 0%, #1f6f8b 55%, #37a2a5 100%);
  color: #fff;
  box-shadow: 0 12px 30px rgba(16, 42, 67, 0.18);
}
.hero h1 {
  color: #fff;
  margin: 0 0 8px;
}
.hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
}
.eyebrow {
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 12px;
  margin-bottom: 8px !important;
}
.top-row {
  margin-bottom: 16px;
}
.panel-card,
.capability-card,
.operation-card {
  border-radius: 8px;
}
.hint {
  color: #667085;
  font-size: 12px;
  margin-top: 12px;
}
.field-hint {
  color: #667085;
  font-size: 12px;
  margin-top: 6px;
}
.capability-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
}
.capability-item {
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfdff;
}
.capability-item span,
.capability-item small {
  display: block;
  color: #667085;
  font-size: 12px;
}
.capability-item strong {
  display: block;
  margin-top: 6px;
  color: #1f2d3d;
  word-break: break-all;
}
.capability-alert {
  margin-top: 14px;
}
.operation-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid #edf0f5;
}
.operation-summary {
  color: #667085;
}
@media (max-width: 900px) {
  .hero,
  .operation-footer {
    flex-direction: column;
    align-items: stretch;
  }
  .capability-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
