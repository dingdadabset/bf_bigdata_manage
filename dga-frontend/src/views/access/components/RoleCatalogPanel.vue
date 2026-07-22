<template>
  <a-card :bordered="false" class="role-card" :title="cardTitle">
    <div class="toolbar">
      <a-input-search v-model="keyword" placeholder="搜索角色编码或名称" allow-clear />
      <a-tag color="blue">{{ filteredRoles.length }} 个角色</a-tag>
      <a-tooltip v-if="allowManageRoles && !canSyncBackendRoles" title="当前授权后端没有原生角色盘点能力，Ranger 角色请在 DGA 本地维护，授权时会物化为 Ranger 策略。">
        <a-button icon="cloud-sync" size="small" disabled>
          同步后端角色
        </a-button>
      </a-tooltip>
      <a-button v-else-if="allowManageRoles" icon="cloud-sync" size="small" :disabled="!capability" @click="$emit('sync-backend-roles')">
        同步后端角色
      </a-button>
      <a-button v-if="allowManageRoles" type="primary" icon="plus" size="small" :disabled="!capability" @click="openRoleModal()">
        新增角色
      </a-button>
    </div>

    <div v-if="selectedRole" class="selected-role-summary">
      <div>
        <div class="selected-role-label">当前选中</div>
        <strong>{{ selectedRole.roleName || selectedRole.roleCode }}</strong>
        <div class="selected-role-code">{{ selectedRole.roleCode }}</div>
      </div>
      <div class="selected-role-stats">
        <a-tag color="blue">权限 {{ rolePermissionCount }}</a-tag>
        <a-tag color="green">绑定 {{ assignmentCount }}</a-tag>
      </div>
    </div>

    <a-spin :spinning="loadingCatalog">
      <div v-if="filteredRoles.length" class="role-list">
        <button
          v-for="item in filteredRoles"
          :key="item.role.roleCode"
          type="button"
          class="role-item"
          :class="{
            active: selectedRoleCode === item.role.roleCode,
            'is-bound': !!subjectBoundRoleMetaMap[item.role.roleCode],
            'is-inherited': subjectBoundRoleMetaMap[item.role.roleCode]?.bindingMode === 'GROUP_INHERITED'
          }"
          @click="$emit('select-role', item.role.roleCode)"
        >
          <div class="role-header">
            <strong>{{ item.role.roleName || item.role.roleCode }}</strong>
            <div class="role-badges">
              <a-tag :color="item.role.status === 'ACTIVE' ? 'green' : 'default'">{{ item.role.status || 'UNKNOWN' }}</a-tag>
              <a-tag
                v-if="subjectBoundRoleMetaMap[item.role.roleCode]"
                :color="subjectBoundRoleMetaMap[item.role.roleCode].bindingMode === 'GROUP_INHERITED' ? 'blue' : 'green'"
              >
                {{ subjectBoundRoleMetaMap[item.role.roleCode].bindingLabel }}
              </a-tag>
            </div>
          </div>
          <div class="role-code">{{ item.role.roleCode }}</div>
          <div class="role-meta">
            <span>{{ item.role.authBackend || capabilityAuthBackend || '-' }}</span>
            <span>{{ item.role.riskLevel || 'LOW' }}</span>
          </div>
          <div class="role-stats">
            <span>权限 {{ item.permissions ? item.permissions.length : 0 }}</span>
            <span>绑定 {{ item.assignments ? item.assignments.length : 0 }}</span>
          </div>
          <div v-if="subjectBoundRoleMetaMap[item.role.roleCode]?.matchedGroupName" class="role-hint">
            来源组 {{ subjectBoundRoleMetaMap[item.role.roleCode].matchedGroupName }}
          </div>
        </button>
      </div>
      <a-empty v-else description="当前集群/后端下暂无角色" />
    </a-spin>

    <div class="catalog-hint">
      {{ catalogHint }}
    </div>

    <a-modal
      :visible="roleModalVisible"
      :title="roleForm.id ? '编辑角色' : '新增角色'"
      width="680px"
      @ok="submitRole"
      @cancel="roleModalVisible = false"
    >
      <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="业务域">
          <a-input v-model="roleForm.domain" :disabled="!!roleForm.id" placeholder="例如 pay、risk、ods" @change="syncGeneratedRoleFields" />
        </a-form-model-item>
        <a-form-model-item label="资源范围">
          <a-input v-model="roleForm.scope" :disabled="!!roleForm.id" placeholder="例如 aggr_pay、some_db、project_x" @change="syncGeneratedRoleFields" />
        </a-form-model-item>
        <a-form-model-item label="权限级别">
          <a-select v-model="roleForm.level" :disabled="!!roleForm.id" @change="onRoleLevelChange">
            <a-select-option v-for="item in permissionLevelOptions" :key="item.code" :value="item.code">
              {{ item.label }}（{{ item.permissions.join(' + ') }}）
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="角色编码">
          <a-input v-if="roleForm.id || roleForm.manualRoleCode" v-model="roleForm.roleCode" :disabled="!!roleForm.id" placeholder="例如 dga_cdh_sentry_pay_readonly" />
          <div v-else class="generated-role-code">{{ generatedRoleCode }}</div>
          <a-checkbox v-if="!roleForm.id" v-model="roleForm.manualRoleCode" class="manual-code-toggle">
            手动覆盖角色编码
          </a-checkbox>
        </a-form-model-item>
        <a-form-model-item label="角色名称">
          <a-input v-model="roleForm.roleName" placeholder="例如 支付域只读分析" />
        </a-form-model-item>
        <a-form-model-item label="Owner">
          <a-input v-model="roleForm.owner" placeholder="例如 data_platform_owner" />
        </a-form-model-item>
        <a-form-model-item label="风险级别">
          <a-select v-model="roleForm.riskLevel">
            <a-select-option value="LOW">LOW</a-select-option>
            <a-select-option value="MEDIUM">MEDIUM</a-select-option>
            <a-select-option value="HIGH">HIGH</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="状态">
          <a-select v-model="roleForm.status">
            <a-select-option value="ACTIVE">ACTIVE</a-select-option>
            <a-select-option value="INACTIVE">INACTIVE</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="过期时间">
          <a-input v-model="roleForm.expiresAt" type="datetime-local" placeholder="长期有效可留空" />
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="roleForm.description" :rows="3" placeholder="说明角色用途、适用人群和审批边界" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal
      :visible="permissionModalVisible"
      title="添加权限范围"
      width="720px"
      @ok="submitPermissions"
      @cancel="permissionModalVisible = false"
    >
      <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="资源维度">
          <a-radio-group v-model="permissionForm.resourceType" button-style="solid" @change="onPermissionResourceTypeChange">
            <a-radio-button value="DATABASE">库级</a-radio-button>
            <a-radio-button value="TABLE">表级</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="数据库">
          <a-select
            v-model="permissionForm.databaseNames"
            mode="multiple"
            show-search
            option-filter-prop="children"
            placeholder="请选择数据库"
            :loading="resourceLoading.databases"
            @change="onPermissionDatabasesChange"
          >
            <a-select-option v-for="item in databaseSelectOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
          <a-alert
            v-if="isStarRocksBackend"
            class="form-hint"
            type="info"
            show-icon
            message="StarRocks 角色范围请使用具体数据库，便于后续按子集授权。"
          />
          <div class="quick-actions">
            <a-button size="small" :disabled="shouldSelectConcreteDatabases && !databases.length" @click="selectAllDatabases">
              {{ allDatabaseButtonText }}
            </a-button>
            <a-button size="small" @click="permissionForm.databaseNames = []; permissionForm.tableSelections = []">清空</a-button>
          </div>
        </a-form-model-item>
        <a-form-model-item v-if="permissionForm.resourceType === 'TABLE'" label="表">
          <a-select
            v-model="permissionForm.tableSelections"
            mode="multiple"
            show-search
            option-filter-prop="children"
            placeholder="请选择表"
            :loading="resourceLoading.tables"
          >
            <a-select-option v-for="item in tableOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
          <div class="quick-actions">
            <a-button size="small" @click="permissionForm.tableSelections = tableOptions.map(item => item.value)">全选表</a-button>
            <a-button size="small" @click="permissionForm.tableSelections = []">清空</a-button>
          </div>
        </a-form-model-item>
        <a-form-model-item label="权限级别">
          <a-select v-model="permissionForm.permissionLevel" @change="applyPermissionLevel">
            <a-select-option v-for="item in permissionLevelOptions" :key="item.code" :value="item.code">
              {{ item.label }}（{{ item.permissions.join(' + ') }}）
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="权限">
          <a-select v-model="permissionForm.permissions" mode="multiple" placeholder="请选择权限">
            <a-select-option v-for="item in permissionOptions" :key="item" :value="item">
              {{ item }}
            </a-select-option>
          </a-select>
          <div class="quick-actions">
            <a-button size="small" @click="permissionForm.permissions = permissionOptions.slice()">全选权限</a-button>
            <a-button size="small" @click="permissionForm.permissions = []">清空</a-button>
          </div>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import axios from 'axios';

