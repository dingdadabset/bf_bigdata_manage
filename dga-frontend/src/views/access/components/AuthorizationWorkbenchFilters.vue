<template>
  <a-card :bordered="false" class="context-card">
    <div class="context-summary">
      <div class="summary-left">
        <div class="context-eyebrow">{{ isRoleManagement ? 'RBAC Role Context' : 'RBAC Authorization Workbench' }}</div>
        <div class="summary-line">
          <strong>{{ selectedClusterLabel }}</strong>
          <span>/</span>
          <strong>{{ state.selectedAuthBackend || '未选择后端' }}</strong>
          <template v-if="showSubjectContext">
            <span>/</span>
            <strong>授权对象: {{ subjectTypeLabel(state.subjectType) }} {{ state.subjectName || '未选择' }}</strong>
            <span>/</span>
            <strong>校验用户: {{ state.verificationUser || defaultVerificationText }}</strong>
          </template>
        </div>
        <div class="summary-hint">{{ identityHintText }}</div>
      </div>
      <div class="summary-actions">
        <a-tooltip placement="bottomRight">
          <template slot="title">
            <div class="operation-help-title">{{ isRoleManagement ? '角色管理提示' : '建议操作路径' }}</div>
            <ol class="operation-help-list">
              <li v-for="item in operationHelpSteps" :key="item">{{ item }}</li>
            </ol>
          </template>
          <span class="operation-help-icon">
            <a-icon type="question-circle" />
          </span>
        </a-tooltip>
        <a-tag v-if="capability" :color="capabilityStatusColor(capability.status)">
          {{ capability.engineType || '-' }} / {{ capability.authBackend || '-' }} / {{ capability.status || 'UNKNOWN' }}
        </a-tag>
        <a-button v-if="showSubjectContext" icon="sync" :loading="loading.verification" :disabled="!state.verificationUser" @click="$emit('sync-verification')">
          同步权限
        </a-button>
        <a-button v-if="showSubjectContext" type="primary" icon="reload" @click="$emit('refresh')">刷新工作台</a-button>
      </div>
    </div>

    <div v-if="showSubjectContext" class="inline-context-form authorization-inline-form">
      <div class="inline-field">
        <div class="inline-label">集群</div>
        <a-select
          :value="state.selectedCluster"
          show-search
          placeholder="请选择集群"
          option-filter-prop="children"
          :loading="loading.clusters"
          @change="update('selectedCluster', $event)"
        >
          <a-select-option
            v-for="cluster in clusters"
            :key="cluster.id"
            :value="cluster.clusterCode || cluster.clusterName"
          >
            {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
          </a-select-option>
        </a-select>
      </div>
      <div class="inline-field">
        <div class="inline-label">授权后端</div>
        <a-select
          :value="state.selectedAuthBackend"
          placeholder="请选择授权后端"
          :loading="loading.backends"
          :disabled="!state.selectedCluster"
          @change="update('selectedAuthBackend', $event)"
        >
          <a-select-option
            v-for="item in backendOptions"
            :key="item.authBackend"
            :value="item.authBackend"
          >
            {{ backendOptionLabel(item) }}
          </a-select-option>
        </a-select>
      </div>
      <div class="inline-field full-span">
        <div class="inline-label">授权对象</div>
        <a-alert
          v-if="state.subjectType === 'USER' && prefersGroupSubject"
          class="subject-route-alert"
          type="info"
          show-icon
          :message="roleDefaultGroupName ? `选择用户时会作为权限验证用户；Sentry 默认绑定该用户所属 LDAP 组，查不到用户组时才使用角色绑定组 ${roleDefaultGroupName}。` : '选择用户时会作为权限验证用户；Sentry 默认绑定该用户所属 LDAP 主组或附加组。'"
        />
        <div class="subject-row">
          <a-radio-group
            :value="state.subjectType"
            button-style="solid"
            size="small"
            @change="update('subjectType', $event.target.value)"
          >
            <a-radio-button v-for="item in subjectTypes" :key="item" :value="item">
              {{ subjectTypeLabel(item) }}
            </a-radio-button>
          </a-radio-group>
          <a-auto-complete
            :value="state.subjectName"
            :data-source="principalNames"
            placeholder="请选择或输入授权对象"
            :disabled="!capability"
            @change="update('subjectName', $event)"
          />
        </div>
        <div class="field-hint">{{ principalHintText }}</div>
      </div>
      <div class="inline-field full-span">
        <div class="inline-label">权限验证用户</div>
        <div class="verification-row">
          <a-auto-complete
            :value="state.verificationUser"
            :data-source="verificationUserNames"
            placeholder="默认跟随授权对象；组选中时请选择组内用户"
            :disabled="!capability"
            @change="update('verificationUser', $event)"
          />
          <a-button :disabled="!state.subjectName || state.subjectType !== 'USER'" @click="useSubjectAsVerification">
            跟随授权对象
          </a-button>
        </div>
        <div class="field-hint">{{ verificationHintText }}</div>
      </div>
    </div>

    <div v-else class="inline-context-form">
      <div class="inline-field">
        <div class="inline-label">集群</div>
        <a-select
          :value="state.selectedCluster"
          show-search
          placeholder="请选择集群"
          option-filter-prop="children"
          :loading="loading.clusters"
          @change="update('selectedCluster', $event)"
        >
          <a-select-option
            v-for="cluster in clusters"
            :key="cluster.id"
            :value="cluster.clusterCode || cluster.clusterName"
          >
            {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
          </a-select-option>
        </a-select>
      </div>
      <div class="inline-field">
        <div class="inline-label">授权后端</div>
        <a-select
          :value="state.selectedAuthBackend"
          placeholder="请选择授权后端"
          :loading="loading.backends"
          :disabled="!state.selectedCluster"
          @change="update('selectedAuthBackend', $event)"
        >
          <a-select-option
            v-for="item in backendOptions"
            :key="item.authBackend"
            :value="item.authBackend"
          >
            {{ backendOptionLabel(item) }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <a-alert
      v-if="showSubjectContext && capability"
      type="info"
      show-icon
      class="identity-alert"
      :message="identityHintText"
    />
  </a-card>
</template>

<script>
import {
  backendOptionLabel,
  canDirectExceptionPrincipal,
  capabilityStatusColor,
  principalOptionName,
  principalRequiresRoleBinding,
  principalSourceLabel,
  principalWarnings,
  subjectTypeLabel,
  userSourceLabel,
  verificationUserRequired
} from '../authorizationCenterHelpers';

export default {
  name: 'AuthorizationWorkbenchFilters',
  props: {
    clusters: {
      type: Array,
      default: () => []
    },
    backendOptions: {
      type: Array,
      default: () => []
    },
    capability: {
      type: Object,
      default: null
    },
    subjectTypes: {
      type: Array,
      default: () => []
    },
    principals: {
      type: Array,
      default: () => []
    },
    verificationPrincipals: {
      type: Array,
      default: () => []
    },
    selectedPrincipal: {
      type: Object,
      default: null
    },
    verificationPrincipal: {
      type: Object,
      default: null
    },
    roleDefaultGroupName: {
      type: String,
      default: ''
    },
    mode: {
      type: String,
      default: 'authorization'
    },
    state: {
      type: Object,
      required: true
    },
    loading: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    selectedClusterLabel() {
      const selected = (this.clusters || []).find(item => (item.clusterCode || item.clusterName) === this.state.selectedCluster);
      return selected ? (selected.clusterName || selected.clusterCode) : (this.state.selectedCluster || '未选择集群');
    },
    principalNames() {
      return (this.principals || []).map(principalOptionName).filter(Boolean);
    },
    verificationUserNames() {
      return (this.verificationPrincipals || []).map(principalOptionName).filter(Boolean);
    },
    defaultVerificationText() {
      return this.state.subjectType === 'USER' && this.state.subjectName ? this.state.subjectName : '未选择';
    },
    isRoleManagement() {
      return this.mode === 'role-management';
    },
    showSubjectContext() {
      return !this.isRoleManagement;
    },
    identityHintText() {
      if (!this.capability) return '请选择集群与授权后端。';
      if (!this.showSubjectContext) return '角色管理仅维护角色定义、权限范围与绑定视图；用户授权请进入授权中心。';
      if (this.prefersGroupSubject) return '当前后端依赖 LDAP 组身份，建议授权对象使用 LDAP/Sentry 组，用户仅作为权限验证账号。';
      return `${userSourceLabel(this.capability)} 身份后端已就绪。`;
    },
    prefersGroupSubject() {
      if (!this.capability) return false;
      return Boolean(this.capability.requiresLdap)
        || /LDAP|SENTRY|HIVE/i.test(`${this.capability.endpointType || ''} ${this.capability.authBackend || ''} ${this.capability.engineType || ''}`);
    },
    principalHelpText() {
      return (this.capability && this.capability.ui && this.capability.ui.principalHelpText)
        || '授权对象是真正获得角色或权限的用户/组。';
    },
    operationHelpSteps() {
      if (this.isRoleManagement) {
        return [
          '选择集群与授权后端。',
          '维护角色定义、权限范围与绑定视图。',
          '用户授权与角色绑定请进入授权中心执行。',
          '调整后确认后端状态与 DGA 记录一致。'
        ];
      }
      return [
        '先在顶部选择授权对象与校验用户。',
        '在左侧选择角色，确认该角色的权限范围。',
        '先绑定角色，再在角色范围内勾选需要下发或回收的权限子集。',
        '动作完成后，在权限校验中核对 live vs recorded 结果。'
      ];
    },
    principalHintText() {
      const parts = [this.principalHelpText];
      if (this.selectedPrincipal) {
        const source = principalSourceLabel(this.selectedPrincipal);
        if (source) parts.push(`来源：${source}`);
        if (principalRequiresRoleBinding(this.selectedPrincipal)) {
          parts.push('该对象需先完成角色绑定。');
        } else if (canDirectExceptionPrincipal(this.selectedPrincipal)) {
          parts.push('该对象可走直接例外路径。');
        }
        const warnings = principalWarnings(this.selectedPrincipal);
        if (warnings.length) parts.push(warnings.join('；'));
      }
      return parts.join(' ');
    },
    verificationHintText() {
      const parts = [
        verificationUserRequired(this.state.subjectType)
          ? '组选中时请选择组内真实用户做权限复核。'
          : '用户模式默认跟随授权对象，仅在需要代验时切换。'
      ];
      if (this.verificationPrincipal) {
        const source = principalSourceLabel(this.verificationPrincipal);
        if (source) parts.push(`来源：${source}`);
        const warnings = principalWarnings(this.verificationPrincipal);
        if (warnings.length) parts.push(warnings.join('；'));
      }
      return parts.join(' ');
    }
  },
  methods: {
    backendOptionLabel,
    capabilityStatusColor,
    subjectTypeLabel,
    update(field, value) {
      this.$emit('change', { field, value });
    },
    useSubjectAsVerification() {
      if (this.state.subjectType !== 'USER' || !this.state.subjectName) return;
      this.update('verificationUser', this.state.subjectName);
    }
  }
};
</script>

<style scoped>
.context-card {
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.05);
}
.context-card :deep(.ant-card-body) {
  padding: 14px 18px;
}
.context-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.summary-left {
  min-width: 0;
}
.context-eyebrow {
  margin-bottom: 4px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #667085;
}
.summary-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: #1f2d3d;
}
.summary-line strong {
  font-weight: 650;
}
.summary-line span {
  color: #98a2b3;
}
.summary-hint,
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.35;
  color: #667085;
}
.summary-actions,
.verification-row,
.subject-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
}
.operation-help-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #d9e7ff;
  border-radius: 50%;
  background: #f4f8ff;
  color: #1677ff;
  cursor: help;
  transition: all 0.2s ease;
}
.operation-help-icon:hover {
  border-color: #1677ff;
  background: #eaf3ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.16);
}
.operation-help-title {
  margin-bottom: 6px;
  font-weight: 650;
}
.operation-help-list {
  margin: 0 0 0 18px;
  padding: 0;
}
.operation-help-list li + li {
  margin-top: 4px;
}
.inline-context-form {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(260px, 1.4fr);
  gap: 10px 14px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}
