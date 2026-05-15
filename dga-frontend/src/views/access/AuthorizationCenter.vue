<template>
  <div class="authorization-center-v2">
    <div class="page-heading">
      <div>
        <div class="page-eyebrow">{{ isRoleManagement ? 'RBAC ROLE MANAGEMENT' : 'RBAC AUTHORIZATION CENTER' }}</div>
        <h2>{{ isRoleManagement ? '角色管理' : '授权中心' }}</h2>
        <p>{{ isRoleManagement ? '维护角色、权限范围与当前绑定对象；用户授权与角色绑定请在授权中心执行。' : '围绕用户或组执行角色绑定、权限下发、回收与后端校验。' }}</p>
      </div>
      <a-button
        v-if="isRoleManagement"
        type="primary"
        icon="safety-certificate"
        @click="$router.push('/authorization-center')"
      >
        去授权中心
      </a-button>
      <a-button
        v-else
        icon="profile"
        @click="$router.push('/role-management')"
      >
        角色管理
      </a-button>
    </div>

    <authorization-workbench-filters
      class="context-panel"
      :clusters="clusters"
      :backend-options="backendOptions"
      :capability="capability"
      :subject-types="subjectTypes"
      :principals="principals"
      :verification-principals="verificationPrincipals"
      :selected-principal="selectedPrincipal"
      :verification-principal="verificationPrincipal"
      :role-default-group-name="selectedRoleGroupName"
      :mode="workbenchMode"
      :state="state"
      :loading="filterLoading"
      @change="handleStateChange"
      @refresh="reloadAll"
      @sync-verification="syncVerificationUser"
    />

    <div v-if="!isRoleManagement && state.mode === 'ROLE'" class="flow-role-panel">
      <div class="flow-strip">
        <div
          v-for="step in authorizationFlowSteps"
          :key="step.key"
          class="flow-step"
          :class="`is-${step.status}`"
        >
          <div class="flow-step-index">{{ step.order }}</div>
          <div class="flow-step-body">
            <div class="flow-step-title">{{ step.title }}</div>
            <div class="flow-step-hint">{{ step.hint }}</div>
          </div>
          <a-tag :color="step.tagColor">{{ step.statusLabel }}</a-tag>
        </div>
      </div>

      <div class="role-catalog-strip-panel">
        <role-catalog-panel
          ref="roleCatalogPanel"
          :roles="roles"
          :subject-bound-roles="subjectContext.roles"
          :selected-role-code="selectedRoleCode"
          :role-detail="selectedRoleView"
          :capability="capability"
          :selected-cluster="state.selectedCluster"
          :selected-auth-backend="state.selectedAuthBackend"
          :loading-catalog="loading.roles"
          :loading-detail="loading.roleDetail"
          :allow-manage-roles="isRoleManagement"
          @select-role="selectRole"
          @save-role="saveRole"
          @delete-role="deleteRole"
          @add-role-permissions="addRolePermissions"
          @delete-role-permission="deleteRolePermission"
        />
      </div>
    </div>

    <a-row :gutter="20" class="workspace-row">
      <a-col v-if="isRoleManagement || state.mode !== 'ROLE'" :xs="24" :lg="8" :xl="7" class="workspace-col role-catalog-col">
        <role-catalog-panel
          ref="roleCatalogPanel"
          :roles="roles"
          :subject-bound-roles="subjectContext.roles"
          :selected-role-code="selectedRoleCode"
          :role-detail="selectedRoleView"
          :capability="capability"
          :selected-cluster="state.selectedCluster"
          :selected-auth-backend="state.selectedAuthBackend"
          :loading-catalog="loading.roles"
          :loading-detail="loading.roleDetail"
          :allow-manage-roles="isRoleManagement"
          @select-role="selectRole"
          @save-role="saveRole"
          @delete-role="deleteRole"
          @add-role-permissions="addRolePermissions"
          @delete-role-permission="deleteRolePermission"
        />
      </a-col>

      <a-col
        :xs="24"
        :lg="isRoleManagement || state.mode !== 'ROLE' ? 16 : 24"
        :xl="isRoleManagement || state.mode !== 'ROLE' ? 17 : 24"
        class="workspace-col workbench-col"
      >
        <role-grant-workbench
          :capability="capability"
          :selected-role-code="selectedRoleCode"
          :selected-role-view="selectedRoleView"
          :selected-principal="selectedPrincipal"
          :verification-principal="verificationPrincipal"
          :verification-user="state.verificationUser"
          :verification-snapshot="verificationSnapshot"
          :subject-context="subjectContext"
          :databases="databases"
          :tables="tables"
          :active-tab="activeRoleTab"
          :state="state"
          :loading="workbenchLoading"
          :batch-result="batchResult"
          :mode="workbenchMode"
          @change="handleStateChange"
          @change-tab="activeRoleTab = $event"
          @edit-role="openRoleEditor"
          @delete-role="deleteSelectedRole"
          @delete-role-permission="deleteRolePermission"
          @open-permission-modal="openPermissionModal"
          @assign-role="assignRole"
          @revoke-role="revokeRole"
          @grant-subset="grantSubset"
          @revoke-subset="revokeSubset"
          @grant-direct="grantDirect"
          @revoke-direct="revokeDirect"
          @dry-run-batch="dryRunBatch"
          @assign-batch="assignBatch"
          @select-all-role-permissions="selectAllRolePermissions"
          @refresh-verification="loadVerificationIfNeeded"
          @sync-verification="syncVerificationUser"
          @use-subject-as-verification="useSubjectAsVerificationUser"
        />
      </a-col>
    </a-row>
  </div>
</template>

<script>
import axios from 'axios';
import AuthorizationWorkbenchFilters from './components/AuthorizationWorkbenchFilters.vue';
import RoleCatalogPanel from './components/RoleCatalogPanel.vue';
import RoleGrantWorkbench from './components/RoleGrantWorkbench.vue';
import {
  allowedSubjectTypes,
  boundRoleMeta,
  defaultSubjectType,
  hasUsableRoleAssignment,
  operationModeLabel,
  parseBatchUsers,
  permissionKey,
  rolePermissionSelection,
  supportsDirectGrant,
  supportsRoles,
  uniqueRolePermissions,
  usableAssignmentStatus,
  verificationUserRequired
} from './authorizationCenterHelpers';