export default {
  name: 'RoleCatalogPanel',
  props: {
    roles: {
      type: Array,
      default: () => []
    },
    subjectBoundRoles: {
      type: Array,
      default: () => []
    },
    selectedRoleCode: {
      type: String,
      default: ''
    },
    roleDetail: {
      type: Object,
      default: null
    },
    capability: {
      type: Object,
      default: null
    },
    selectedCluster: {
      type: String,
      default: ''
    },
    selectedAuthBackend: {
      type: String,
      default: ''
    },
    loadingCatalog: {
      type: Boolean,
      default: false
    },
    loadingDetail: {
      type: Boolean,
      default: false
    },
    allowManageRoles: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      keyword: '',
      roleModalVisible: false,
      permissionModalVisible: false,
      roleForm: this.emptyRoleForm(),
      permissionForm: this.emptyPermissionForm(),
      databases: [],
      tableOptions: [],
      resourceLoading: {
        databases: false,
        tables: false
      }
    };
  },
  computed: {
    filteredRoles() {
      const keyword = String(this.keyword || '').trim().toLowerCase();
      if (!keyword) return this.roles || [];
      return (this.roles || []).filter(item => {
        const role = item.role || {};
        return String(role.roleCode || '').toLowerCase().includes(keyword)
          || String(role.roleName || '').toLowerCase().includes(keyword)
          || String(role.authBackend || '').toLowerCase().includes(keyword);
      });
    },
    subjectBoundRoleMetaMap() {
      return (this.subjectBoundRoles || []).reduce((result, item) => {
        const roleCode = String(item?.role?.roleCode || '').trim();
        if (!roleCode) return result;
        result[roleCode] = {
          bindingMode: item.bindingMode || 'DIRECT',
          bindingLabel: item.bindingLabel || '已绑定',
          matchedGroupName: item.matchedGroupName || ''
        };
        return result;
      }, {});
    },
    cardTitle() {
      return this.allowManageRoles ? '角色库' : '步骤 2：选择角色';
    },
    selectedRole() {
      return this.roleDetail && this.roleDetail.role ? this.roleDetail.role : null;
    },
    rolePermissionCount() {
      return Array.isArray(this.roleDetail?.permissions) ? this.roleDetail.permissions.length : 0;
    },
    assignmentCount() {
      return Array.isArray(this.roleDetail?.assignments) ? this.roleDetail.assignments.length : 0;
    },
    catalogHint() {
      if (this.allowManageRoles) {
        return '选择角色后，在右侧查看角色信息、权限范围与绑定对象。';
      }
      return '选择角色后，在右侧先绑定角色，再从角色范围内勾选需要下发或回收的权限，最后校验结果。';
    },
    capabilityAuthBackend() {
      return this.capability && this.capability.authBackend ? this.capability.authBackend : '';
    },
    capabilityEngineType() {
      return this.capability && this.capability.engineType ? this.capability.engineType : '';
    },
    canSyncBackendRoles() {
      return String(this.selectedAuthBackend || this.capabilityAuthBackend || '').trim().toUpperCase() === 'SENTRY';
    },
    isStarRocksBackend() {
      const text = [
        this.selectedAuthBackend,
        this.capabilityAuthBackend,
        this.capabilityEngineType,
        this.roleDetail?.role?.authBackend,
        this.roleDetail?.role?.engineType
      ].filter(Boolean).join(' ').toUpperCase();
      return text.includes('STARROCKS') || text.includes('STAR_ROCKS');
    },
    allDatabaseButtonText() {
      return this.shouldSelectConcreteDatabases ? '全选当前库' : '全部库 (*)';
    },
    shouldSelectConcreteDatabases() {
      return this.isStarRocksBackend || this.permissionForm.resourceType === 'TABLE';
    },
    permissionOptions() {
      return Array.isArray(this.capability?.permissions) && this.capability.permissions.length
        ? this.capability.permissions
        : ['SELECT'];
    },
    permissionLevelOptions() {
      if (Array.isArray(this.capability?.permissionLevels) && this.capability.permissionLevels.length) {
        return this.capability.permissionLevels.map(item => ({
          code: item.code,
          label: item.label || item.code,
          riskLevel: item.riskLevel || 'LOW',
          permissions: Array.isArray(item.permissions) ? item.permissions : []
        }));
      }
      return [
        { code: 'READONLY', label: '只读', riskLevel: 'LOW', permissions: ['SELECT'] },
        { code: 'WRITE', label: '读写', riskLevel: 'MEDIUM', permissions: ['SELECT', 'INSERT'] },
        { code: 'DDL', label: '结构变更', riskLevel: 'HIGH', permissions: ['CREATE', 'ALTER', 'DROP'] },
        { code: 'ADMIN', label: '管理员', riskLevel: 'HIGH', permissions: ['ALL'] }
      ];
    },
    selectedPermissionLevel() {
      return this.permissionLevelOptions.find(item => item.code === this.roleForm.level) || this.permissionLevelOptions[0];
    },
    generatedRoleCode() {
      return this.buildRoleCode(this.selectedCluster, this.capabilityAuthBackend || this.selectedAuthBackend, this.roleForm.domain, this.roleForm.scope, this.roleForm.level);
    },
    databaseSelectOptions() {
      const options = (this.databases || [])
        .filter(item => String(item || '').trim() && String(item || '').trim() !== '*')
        .map(item => ({ value: item, label: item }));
      if (this.permissionForm.resourceType === 'DATABASE' && !this.isStarRocksBackend) {
        return [{ value: '*', label: '*（全部库）' }, ...options];
      }
      return options;
    }
  },
  methods: {
    emptyRoleForm() {
      return {
        id: null,
        roleCode: '',
        roleName: '',
        owner: '',
        domain: '',
        scope: '',
        level: 'READONLY',
        manualRoleCode: false,
        riskLevel: 'LOW',
        status: 'ACTIVE',
        expiresAt: '',
        description: ''
      };
    },
    emptyPermissionForm() {
      return {
        resourceType: 'DATABASE',
        databaseNames: [],
        tableSelections: [],
        permissionLevel: 'READONLY',
        permissions: []
      };
    },
    openRoleModal(role) {
      if (role) {
        this.roleForm = {
          id: role.id,
          roleCode: role.roleCode || '',
          roleName: role.roleName || '',
          owner: role.owner || '',
          domain: role.domain || '',
          scope: role.scope || '',
          level: role.level || 'READONLY',
          manualRoleCode: true,
          riskLevel: role.riskLevel || 'LOW',
          status: role.status || 'ACTIVE',
          expiresAt: this.formatDateTimeInput(role.expiresAt),
          description: role.description || ''
        };
      } else {
        this.roleForm = this.emptyRoleForm();
        this.onRoleLevelChange(this.roleForm.level);
        this.syncGeneratedRoleFields();
      }
      this.roleModalVisible = true;
    },
    submitRole() {
      const roleCode = this.roleForm.manualRoleCode || this.roleForm.id
        ? this.roleForm.roleCode.trim()
        : this.generatedRoleCode;
      if (!roleCode) {
        this.$message.warning('请填写业务域、资源范围和权限级别以生成角色编码');
        return;
      }
      const { manualRoleCode, ...rolePayload } = this.roleForm;
      this.$emit('save-role', {
        ...rolePayload,
        roleCode,
        roleName: this.roleForm.roleName || this.generatedRoleName(roleCode),
        domain: this.roleForm.domain.trim(),
        scope: this.roleForm.scope.trim(),
        level: this.roleForm.level,
        cluster: this.roleDetail?.role?.cluster || this.selectedCluster,
        engineType: this.capabilityEngineType,
        authBackend: this.capabilityAuthBackend || this.selectedAuthBackend,
        expiresAt: this.normalizeDateTimeInput(this.roleForm.expiresAt)
      });
      this.roleModalVisible = false;
    },
    onRoleLevelChange() {
      const selected = this.selectedPermissionLevel;
      if (selected && selected.riskLevel) {
        this.roleForm.riskLevel = selected.riskLevel;
      }
      this.syncGeneratedRoleFields();
    },
    syncGeneratedRoleFields() {
      if (!this.roleForm.id && !this.roleForm.manualRoleCode) {
        this.roleForm.roleCode = this.generatedRoleCode;
      }
      if (!this.roleForm.roleName && this.roleForm.scope) {
        this.roleForm.roleName = this.generatedRoleName(this.generatedRoleCode);
      }
    },
    generatedRoleName(roleCode) {
      const level = this.selectedPermissionLevel;
      const scope = this.roleForm.scope || roleCode;
      return `${scope} ${level ? level.label : this.roleForm.level}`;
    },
    buildRoleCode(cluster, authBackend, domain, scope, level) {
      const parts = [
        'dga',
        this.normalizeCodePart(cluster),
        this.normalizeCodePart(authBackend),
        this.normalizeCodePart(domain),
        this.normalizeCodePart(scope),
        this.normalizeCodePart(level)
      ].filter(Boolean);
      return parts.join('_').replace(/_+/g, '_').replace(/^_|_$/g, '');
    },
    normalizeCodePart(value) {
      return String(value || '')
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_|_$/g, '');
    },
    async openPermissionModal() {
      if (!this.roleDetail || !this.roleDetail.role) return;
      this.permissionForm = this.emptyPermissionForm();
      this.applyPermissionLevel(this.permissionForm.permissionLevel);
      this.permissionModalVisible = true;
      await this.loadDatabases();
    },
    applyPermissionLevel(value) {
      const selected = this.permissionLevelOptions.find(item => item.code === value) || this.permissionLevelOptions[0];
      this.permissionForm.permissionLevel = selected ? selected.code : 'READONLY';
      const allowed = new Set(this.permissionOptions);
      this.permissionForm.permissions = selected && selected.permissions.length
        ? selected.permissions.filter(item => allowed.has(item))
        : (this.permissionOptions.length ? [this.permissionOptions[0]] : []);
    },
    onPermissionResourceTypeChange() {
      if (this.permissionForm.resourceType === 'TABLE' && this.permissionForm.databaseNames.includes('*')) {
        this.permissionForm.databaseNames = [];
      }
      this.permissionForm.tableSelections = [];
      if (this.permissionForm.resourceType === 'TABLE' && this.permissionForm.databaseNames.length) {
        this.onPermissionDatabasesChange(this.permissionForm.databaseNames);
      }
    },
    async onPermissionDatabasesChange(values) {
      const nextValues = values || [];
      if (this.isStarRocksBackend && nextValues.includes('*')) {
        this.permissionForm.databaseNames = nextValues.filter(item => item !== '*');
        this.$message.warning('StarRocks 角色权限范围请指定具体数据库');
      } else {
        this.permissionForm.databaseNames = nextValues.includes('*') ? ['*'] : nextValues.filter(item => item !== '*');
      }
      this.permissionForm.tableSelections = [];
      if (this.permissionForm.resourceType !== 'TABLE' || !this.permissionForm.databaseNames.length) {
        this.tableOptions = [];
        return;
      }
      await this.loadTablesForDatabases(this.permissionForm.databaseNames);
    },
    async selectAllDatabases() {
      this.permissionForm.databaseNames = this.shouldSelectConcreteDatabases
        ? (this.databases || []).filter(item => String(item || '').trim() && String(item || '').trim() !== '*')
        : ['*'];
      this.permissionForm.tableSelections = [];
      this.tableOptions = [];
      if (this.permissionForm.resourceType === 'TABLE' && this.permissionForm.databaseNames.length) {
        await this.loadTablesForDatabases(this.permissionForm.databaseNames);
      }
    },
    async loadDatabases() {
      if (!this.roleDetail?.role?.cluster) return;
      this.resourceLoading.databases = true;
      try {
        const { data } = await axios.get('/api/access/resources/databases', {
          params: {
            cluster: this.roleDetail.role.cluster,
            authBackend: this.capabilityAuthBackend || this.selectedAuthBackend
          }
        });
        this.databases = Array.isArray(data) ? data : [];
      } catch (e) {
        this.databases = [];
        this.$message.error(e.response?.data?.message || '加载数据库失败');
      } finally {
        this.resourceLoading.databases = false;
      }
    },
    async loadTablesForDatabases(databases) {
      this.resourceLoading.tables = true;
      try {
        const requests = databases.map(database => axios.get('/api/access/resources/tables', {
          params: {
            cluster: this.roleDetail.role.cluster,
            authBackend: this.capabilityAuthBackend || this.selectedAuthBackend,
            database
          }
        }).then(res => ({
          database,
          tables: Array.isArray(res.data) ? res.data : []
        })));
        const groups = await Promise.all(requests);
        this.tableOptions = groups.flatMap(group => group.tables.map(table => ({
          value: `${group.database}::${table}`,
          label: `${group.database}.${table}`,
          database: group.database,
          table
        })));
      } catch (e) {
        this.tableOptions = [];
        this.$message.error(e.response?.data?.message || '加载数据表失败');
      } finally {
        this.resourceLoading.tables = false;
      }
    },
    submitPermissions() {
      if (!this.roleDetail?.role?.roleCode) return;
      if (!this.permissionForm.databaseNames.length) {
        this.$message.warning('请选择数据库');
        return;
      }
      if (this.isStarRocksBackend && this.permissionForm.databaseNames.includes('*')) {
        this.$message.warning('StarRocks 角色权限范围请指定具体数据库，避免后续子集授权无法展开');
        return;
      }
      if (!this.permissionForm.permissions.length) {
        this.$message.warning('请选择权限');
        return;
      }
      let permissions = [];
      if (this.permissionForm.resourceType === 'DATABASE') {
        permissions = this.permissionForm.databaseNames.flatMap(databaseName =>
          this.permissionForm.permissions.map(permission => ({
            resourceType: 'DATABASE',
            databaseName,
            tableName: null,
            permission
          }))
        );
      } else {
        if (!this.permissionForm.tableSelections.length) {
          this.$message.warning('请选择表');
          return;
        }
        permissions = this.permissionForm.tableSelections.flatMap(value => {
          const option = this.tableOptions.find(item => item.value === value);
          if (!option) return [];
          return this.permissionForm.permissions.map(permission => ({
            resourceType: 'TABLE',
            databaseName: option.database,
            tableName: option.table,
            permission
          }));
        });
      }
      this.$emit('add-role-permissions', {
        roleCode: this.roleDetail.role.roleCode,
        permissions
      });
      this.permissionModalVisible = false;
    },
    formatDateTimeInput(value) {
      if (!value) return '';
      return String(value).slice(0, 16);
    },
    normalizeDateTimeInput(value) {
      if (!value) return null;
      return String(value).length === 16 ? `${value}:00` : value;
    }
  }
};
</script>