.authorization-inline-form {
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.15fr) minmax(320px, 1.5fr) minmax(280px, 1.25fr);
  align-items: start;
}
.authorization-inline-form .full-span {
  grid-column: auto;
}
.inline-field {
  min-width: 0;
}
.full-span {
  grid-column: 1 / -1;
}
.inline-label {
  margin-bottom: 5px;
  font-size: 12px;
  line-height: 1;
  color: #667085;
}
.inline-field :deep(.ant-select),
.subject-row :deep(.ant-select-auto-complete),
.verification-row :deep(.ant-select-auto-complete) {
  width: 100%;
}
.subject-row,
.verification-row {
  flex-wrap: nowrap;
}
.subject-row :deep(.ant-radio-group) {
  flex: 0 0 auto;
}
.subject-row :deep(.ant-select-auto-complete),
.verification-row :deep(.ant-select-auto-complete) {
  min-width: 0;
  flex: 1 1 auto;
}
.identity-alert {
  display: none;
}
.subject-route-alert {
  display: none;
}
@media (max-width: 1100px) {
  .context-summary {
    align-items: flex-start;
    flex-direction: column;
  }
  .summary-actions {
    justify-content: flex-start;
  }
  .inline-context-form {
    grid-template-columns: 1fr;
  }
  .authorization-inline-form .full-span {
    grid-column: 1 / -1;
  }
  .subject-row,
  .verification-row {
    flex-wrap: wrap;
  }
}
@media (max-width: 640px) {
  .subject-row,
  .verification-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