export default {
  name: 'AuthorizationCenter',
  components: {
    AuthorizationWorkbenchFilters,
    RoleCatalogPanel,
    RoleGrantWorkbench
  },
  props: {
    pageMode: {
      type: String,
      default: 'authorization'
    }
  },
  data() {
    return {
      loading: {
        page: false,
        clusters: false,
        backends: false,
        roles: false,
        roleDetail: false,
        principals: false,
        verificationPrincipals: false,
        databases: false,
        tables: false,
        verification: false,
        submitting: false,
        dryRunning: false
      },
      clusters: [],
      backendOptions: [],
      capability: null,
      roles: [],
      principals: [],
      verificationPrincipals: [],
      databases: [],
      tables: [],
      selectedRoleCode: '',
      selectedRoleView: null,
      activeRoleTab: this.pageMode === 'role-management' ? 'info' : 'actions',
      verificationSnapshot: null,
      subjectContext: {
        loading: false,
        roles: [],
        ldapProfile: null
      },
      batchResult: null,
      routeContext: {
        username: '',
        cluster: ''
      },
      state: {
        selectedCluster: '',
        selectedAuthBackend: '',
        subjectType: 'USER',
        subjectName: '',
        verificationUser: '',
        mode: 'ROLE',
        scopeLevel: 'DATABASE',
        directDatabases: [],
        databaseName: '',
        tableNames: [],
        permissions: ['SELECT'],
        selectedRolePermissionKeys: [],
        batchUsers: '',
        exceptionReason: '',
        ticketNo: '',
        approver: '',
        expiresAt: '',
        riskLevel: 'LOW'
      }
    };
  },
  computed: {
    isRoleManagement() {
      return this.pageMode === 'role-management';
    },
    workbenchMode() {
      return this.isRoleManagement ? 'role-management' : 'authorization';
    },
    subjectTypes() {
      return this.capability ? allowedSubjectTypes(this.capability) : ['USER'];
    },
    filterLoading() {
      return {
        clusters: this.loading.clusters,
        backends: this.loading.backends,
        principals: this.loading.principals || this.loading.verificationPrincipals,
        databases: this.loading.databases,
        tables: this.loading.tables,
        verification: this.loading.verification
      };
    },
    selectedPrincipal() {
      return this.findPrincipalOption(this.principals, this.state.subjectName);
    },
    verificationPrincipal() {
      return this.findPrincipalOption(this.verificationPrincipals, this.state.verificationUser);
    },
    selectedRoleLabel() {
      if (this.selectedRoleView?.role?.roleName) return this.selectedRoleView.role.roleName;
      if (this.selectedRoleView?.role?.roleCode) return this.selectedRoleView.role.roleCode;
      return '未选择角色';
    },
    selectedClusterLabel() {
      const selected = (this.clusters || []).find(item => (item.clusterCode || item.clusterName) === this.state.selectedCluster);
      return selected ? (selected.clusterName || selected.clusterCode) : (this.state.selectedCluster || '未选择');
    },
    selectedRoleGroupName() {
      const assignments = Array.isArray(this.selectedRoleView?.assignments) ? this.selectedRoleView.assignments : [];
      const normalizedAuthBackend = String(this.state.selectedAuthBackend || '').trim().toUpperCase();
      const matched = assignments.find(item => {
        const assignmentAuthBackend = String(item?.authBackend || '').trim().toUpperCase();
        return String(item?.subjectType || '').toUpperCase() === 'GROUP'
          && (!normalizedAuthBackend || !assignmentAuthBackend || assignmentAuthBackend === normalizedAuthBackend)
          && usableAssignmentStatus(item?.backendSyncStatus)
          && String(item?.subjectName || '').trim();
      });
      if (matched?.subjectName) return String(matched.subjectName).trim();
      const role = this.selectedRoleView?.role || {};
      return this.firstNonBlank(role.sentryGroup, role.ldapGroup, role.groupName, role.defaultGroupName);
    },
    workbenchLoading() {
      return {
        submitting: this.loading.submitting,
        dryRunning: this.loading.dryRunning,
        roleDetail: this.loading.roleDetail,
        verification: this.loading.verification,
        databases: this.loading.databases,
        tables: this.loading.tables
      };
    },
    hasSelectedContext() {
      return Boolean(this.state.selectedCluster && this.state.selectedAuthBackend);
    },
    hasSelectedSubject() {
      return Boolean(this.state.subjectName);
    },
    hasSelectedRole() {
      return Boolean(this.selectedRoleCode && this.selectedRoleView?.role);
    },
    hasCurrentRoleBinding() {
      return Boolean(
        this.selectedRoleView
        && this.state.subjectName
        && hasUsableRoleAssignment(this.selectedRoleView, this.state.subjectType, this.state.subjectName, this.state.selectedAuthBackend)
      );
    },
    hasSelectedSubset() {
      return Array.isArray(this.state.selectedRolePermissionKeys) && this.state.selectedRolePermissionKeys.length > 0;
    },
    canVerifyFlow() {
      return Boolean(this.state.verificationUser && this.verificationSnapshot);
    },
    authorizationFlowSteps() {
      return [
        {
          key: 'subject',
          order: 1,
          title: '选择授权对象',
          hint: this.hasSelectedSubject ? `当前对象：${this.state.subjectName}` : '先选用户或组主体',
          ...this.flowStepMeta(this.hasSelectedContext && this.hasSelectedSubject, this.hasSelectedContext && !this.hasSelectedSubject)
        },
        {
          key: 'role',
          order: 2,
          title: '选择角色',
          hint: this.hasSelectedRole ? `当前角色：${this.selectedRoleLabel}` : '从左侧角色库选择角色',
          ...this.flowStepMeta(this.hasSelectedRole, this.hasSelectedSubject && !this.hasSelectedRole)
        },
        {
          key: 'binding',
          order: 3,
          title: '绑定角色',
          hint: this.hasCurrentRoleBinding ? '当前对象已完成角色绑定' : '绑定后才可执行角色内授权',
          ...this.flowStepMeta(this.hasCurrentRoleBinding, this.hasSelectedRole && !this.hasCurrentRoleBinding)
        },
        {
          key: 'grant',
          order: 4,
          title: '选择并执行授权',
          hint: this.hasSelectedSubset ? `已选 ${this.state.selectedRolePermissionKeys.length} 项权限子集` : '从角色范围内勾选权限子集',
          ...this.flowStepMeta(this.hasCurrentRoleBinding && this.hasSelectedSubset, this.hasCurrentRoleBinding && !this.hasSelectedSubset)
        },
        {
          key: 'verify',
          order: 5,
          title: '校验结果',
          hint: this.canVerifyFlow ? '可在下方直接比对 live 与 recorded' : '执行后使用校验用户复核结果',
          ...this.flowStepMeta(this.canVerifyFlow, Boolean(this.state.verificationUser) && !this.verificationSnapshot)
        }
      ];
    },
    capabilityReady() {
      return Boolean(this.capability && this.capability.status === 'READY');
    }
  },
  created() {
    this.applyRouteContext();
    this.reloadAll();
  },
  watch: {
    pageMode(value) {
      this.activeRoleTab = value === 'role-management' ? 'info' : 'actions';
    }
  },
  methods: {
    operationModeLabel,
    flowStepMeta(done, current) {
      if (done) {
        return { status: 'done', statusLabel: '已就绪', tagColor: 'green' };
      }
      if (current) {
        return { status: 'current', statusLabel: '进行中', tagColor: 'blue' };
      }
      return { status: 'todo', statusLabel: '待完成', tagColor: 'default' };
    },
    openRoleEditor() {
      if (!this.isRoleManagement) return;
      if (!this.selectedRoleView?.role || !this.$refs.roleCatalogPanel) return;
      this.$refs.roleCatalogPanel.openRoleModal(this.selectedRoleView.role);
      this.activeRoleTab = 'info';
    },
    openPermissionModal() {
      if (!this.isRoleManagement) return;
      if (!this.selectedRoleView?.role || !this.$refs.roleCatalogPanel) return;
      this.$refs.roleCatalogPanel.openPermissionModal();
      this.activeRoleTab = 'scope';
    },
    deleteSelectedRole() {
      if (!this.isRoleManagement) return;
      if (!this.selectedRoleCode) return;
      this.deleteRole(this.selectedRoleCode);
    },
    async useSubjectAsVerificationUser() {
      if (!this.state.subjectName) return;
      this.state.verificationUser = this.state.subjectName;
      await this.loadVerificationIfNeeded();
    },
    applyRouteContext() {
      const query = this.$route && this.$route.query ? this.$route.query : {};
      this.routeContext.username = String(query.username || '').trim();
      this.routeContext.cluster = String(query.cluster || '').trim();
      if (this.routeContext.cluster) {
        this.state.selectedCluster = this.routeContext.cluster;
      }
    },
    async reloadAll() {
      this.loading.page = true;
      try {
        await this.loadClusters();
      } finally {
        this.loading.page = false;
      }
    },
    async loadClusters() {
      this.loading.clusters = true;
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = (res.data || []).filter(item => item.status !== 'DELETED');
        if (!this.state.selectedCluster && this.clusters.length) {
          this.state.selectedCluster = this.clusters[0].clusterCode || this.clusters[0].clusterName;
        }
        if (this.state.selectedCluster) {
          await this.onClusterChanged();
        }
      } catch (e) {
        this.$message.error(this.messageOf(e, '加载集群失败'));
      } finally {
        this.loading.clusters = false;
      }
    },
    async handleStateChange({ field, value }) {
      const previousSubjectName = this.state.subjectName;
      if (field === 'selectedCluster') {
        this.state.selectedCluster = value || '';
        await this.onClusterChanged();
        return;
      }
      if (field === 'selectedAuthBackend') {
        this.state.selectedAuthBackend = value || '';
        await this.onBackendChanged();
        return;
      }
      if (field === 'subjectType') {
        this.state.subjectType = value || 'USER';
        this.state.subjectName = '';
        this.state.verificationUser = verificationUserRequired(this.state.subjectType)
          ? ''
          : this.state.verificationUser;
        this.verificationSnapshot = null;
        this.subjectContext = { loading: false, roles: [], ldapProfile: null };
        await this.loadPrincipals();
        return;
      }
      if (field === 'subjectName') {
        const nextSubjectName = value || '';
        const redirected = await this.tryRedirectUserToLdapGroupSubject(nextSubjectName, previousSubjectName);
        if (redirected) return;
        this.state.subjectName = nextSubjectName;
        if (this.state.subjectType === 'USER' && (!this.state.verificationUser || this.state.verificationUser === previousSubjectName)) {
          this.state.verificationUser = this.state.subjectName;
        }
        await Promise.all([
          this.loadSubjectContext(),
          this.loadVerificationIfNeeded()
        ]);
        return;
      }
      if (field === 'verificationUser') {
        this.state.verificationUser = value || '';
        await Promise.all([
          this.loadSubjectContext(),
          this.loadVerificationIfNeeded()
        ]);
        return;
      }
      if (field === 'scopeLevel') {
        this.state.scopeLevel = value || 'DATABASE';
        this.state.directDatabases = [];
        this.state.databaseName = '';
        this.state.tableNames = [];
        this.tables = [];
        return;
      }
      if (field === 'databaseName') {
        this.state.databaseName = value || '';
        this.state.tableNames = [];
        if (this.state.databaseName) {
          await this.loadTables(this.state.databaseName);
        } else {
          this.tables = [];
        }
        return;
      }
      this.state[field] = value;
    },
    async tryRedirectUserToLdapGroupSubject(username, previousSubjectName) {
      const normalizedUsername = String(username || '').trim();
      if (!normalizedUsername || this.state.subjectType !== 'USER' || !this.shouldPreferGroupSubject()) {
        return false;
      }
      const userPrincipal = this.findPrincipalOption(this.principals, normalizedUsername)
        || this.findPrincipalOption(this.verificationPrincipals, normalizedUsername);
      const principalGroup = this.resolvePrincipalGroupName(userPrincipal);
      const ldapProfileGroup = principalGroup ? '' : await this.loadUserLdapPrimaryGroup(normalizedUsername);
      const fallbackRoleGroup = this.selectedRoleGroupName;
      const targetGroup = principalGroup || ldapProfileGroup || fallbackRoleGroup;
      if (!targetGroup) {
        return false;
      }
      this.state.verificationUser = normalizedUsername;
      this.state.subjectType = 'GROUP';
      this.state.subjectName = targetGroup;
      this.verificationSnapshot = null;
      await this.loadPrincipals();
      await Promise.all([
        this.loadSubjectContext(),
        this.loadVerificationIfNeeded()
      ]);
      const reason = principalGroup || ldapProfileGroup
        ? `已使用用户所属组 ${targetGroup} 作为授权对象，${normalizedUsername} 作为校验用户`
        : `未读取到该用户 LDAP 组，已临时使用角色绑定组 ${targetGroup}`;
      if (normalizedUsername !== previousSubjectName) {
        this.$message.info(reason);
      }
      return true;
    },
    async loadSubjectContext() {
      if (this.isRoleManagement) {
        this.subjectContext = { loading: false, roles: [], ldapProfile: null };
        await this.syncSelectedRoleForCurrentSubject();
        return;
      }
      const subjectName = String(this.state.subjectName || '').trim();
      const verificationUser = String(this.state.verificationUser || '').trim();
      if (!subjectName && !verificationUser) {
        this.subjectContext = { loading: false, roles: [], ldapProfile: null };
        await this.syncSelectedRoleForCurrentSubject();
        return;
      }
      this.subjectContext = {
        ...this.subjectContext,
        loading: true
      };
      const ldapUser = this.state.subjectType === 'USER' ? subjectName : verificationUser;
      const ldapProfile = ldapUser ? await this.loadUserLdapProfile(ldapUser) : null;
      this.subjectContext = {
        loading: false,
        roles: this.resolveSubjectBoundRoles(subjectName, ldapProfile),
        ldapProfile
      };
      await this.syncSelectedRoleForCurrentSubject();
    },
    resolveSubjectBoundRoles(subjectName, ldapProfile) {
      const subject = String(subjectName || '').trim();
      if (!subject) return [];
      const groupNames = this.profileGroupNames(ldapProfile);
      const currentSubjectType = String(this.state.subjectType || '').toUpperCase();
      const selectedBackend = String(this.state.selectedAuthBackend || '').trim().toUpperCase();
      const directRoles = [];
      const inheritedRoles = [];
      (this.roles || []).forEach(roleView => {
        const meta = boundRoleMeta(roleView, currentSubjectType, subject, selectedBackend, groupNames);
        if (!meta.isBound) return;
        const decoratedRoleView = {
          ...roleView,
          ...meta
        };
        if (meta.bindingMode === 'DIRECT') {
          directRoles.push(decoratedRoleView);
          return;
        }
        inheritedRoles.push(decoratedRoleView);
      });
      return [...directRoles, ...inheritedRoles];
    },
    preferredRoleCodeForCurrentSubject() {
      const boundRoles = Array.isArray(this.subjectContext?.roles) ? this.subjectContext.roles : [];
      if (!this.selectedRoleCode && boundRoles.length === 1) {
        return boundRoles[0]?.role?.roleCode || '';
      }
      return this.roles[0]?.role?.roleCode || '';
    },
    async syncSelectedRoleForCurrentSubject() {
      const roleCodes = (this.roles || []).map(item => item?.role?.roleCode).filter(Boolean);
      if (!roleCodes.length) {
        this.selectedRoleCode = '';
        this.selectedRoleView = null;
        this.state.selectedRolePermissionKeys = [];
        return;
      }
      if (this.selectedRoleCode && roleCodes.includes(this.selectedRoleCode)) {
        if (this.selectedRoleView?.role?.roleCode !== this.selectedRoleCode) {
          await this.loadRoleDetail(this.selectedRoleCode);
        }
        return;
      }
      const nextRoleCode = this.preferredRoleCodeForCurrentSubject();
      if (!nextRoleCode) {
        this.selectedRoleCode = '';
        this.selectedRoleView = null;
        this.state.selectedRolePermissionKeys = [];
        return;
      }
      await this.selectRole(nextRoleCode, { refreshSubjectContext: false });
    },
    profileGroupNames(profile) {
      if (!profile) return [];
      const names = [];
      const pushName = value => {
        if (value == null) return;
        const name = this.resolvePrincipalGroupName(value && typeof value === 'object' ? value : { groupName: value });
        if (name && !names.some(item => item.toLowerCase() === name.toLowerCase())) {
          names.push(name);
        }
      };
      pushName(profile.primaryGroup);
      pushName(profile.primaryGroupName);
      pushName(profile.ldapGroup);
      pushName(profile.groupName);
      if (Array.isArray(profile.supplementaryGroups)) {
        profile.supplementaryGroups.forEach(pushName);
      }
      return names;
    },
    async loadUserLdapPrimaryGroup(username) {
      const profile = await this.loadUserLdapProfile(username);
      return this.resolvePrincipalGroupName(profile || null);
    },
    async loadUserLdapProfile(username) {
      if (!username || !this.state.selectedCluster) return '';
      try {
        const res = await axios.get(`/api/access/user/${encodeURIComponent(username)}/ldap-profile`, {
          params: { cluster: this.state.selectedCluster }
        });
        return res.data || null;
      } catch (e) {
        return null;
      }
    },
    shouldPreferGroupSubject() {
      const subjectTypes = this.subjectTypes || [];
      if (!subjectTypes.includes('GROUP')) return false;
      if (!this.capability) return false;
      return Boolean(this.capability.requiresLdap)
        || /LDAP|SENTRY|HIVE/i.test(`${this.capability.endpointType || ''} ${this.capability.authBackend || ''} ${this.capability.engineType || ''}`);
    },
    resolvePrincipalGroupName(principal) {
      if (!principal) return '';
      const nestedPrimary = principal.primaryGroup && typeof principal.primaryGroup === 'object'
        ? this.firstNonBlank(principal.primaryGroup.name, principal.primaryGroup.cn)
        : '';
      const nestedLdap = principal.ldapGroup && typeof principal.ldapGroup === 'object'
        ? this.firstNonBlank(principal.ldapGroup.name, principal.ldapGroup.cn)
        : '';
      const firstSupplementary = Array.isArray(principal.supplementaryGroups) && principal.supplementaryGroups.length
        ? this.firstNonBlank(
            principal.supplementaryGroups[0]?.name,
            principal.supplementaryGroups[0]?.cn,
            typeof principal.supplementaryGroups[0] === 'string' ? principal.supplementaryGroups[0] : ''
          )
        : '';
      return this.firstNonBlank(
        principal.sentryGroup,
        principal.sentryGroupName,
        nestedLdap,
        typeof principal.ldapGroup === 'string' ? principal.ldapGroup : '',
        principal.groupName,
        principal.primaryGroupName,
        nestedPrimary,
        firstSupplementary,
        principal.defaultGroupName,
        principal.name,
        principal.cn,
        principal.value
      );
    },
    async onClusterChanged() {
      this.backendOptions = [];
      this.state.selectedAuthBackend = '';
      this.resetBackendScopedState();
      if (!this.state.selectedCluster) {
        return;
      }
      await this.loadBackendOptions();
      const preferred = this.pickPreferredBackend();
      if (!preferred) {
        return;
      }
      this.state.selectedAuthBackend = preferred;
      await this.onBackendChanged();
    },
    async onBackendChanged() {
      this.resetBackendScopedState();
      if (!this.state.selectedCluster || !this.state.selectedAuthBackend) {
        return;
      }
      await this.loadCapability();
      this.applyRouteSubjectType();
      this.applyRouteUserContext();
      const tasks = [
        this.loadRoles(),
        this.capabilityReady ? this.loadDatabases() : Promise.resolve()
      ];
      if (!this.isRoleManagement) {
        tasks.push(this.loadPrincipals(), this.loadVerificationPrincipals());
      }
      await Promise.all(tasks);
      await this.loadVerificationIfNeeded();
    },
    applyRouteUserContext() {
      if (!this.routeContext.username) return;
      this.applyRouteSubjectType();
      if (this.state.subjectType === 'USER') {
        this.state.subjectName = this.routeContext.username;
      }
      this.state.verificationUser = this.routeContext.username;
    },
    applyRouteSubjectType() {
      if (!this.routeContext.username || !this.capability) return;
      const availableSubjectTypes = allowedSubjectTypes(this.capability);
      if (availableSubjectTypes.includes('USER')) {
        this.state.subjectType = 'USER';
      }
    },
    resetBackendScopedState() {
      this.capability = null;
      this.roles = [];
      this.principals = [];
      this.verificationPrincipals = [];
      this.databases = [];
      this.tables = [];
      this.selectedRoleCode = '';
      this.selectedRoleView = null;
      this.activeRoleTab = 'info';
      this.verificationSnapshot = null;
      this.subjectContext = { loading: false, roles: [], ldapProfile: null };
      this.batchResult = null;
      this.state.subjectName = '';
      this.state.verificationUser = '';
      this.state.mode = 'ROLE';
      this.state.scopeLevel = 'DATABASE';
      this.state.directDatabases = [];
      this.state.databaseName = '';
      this.state.tableNames = [];
      this.state.selectedRolePermissionKeys = [];
      this.state.batchUsers = '';
      this.state.exceptionReason = '';
      this.state.ticketNo = '';
      this.state.approver = '';
      this.state.expiresAt = '';
      this.state.riskLevel = 'LOW';
    },
    async loadBackendOptions() {
      this.loading.backends = true;
      try {
        const res = await axios.get('/api/access/capabilities/backends', {
          params: { cluster: this.state.selectedCluster }
        });
        this.backendOptions = res.data || [];
      } catch (e) {
        this.backendOptions = [];
        this.$message.error(this.messageOf(e, '加载授权后端失败'));
      } finally {
        this.loading.backends = false;
      }
    },
    pickPreferredBackend() {
      const current = this.backendOptions.find(item => item.authBackend === this.state.selectedAuthBackend);
      if (current) return current.authBackend;
      if (this.isHdpCluster(this.state.selectedCluster)) {
        const rangerReady = this.backendOptions.find(item => this.isRangerBackend(item) && item.status === 'READY');
        if (rangerReady) return rangerReady.authBackend;
        const ranger = this.backendOptions.find(this.isRangerBackend);
        if (ranger) return ranger.authBackend;
      }
      const ready = this.backendOptions.find(item => item.status === 'READY');
      if (ready) return ready.authBackend;
      return this.backendOptions.length ? this.backendOptions[0].authBackend : '';
    },
    isHdpCluster(clusterIdentifier) {
      const selected = (this.clusters || []).find(item => (item.clusterCode || item.clusterName) === clusterIdentifier) || {};
      const text = [
        clusterIdentifier,
        selected.clusterCode,
        selected.clusterName,
        selected.clusterType,
        selected.type
      ].filter(Boolean).join(' ').toUpperCase();
      return /\bHDP\b/.test(text) || text.includes('HDP');
    },
    isRangerBackend(item) {
      const text = [
        item && item.authBackend,
        item && item.endpointType,
        item && item.engineType,
        item && item.name,
        item && item.label
      ].filter(Boolean).join(' ').toUpperCase();
      return text.includes('RANGER');
    },
    async loadCapability() {
      try {
        const res = await axios.get('/api/access/capabilities', {
          params: {
            cluster: this.state.selectedCluster,
            authBackend: this.state.selectedAuthBackend
          }
        });
        this.capability = res.data || null;
        this.applyCapabilityDefaults();
      } catch (e) {
        this.capability = null;
        this.$message.error(this.messageOf(e, '加载授权能力失败'));
      }
    },
    applyCapabilityDefaults() {
      const capability = this.capability;
      if (!capability) {
        return;
      }
      const availableSubjectTypes = allowedSubjectTypes(capability);
      if (!availableSubjectTypes.includes(this.state.subjectType)) {
        this.state.subjectType = defaultSubjectType(capability);
      }
      if (!Array.isArray(this.state.permissions) || !this.state.permissions.length) {
        this.state.permissions = Array.isArray(capability.permissions) && capability.permissions.length
          ? [capability.permissions[0]]
          : ['SELECT'];
      } else {
        const allowedPermissions = Array.isArray(capability.permissions) ? capability.permissions : [];
        const filtered = this.state.permissions.filter(item => allowedPermissions.includes(item));
        this.state.permissions = filtered.length ? filtered : (allowedPermissions.length ? [allowedPermissions[0]] : ['SELECT']);
      }
      if (!supportsRoles(capability) && supportsDirectGrant(capability)) {
        this.state.mode = 'DIRECT_EXCEPTION';
      } else if (supportsRoles(capability)) {
        this.state.mode = 'ROLE';
      }
    },
    async loadRoles() {
      this.loading.roles = true;
      try {
        const res = await axios.get('/api/access/roles', {
          params: {
            cluster: this.state.selectedCluster,
            authBackend: this.state.selectedAuthBackend
          }
        });
        this.roles = res.data || [];
      } catch (e) {
        this.roles = [];
        this.selectedRoleCode = '';
        this.selectedRoleView = null;
        this.state.selectedRolePermissionKeys = [];
        this.$message.error(this.messageOf(e, '加载角色目录失败'));
      } finally {
        this.loading.roles = false;
      }
      await this.loadSubjectContext();
    },
    async selectRole(roleCode, { refreshSubjectContext = true } = {}) {
      if (!roleCode) {
        this.selectedRoleCode = '';
        this.selectedRoleView = null;
        this.state.selectedRolePermissionKeys = [];
        return;
      }
      this.selectedRoleCode = roleCode;
      await this.loadRoleDetail(roleCode);
      if (refreshSubjectContext) {
        await this.loadSubjectContext();
      }
    },
    async loadRoleDetail(roleCode) {
      this.loading.roleDetail = true;
      try {
        const res = await axios.get(`/api/access/roles/${encodeURIComponent(roleCode)}/effective-permissions`);
        this.selectedRoleView = res.data || null;
        const keys = uniqueRolePermissions(this.selectedRoleView).map(permissionKey);
        const current = Array.isArray(this.state.selectedRolePermissionKeys) ? this.state.selectedRolePermissionKeys : [];
        const preserved = keys.filter(key => current.includes(key));
        this.state.selectedRolePermissionKeys = preserved.length ? preserved : keys;
      } catch (e) {
        this.selectedRoleView = null;
        this.state.selectedRolePermissionKeys = [];
        this.$message.error(this.messageOf(e, '加载角色详情失败'));
      } finally {
        this.loading.roleDetail = false;
      }
    },
    async fetchPrincipalOptions(subjectType) {
      const res = await axios.get('/api/access/resources/principals', {
        params: {
          cluster: this.state.selectedCluster,
          authBackend: this.state.selectedAuthBackend,
          subjectType
        }
      });
      return Array.isArray(res.data) ? res.data : [];
    },
    async loadPrincipals() {
      if (!this.state.selectedCluster || !this.state.selectedAuthBackend) {
        this.principals = [];
        return;
      }
      this.loading.principals = true;
      try {
        this.principals = await this.fetchPrincipalOptions(this.state.subjectType);
      } catch (e) {
        this.principals = [];
        this.$message.error(this.messageOf(e, '加载主体候选失败'));
      } finally {
        this.loading.principals = false;
      }
    },
    async loadVerificationPrincipals() {
      if (!this.state.selectedCluster || !this.state.selectedAuthBackend) {
        this.verificationPrincipals = [];
        return;
      }
      this.loading.verificationPrincipals = true;
      try {
        this.verificationPrincipals = await this.fetchPrincipalOptions('USER');
      } catch (e) {
        this.verificationPrincipals = [];
        this.$message.error(this.messageOf(e, '加载验证用户候选失败'));
      } finally {
        this.loading.verificationPrincipals = false;
      }
    },
    async loadDatabases() {
      this.loading.databases = true;
      try {
        const res = await axios.get('/api/access/resources/databases', {
          params: {
            cluster: this.state.selectedCluster,
            authBackend: this.state.selectedAuthBackend
          }
        });
        this.databases = res.data || [];
      } catch (e) {
        this.databases = [];
        this.$message.error(this.messageOf(e, '加载数据库失败'));
      } finally {
        this.loading.databases = false;
      }
    },
    async loadTables(database) {
      if (!database) {
        this.tables = [];
        return;
      }
      this.loading.tables = true;
      try {
        const res = await axios.get('/api/access/resources/tables', {
          params: {
            cluster: this.state.selectedCluster,
            authBackend: this.state.selectedAuthBackend,
            database
          }
        });
        this.tables = res.data || [];
      } catch (e) {
        this.tables = [];
        this.$message.error(this.messageOf(e, '加载数据表失败'));
      } finally {
        this.loading.tables = false;
      }
    },
    async loadVerificationIfNeeded() {
      if (this.isRoleManagement) {
        this.verificationSnapshot = null;
        return;
      }
      if (!this.state.selectedCluster || !this.state.selectedAuthBackend || !this.state.verificationUser) {
        this.verificationSnapshot = null;
        return;
      }
      this.loading.verification = true;
      try {
        const res = await axios.get('/api/access/resources/permissions', {
          params: {
            cluster: this.state.selectedCluster,
            authBackend: this.state.selectedAuthBackend,
            username: this.state.verificationUser
          }
        });
        this.verificationSnapshot = res.data || null;
      } catch (e) {
        this.verificationSnapshot = null;
        this.$message.error(this.messageOf(e, '加载验证用户权限失败'));
      } finally {
        this.loading.verification = false;
      }
    },
    async syncVerificationUser() {
      if (!this.state.selectedCluster || !this.state.verificationUser) {
        this.$message.warning('请先选择验证用户');
        return;
      }
      this.loading.verification = true;
      try {
        await axios.post(`/api/access/sync/${encodeURIComponent(this.state.verificationUser)}`, null, {
          params: { cluster: this.state.selectedCluster }
        });
        this.$message.success('已同步后端权限');
        await this.loadVerificationIfNeeded();
      } catch (e) {
        this.$message.error(this.messageOf(e, '同步后端权限失败'));
      } finally {
        this.loading.verification = false;
      }
    },
    selectedRolePermissions() {
      const selected = new Set(this.state.selectedRolePermissionKeys || []);
      return uniqueRolePermissions(this.selectedRoleView)
        .filter(item => selected.has(permissionKey(item)))
        .map(rolePermissionSelection);
    },
    async saveRole(payload) {
      if (!payload || !this.state.selectedCluster || !this.state.selectedAuthBackend) {
        this.$message.warning('请先选择集群和授权后端');
        return;
      }
      this.loading.submitting = true;
      try {
        const body = {
          ...payload,
          cluster: this.state.selectedCluster,
          authBackend: this.state.selectedAuthBackend,
          engineType: this.capability?.engineType || payload.engineType
        };
        let savedRoleView = null;
        if (payload.id && payload.roleCode) {
          const res = await axios.put(`/api/access/roles/${encodeURIComponent(payload.roleCode)}`, body);
          savedRoleView = res.data || null;
        } else {
          const res = await axios.post('/api/access/roles', body);
          savedRoleView = res.data || null;
        }
        this.$message.success(payload.id ? '角色已更新' : '角色已创建');
        await this.loadRoles();
        const savedRoleCode = savedRoleView?.role?.roleCode || this.findSavedRoleCode(body.roleCode);
        if (savedRoleCode) {
          await this.selectRole(savedRoleCode);
        }
      } catch (e) {
        this.$message.error(this.messageOf(e, '保存角色失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    findSavedRoleCode(inputRoleCode) {
      const normalizedInput = String(inputRoleCode || '').trim().toLowerCase();
      if (!normalizedInput) return '';
      const prefixedInput = normalizedInput.startsWith('dga_') ? normalizedInput : `dga_${normalizedInput}`;
      const matched = (this.roles || []).find(item => {
        const roleCode = String(item?.role?.roleCode || '').trim().toLowerCase();
        return roleCode === normalizedInput || roleCode === prefixedInput;
      });
      return matched?.role?.roleCode || '';
    },
    async deleteRole(roleCode) {
      if (!roleCode) return;
      this.loading.submitting = true;
      try {
        await axios.delete(`/api/access/roles/${encodeURIComponent(roleCode)}`);
        this.$message.success('角色已删除');
        this.selectedRoleCode = '';
        this.selectedRoleView = null;
        await this.loadRoles();
      } catch (e) {
        this.$message.error(this.messageOf(e, '删除角色失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async addRolePermissions({ roleCode, permissions }) {
      if (!roleCode || !Array.isArray(permissions) || !permissions.length) return;
      this.loading.submitting = true;
      try {
        for (const permission of permissions) {
          await axios.post(`/api/access/roles/${encodeURIComponent(roleCode)}/permissions`, permission);
        }
        this.$message.success(`已添加 ${permissions.length} 项权限范围`);
        await this.loadRoleDetail(roleCode);
      } catch (e) {
        this.$message.error(this.messageOf(e, '添加角色权限范围失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async deleteRolePermission({ roleCode, permissionId }) {
      if (!roleCode || !permissionId) return;
      this.loading.submitting = true;
      try {
        await axios.delete(`/api/access/roles/${encodeURIComponent(roleCode)}/permissions/${permissionId}`);
        this.$message.success('角色权限范围已删除');
        await this.loadRoleDetail(roleCode);
      } catch (e) {
        this.$message.error(this.messageOf(e, '删除角色权限范围失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async assignRole() {
      if (!this.selectedRoleCode || !this.state.subjectName) {
        return;
      }
      if (!this.ensureRangerPrincipalExists()) {
        return;
      }
      this.loading.submitting = true;
      try {
        await axios.post(`/api/access/roles/${encodeURIComponent(this.selectedRoleCode)}/assignments`, {
          subjectType: this.state.subjectType,
          subjectName: this.state.subjectName,
          authBackend: this.state.selectedAuthBackend
        });
        this.$message.success('角色绑定已提交');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '角色绑定失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async revokeRole() {
      if (!this.selectedRoleCode || !this.state.subjectName) {
        return;
      }
      this.loading.submitting = true;
      try {
        await axios.delete(`/api/access/roles/${encodeURIComponent(this.selectedRoleCode)}/assignments`, {
          params: {
            subjectType: this.state.subjectType,
            subjectName: this.state.subjectName,
            authBackend: this.state.selectedAuthBackend
          }
        });
        this.$message.success('角色绑定已回收');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '角色绑定回收失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    buildSubsetPayload() {
      return {
        username: this.state.subjectType === 'GROUP' ? this.state.verificationUser : this.state.subjectName,
        cluster: this.state.selectedCluster,
        authBackend: this.state.selectedAuthBackend,
        grantMode: 'ROLE',
        roleSubsetMode: true,
        roleCode: this.selectedRoleCode,
        subjectType: this.state.subjectType,
        subjectName: this.state.subjectName,
        rolePermissions: this.selectedRolePermissions()
      };
    },
    async grantSubset() {
      if (!this.ensureRangerPrincipalExists(true)) {
        return;
      }
      this.loading.submitting = true;
      try {
        await axios.post('/api/access/grants/batch', this.buildSubsetPayload());
        this.$message.success('角色权限子集已下发');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '角色权限子集授权失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async revokeSubset() {
      this.loading.submitting = true;
      try {
        await axios.post('/api/access/revokes/batch', this.buildSubsetPayload());
        this.$message.success('角色权限子集已回收');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '角色权限子集回收失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    buildDirectPayload() {
      const payload = {
        username: this.state.subjectName,
        cluster: this.state.selectedCluster,
        authBackend: this.state.selectedAuthBackend,
        grantMode: 'DIRECT_EXCEPTION',
        level: this.state.scopeLevel,
        permission: this.state.permissions[0],
        permissions: this.state.permissions,
        exceptionReason: this.state.exceptionReason,
        ticketNo: this.state.ticketNo,
        approver: this.state.approver,
        expiresAt: this.normalizeDateTimeInput(this.state.expiresAt),
        riskLevel: this.state.riskLevel
      };
      if (this.state.scopeLevel === 'DATABASE') {
        payload.databases = this.state.directDatabases;
      } else {
        payload.tables = (this.state.tableNames || []).map(table => ({
          database: this.state.databaseName,
          table
        }));
      }
      return payload;
    },
    async grantDirect() {
      if (!this.ensureRangerPrincipalExists(true)) {
        return;
      }
      this.loading.submitting = true;
      try {
        await axios.post('/api/access/grants/batch', this.buildDirectPayload());
        this.$message.success('直接例外权限已下发');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '直接例外授权失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async revokeDirect() {
      this.loading.submitting = true;
      try {
        await axios.post('/api/access/revokes/batch', this.buildDirectPayload());
        this.$message.success('直接例外权限已回收');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '直接例外回收失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    async dryRunBatch() {
      if (!this.selectedRoleCode) {
        return;
      }
      this.loading.dryRunning = true;
      try {
        const res = await axios.post(`/api/access/roles/${encodeURIComponent(this.selectedRoleCode)}/assignments/batch/dry-run`, {
          cluster: this.state.selectedCluster,
          authBackend: this.state.selectedAuthBackend,
          usernames: parseBatchUsers(this.state.batchUsers)
        });
        this.batchResult = res.data || null;
        this.$message.success('Dry-run 已完成');
      } catch (e) {
        this.$message.error(this.messageOf(e, '批量 dry-run 失败'));
      } finally {
        this.loading.dryRunning = false;
      }
    },
    async assignBatch() {
      if (!this.selectedRoleCode) {
        return;
      }
      this.loading.submitting = true;
      try {
        const res = await axios.post(`/api/access/roles/${encodeURIComponent(this.selectedRoleCode)}/assignments/batch`, {
          cluster: this.state.selectedCluster,
          authBackend: this.state.selectedAuthBackend,
          usernames: parseBatchUsers(this.state.batchUsers)
        });
        this.batchResult = res.data || null;
        this.$message.success('批量补绑定已处理');
        await this.afterMutation();
      } catch (e) {
        this.$message.error(this.messageOf(e, '批量补绑定失败'));
      } finally {
        this.loading.submitting = false;
      }
    },
    selectAllRolePermissions() {
      this.state.selectedRolePermissionKeys = uniqueRolePermissions(this.selectedRoleView).map(permissionKey);
    },
    async afterMutation() {
      if (this.state.selectedCluster && this.state.selectedAuthBackend) {
        await this.loadRoles();
      }
      await this.loadVerificationIfNeeded();
    },
    findPrincipalOption(options, name) {
      const target = String(name || '').trim().toLowerCase();
      if (!target) return null;
      return (options || []).find(item => String(item?.name || item?.value || '').trim().toLowerCase() === target) || null;
    },
    isRangerBackend() {
      return String(this.state.selectedAuthBackend || '').toUpperCase().includes('RANGER');
    },
    ensureRangerPrincipalExists(checkVerificationUser = false) {
      if (!this.isRangerBackend()) {
        return true;
      }
      const subjectName = String(this.state.subjectName || '').trim();
      const subjectType = String(this.state.subjectType || 'USER').toUpperCase();
      if (!subjectName || !this.findPrincipalOption(this.principals, subjectName)) {
        this.$message.warning(`${subjectType === 'GROUP' ? '授权组' : '授权用户'}不在 Ranger 中，请先完成 Ranger 用户/组同步后再授权。`);
        return false;
      }
      const verificationUser = String(this.state.verificationUser || '').trim();
      if (checkVerificationUser && verificationUser && !this.findPrincipalOption(this.verificationPrincipals, verificationUser)) {
        this.$message.warning('权限验证用户不在 Ranger 中，无法作为本次授权后的实时校验账号。');
        return false;
      }
      return true;
    },
    messageOf(error, fallback) {
      return error && error.response && error.response.data && error.response.data.message
        ? error.response.data.message
        : (error && error.message) || fallback;
    },
    normalizeDateTimeInput(value) {
      if (!value) return null;
      return String(value).length === 16 ? `${value}:00` : value;
    },
    firstNonBlank(...values) {
      for (const value of values) {
        if (value == null) continue;
        const text = String(value).trim();
        if (text) return text;
      }
      return '';
    }
  }
};
</script>

<style scoped>
.authorization-center-v2 {
  min-height: 100%;
  padding: 0 4px 24px;
  background: linear-gradient(180deg, #f7faff 0%, #ffffff 260px);
}
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px 18px;
  border: 1px solid #e7edf5;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.05);
}
.page-eyebrow {
  margin-bottom: 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #667085;
}
.page-heading h2 {
  margin: 0 0 6px;
  color: #1f2d3d;
}
.page-heading p {
  margin: 0;
  color: #667085;
}
.context-panel {
  margin-bottom: 12px;
}
.flow-role-panel {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.6fr);
  gap: 14px;
  align-items: stretch;
  margin-bottom: 16px;
}
.flow-strip {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 8px;
  height: 100%;
}
.flow-step {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid #e7edf5;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
}
.flow-step.is-current {
  border-color: #b2ddff;
  background: #f5faff;
}
.flow-step.is-done {
  border-color: #abefc6;
  background: #f6fef9;
}
.flow-step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: #eff4ff;
  color: #175cd3;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.flow-step.is-done .flow-step-index {
  background: #ecfdf3;
  color: #067647;
}
.flow-step-body {
  min-width: 0;
  flex: 1;
}
.flow-step-title {
  color: #1f2d3d;
  font-size: 13px;
  font-weight: 600;
}
.flow-step-hint {
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.context-strip {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid #e7edf5;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.06);
}
.context-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.context-label {
  margin-bottom: 4px;
  font-size: 12px;
  color: #8a94a6;
}
.context-title {
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.context-meta-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.context-meta {
  display: inline-flex;
  align-items: center;
  max-width: 260px;
  min-height: 28px;
  padding: 4px 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #f5f8fc;
  color: #667085;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-catalog-strip-panel,
.role-catalog-strip-panel :deep(.role-card) {
  min-height: 100%;
}
.role-catalog-strip-panel :deep(.ant-card-body) {
  padding: 12px 14px;
}
.role-catalog-strip-panel :deep(.toolbar) {
  margin-bottom: 10px;
  padding-bottom: 10px;
}
.role-catalog-strip-panel :deep(.selected-role-summary) {
  margin-bottom: 10px;
  padding: 9px 12px;
}
.role-catalog-strip-panel :deep(.role-list) {
  min-height: 116px;
  padding-bottom: 8px;
}
.role-catalog-strip-panel :deep(.role-item) {
  flex-basis: 230px;
  min-width: 230px;
  padding: 10px 11px;
}
.role-catalog-strip-panel :deep(.catalog-hint) {
  display: none;
}
.workspace-row {
  align-items: stretch;
}
.workspace-col {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}
.workspace-col :deep(.ant-card) {
  width: 100%;
  min-height: 100%;
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.06);
}
@media (max-width: 1400px) {
  .flow-role-panel {
    grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.4fr);
  }
}
@media (max-width: 991px) {
  .flow-role-panel,
  .context-strip {
    grid-template-columns: 1fr;
  }
  .flow-strip {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    height: auto;
  }
  .context-meta-grid {
    justify-content: flex-start;
  }
}
@media (max-width: 768px) {
  .authorization-center-v2 {
    padding: 0 0 16px;
  }
  .page-heading {
    flex-direction: column;
    padding: 14px;
  }
  .flow-strip,
  .context-strip {
    grid-template-columns: 1fr;
  }
  .context-strip {
    padding: 14px;
  }
  .context-meta {
    max-width: 100%;
  }
}
</style>