<style scoped>
.role-card {
  overflow: hidden;
  border-radius: 14px;
}
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f7;
}
.toolbar .ant-input-search {
  flex: 1;
}
.selected-role-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #e7edf5;
  border-radius: 10px;
  background: #fbfdff;
}
.selected-role-label,
.selected-role-code {
  margin-top: 4px;
  font-size: 12px;
  color: #667085;
}
.generated-role-code {
  min-height: 32px;
  padding: 5px 11px;
  color: #344054;
  background: #f8fafc;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  word-break: break-all;
}
.manual-code-toggle {
  margin-top: 8px;
}
.selected-role-label {
  margin-top: 0;
}
.selected-role-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
.role-list {
  display: flex;
  gap: 12px;
  min-height: 142px;
  padding: 2px 4px 12px 2px;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-snap-type: x proximity;
  -webkit-overflow-scrolling: touch;
}
.role-list::-webkit-scrollbar {
  height: 8px;
}
.role-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #d8e2ef;
}
.role-list::-webkit-scrollbar-track {
  border-radius: 999px;
  background: #f3f6fa;
}
.role-item {
  flex: 0 0 260px;
  min-width: 260px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  padding: 13px;
  text-align: left;
  cursor: pointer;
  scroll-snap-align: start;
  transition: all 0.2s ease;
}
.role-item:hover,
.role-item.active {
  border-color: #1890ff;
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
  transform: translateY(-1px);
}
.role-item.active {
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
}
.role-item.is-bound {
  border-color: #b7eb8f;
  background: linear-gradient(180deg, #f6ffed 0%, #ffffff 100%);
}
.role-item.is-bound.active {
  border-color: #1890ff;
  background: linear-gradient(180deg, #eef7ff 0%, #ffffff 100%);
}
.role-item.is-inherited {
  border-style: dashed;
}
.role-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.role-header strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}
.role-code,
.role-meta,
.role-stats,
.catalog-hint,
.role-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
}
.role-code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-meta,
.role-stats {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.catalog-hint {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fbfdff;
  border: 1px solid #edf0f5;
}
@media (max-width: 1200px) {
  .role-item {
    flex-basis: 240px;
    min-width: 240px;
  }
}
@media (max-width: 768px) {
  .role-list {
    min-height: 0;
  }
  .role-item {
    flex-basis: 82vw;
    min-width: 220px;
  }
}
.quick-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.form-hint {
  margin-top: 8px;
}
</style>
