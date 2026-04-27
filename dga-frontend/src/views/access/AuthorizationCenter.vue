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

    <a-card :bordered="false" class="permission-card">
      <div class="card-title-row">
        <div>
          <h3>当前权限画像</h3>
          <p>从当前授权连接实时查询用户已有库表权限，授予或回收后可立即刷新对照。</p>
        </div>
        <div class="permission-toolbar">
          <a-select
            v-model="form.username"
            show-search
            placeholder="选择用户查看权限"
            option-filter-prop="children"
            :loading="loadingUsers"
            @change="onUserChange"
          >
            <a-select-option v-for="user in users" :key="user" :value="user">
              {{ user }}
            </a-select-option>
          </a-select>
          <a-button
            icon="reload"
            :disabled="!form.username || !selectedCluster"
            :loading="loadingPermissions"
            @click="loadPermissions"
          >
            刷新权限
          </a-button>
        </div>
      </div>

      <div v-if="permissionSnapshot" class="permission-summary">
        <div class="summary-pill">
          <span>用户</span>
          <strong>{{ permissionSnapshot.username }}</strong>
        </div>
        <div class="summary-pill">
          <span>来源</span>
          <strong>{{ permissionSnapshot.authBackend }}</strong>
        </div>
        <div class="summary-pill">
          <span>全局/系统</span>
          <strong>{{ globalGrantCount }}</strong>
        </div>
        <div class="summary-pill">
          <span>库/表权限</span>
          <strong>{{ databaseGrantCount }} / {{ tableGrantCount }}</strong>
        </div>
      </div>

      <a-table
        v-if="permissionSnapshot"
        class="permission-table"
        size="middle"
        row-key="id"
        :columns="permissionColumns"
        :data-source="permissionGrants"
        :loading="loadingPermissions"
        :pagination="{ pageSize: 6 }"
        :locale="{ emptyText: '当前用户暂无可见授权记录' }"
      >
        <template slot="resource" slot-scope="text, record">
          <div class="resource-cell">
            <a-tag :color="scopeColor(record.resourceType)">
              {{ scopeLabel(record.resourceType) }}
            </a-tag>
            <span>{{ displayResource(record) }}</span>
          </div>
        </template>
        <template slot="permission" slot-scope="text">
          <a-tag color="green">{{ text }}</a-tag>
        </template>
        <template slot="grantText" slot-scope="text">
          <a-tooltip v-if="text" :title="text">
            <code>{{ text }}</code>
          </a-tooltip>
          <span v-else>-</span>
        </template>
      </a-table>

      <a-empty
        v-else
        class="permission-empty"
        description="选择环境和用户后，查看该用户当前已有权限"
      />
    </a-card>

    <a-card :bordered="false" class="operation-card">
      <div class="operation-heading">
        <div>
          <p class="eyebrow dark">Grant Workbench</p>
          <h3>统一授权操作</h3>
          <p>先确认目标用户和动作，再选择授权范围。表级授权需要先选数据库，再加载该库下表。</p>
        </div>
        <a-tag :color="form.actionType === 'GRANT' ? 'blue' : 'orange'">
          {{ form.actionType === 'GRANT' ? '授权模式' : '回收模式' }}
        </a-tag>
      </div>
      <a-form-model :model="form" layout="vertical">
        <div class="operation-grid">
          <div class="operation-step">
            <div class="step-title"><span>1</span>目标与动作</div>
            <a-form-model-item label="目标用户">
              <a-select
                v-model="form.username"
                show-search
                placeholder="请选择用户"
                option-filter-prop="children"
                :loading="loadingUsers"
                @change="onUserChange"
              >
                <a-select-option v-for="user in users" :key="user" :value="user">
                  {{ user }}
                </a-select-option>
              </a-select>
              <div class="field-hint">{{ principalHint }}</div>
            </a-form-model-item>
            <a-form-model-item label="操作类型">
              <a-radio-group v-model="form.actionType" button-style="solid" @change="onActionChange">
                <a-radio-button value="GRANT">授权</a-radio-button>
                <a-radio-button value="REVOKE">回收</a-radio-button>
              </a-radio-group>
            </a-form-model-item>
          </div>

          <div class="operation-step resource-step">
            <div class="step-title"><span>2</span>资源范围</div>
            <a-form-model-item label="资源维度">
              <a-radio-group v-model="form.level" button-style="solid" @change="resetResourceSelection">
                <a-radio-button
                  v-for="type in resourceTypes"
                  :key="type"
                  :value="type"
                >
                  {{ type === 'DATABASE' ? '库级：库下所有表' : '表级：指定表' }}
                </a-radio-button>
              </a-radio-group>
            </a-form-model-item>

            <a-row :gutter="14">
              <a-col :xs="24" :md="form.level === 'TABLE' ? 10 : 14">
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
                  <div class="field-hint" v-if="form.level === 'DATABASE'">
                    StarRocks 库级 SELECT 会转换为 ALL TABLES IN DATABASE，不是 DATABASE 对象自身授权。
                  </div>
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
                  <div class="field-hint">表级授权会生成 ON TABLE db.table。</div>
                </a-form-model-item>
              </a-col>
              <a-col :xs="24" :md="form.level === 'TABLE' ? 6 : 10">
                <a-form-model-item label="权限类型">
                  <a-select v-model="form.permission" placeholder="请选择权限">
                    <a-select-option v-for="permission in permissions" :key="permission" :value="permission">
                      {{ permission }}
                    </a-select-option>
                  </a-select>
                </a-form-model-item>
              </a-col>
            </a-row>
          </div>
        </div>

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
      loadingPermissions: false,
      submitting: false,
      clusters: [],
      users: [],
      capability: null,
      permissionSnapshot: null,
      databases: [],
      tables: [],
      selectedCluster: '',
      permissionColumns: [
        { title: '作用范围 / 资源', key: 'resource', scopedSlots: { customRender: 'resource' } },
        { title: '权限', dataIndex: 'permission', key: 'permission', scopedSlots: { customRender: 'permission' } },
        { title: '原始授权语句 / 后端返回', dataIndex: 'grantText', key: 'grantText', scopedSlots: { customRender: 'grantText' } }
      ],
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
    permissionGrants() {
      return this.permissionSnapshot && this.permissionSnapshot.grants
        ? this.permissionSnapshot.grants
        : [];
    },
    databaseGrantCount() {
      return this.permissionGrants.filter(item => item.resourceType === 'DATABASE').length;
    },
    tableGrantCount() {
      return this.permissionGrants.filter(item => item.resourceType === 'TABLE').length;
    },
    globalGrantCount() {
      return this.permissionGrants.filter(item =>
        ['GLOBAL', 'VIEW', 'MATERIALIZED_VIEW', 'FUNCTION'].includes(item.resourceType)
      ).length;
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
          this.permissionSnapshot = null;
        } else {
          this.loadPermissions();
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
      this.permissionSnapshot = null;
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
    onUserChange() {
      this.permissionSnapshot = null;
      this.loadPermissions();
    },
    async loadPermissions() {
      if (!this.selectedCluster || !this.form.username) {
        this.permissionSnapshot = null;
        return;
      }
      this.loadingPermissions = true;
      try {
        const res = await axios.get('/api/access/resources/permissions', {
          params: {
            cluster: this.selectedCluster,
            username: this.form.username
          }
        });
        this.permissionSnapshot = res.data || null;
      } catch (e) {
        this.permissionSnapshot = null;
        this.$message.error(e.response?.data?.message || '加载当前权限失败，请检查授权端点账号权限');
      } finally {
        this.loadingPermissions = false;
      }
    },
    displayResource(record) {
      if (!record) return '-';
      if (record.resourceType === 'TABLE') {
        return `${record.databaseName || '-'}.${record.tableName || '*'}`;
      }
      if (['VIEW', 'MATERIALIZED_VIEW', 'FUNCTION'].includes(record.resourceType)) {
        const database = record.databaseName || 'ALL DATABASES';
        return `${database} / ${record.tableName || this.scopeLabel(record.resourceType)}`;
      }
      if (record.resourceType === 'GLOBAL') {
        return record.databaseName || 'ALL DATABASES';
      }
      return record.databaseName || '未解析资源';
    },
    scopeLabel(type) {
      const labels = {
        GLOBAL: '全局',
        DATABASE: '库级',
        TABLE: '表级',
        VIEW: '视图',
        MATERIALIZED_VIEW: '物化视图',
        FUNCTION: '函数'
      };
      return labels[type] || type || '未知';
    },
    scopeColor(type) {
      const colors = {
        GLOBAL: 'purple',
        DATABASE: 'cyan',
        TABLE: 'blue',
        VIEW: 'geekblue',
        MATERIALIZED_VIEW: 'volcano',
        FUNCTION: 'gold'
      };
      return colors[type] || 'default';
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
        this.loadPermissions();
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
.eyebrow.dark {
  color: #667085;
}
.top-row {
  margin-bottom: 16px;
}
.panel-card,
.capability-card,
.permission-card,
.operation-card {
  border-radius: 8px;
}
.permission-card {
  margin-bottom: 16px;
}
.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
.card-title-row h3 {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-weight: 700;
}
.card-title-row p {
  margin: 0;
  color: #667085;
}
.permission-toolbar {
  display: flex;
  gap: 10px;
  min-width: 360px;
}
.permission-toolbar .ant-select {
  flex: 1;
}
.permission-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-pill {
  padding: 12px 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f6fbff 0%, #eef8f8 100%);
  border: 1px solid #e2eef5;
}
.summary-pill span {
  display: block;
  color: #667085;
  font-size: 12px;
}
.summary-pill strong {
  display: block;
  margin-top: 6px;
  color: #102a43;
  word-break: break-all;
}
.permission-table code {
  display: inline-block;
  max-width: 520px;
  color: #334e68;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}
.permission-empty {
  padding: 16px 0;
}
.resource-cell {
  display: flex;
  align-items: center;
  gap: 8px;
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
.operation-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
}
.operation-heading h3 {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-weight: 700;
}
.operation-heading p {
  margin: 0;
  color: #667085;
}
.operation-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}
.operation-step {
  padding: 18px;
  border: 1px solid #e6edf3;
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fbfc 100%);
}
.resource-step {
  background: #fff;
}
.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #102a43;
  font-weight: 700;
}
.step-title span {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #1890ff;
  color: #fff;
  font-size: 12px;
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
  .card-title-row,
  .operation-heading,
  .operation-footer {
    flex-direction: column;
    align-items: stretch;
  }
  .permission-toolbar {
    min-width: 0;
  }
  .capability-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
  .permission-summary {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
  .operation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
