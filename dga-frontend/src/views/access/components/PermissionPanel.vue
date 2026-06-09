<template>
  <div v-if="user" class="control-panel">
    <a-card :bordered="false" class="header-card">
      <div class="user-header">
        <div class="user-info">
          <a-avatar :size="48" icon="user" :style="{ backgroundColor: getAvatarColor(user.username) }" />
          <div class="info-content">
            <div class="info-title">
              {{ user.username }}
              <a-tag color="green" v-if="user.status !== 'DISABLED'">Active</a-tag>
              <a-tag color="red" v-else>Disabled</a-tag>
              <a-tag :color="accountIdentity.color">{{ accountIdentity.label }}</a-tag>
              <a-tag v-if="isProtectedUser" color="orange">保护用户</a-tag>
            </div>
            <div class="info-desc">
              <span v-if="accountIdentity.hasPosix && !isSqlAuthorizationUser">主组：{{ ldapGroupName || user.primaryGroupName || '未识别' }}</span>
              <span>{{ sourceLabel }}</span>
              <span>{{ accountIdentity.description }}</span>
              <span>最近活跃：{{ user.lastActiveAt ? formatDate(user.lastActiveAt) : '暂无记录' }}</span>
              <a-tag color="blue">{{ effectiveCluster || '未选择集群' }}</a-tag>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <a-button type="primary" icon="plus-circle" @click="$emit('grant', user.username, effectiveCluster)">
            授权处理
          </a-button>
          <a-dropdown v-if="hasSecondaryActions" placement="bottomRight">
            <a-button>
              更多操作 <a-icon type="down" />
            </a-button>
            <a-menu slot="overlay" @click="handleMoreAction">
              <a-menu-item v-if="showLdapTab" key="ldap-refresh">
                <a-icon type="reload" /> 刷新 LDAP 属性
              </a-menu-item>
              <a-menu-item v-if="canRepairLdapUser" key="repair">
                <a-icon type="tool" /> 修复系统账号
              </a-menu-item>
              <a-menu-item v-if="canManageLdapGroup" key="ldap">
                <a-icon type="team" /> LDAP 用户管理
              </a-menu-item>
              <a-menu-item v-if="canManageLdapGroup" key="password">
                <a-icon type="key" /> 重置 LDAP 密码
              </a-menu-item>
              <a-menu-item v-if="canManageLdapLock" key="ldap-lock">
                <a-icon :type="ldapProfile && ldapProfile.locked ? 'unlock' : 'lock'" />
                {{ ldapProfile && ldapProfile.locked ? '解锁 LDAP 用户' : '锁定 LDAP 用户' }}
              </a-menu-item>
              <a-menu-item v-if="canManageProtection" key="protection">
                <a-icon :type="isProtectedUser ? 'unlock' : 'lock'" />
                {{ isProtectedUser ? '取消保护' : '设为保护' }}
              </a-menu-item>
              <a-menu-divider v-if="canDeleteUser" />
              <a-menu-item v-if="canDeleteUser" key="delete" :disabled="isProtectedUser" class="danger-menu-item">
                <a-icon type="delete" /> 删除用户
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </div>
      </div>
    </a-card>

    <a-tabs :active-key="activeDetailTab" class="detail-tabs" @change="activeDetailTab = $event">
      <a-tab-pane key="basic" tab="概览">
        <div class="tab-grid">
          <a-card :bordered="false" class="section-card">
            <div class="section-heading">
              <h3>基础属性</h3>
              <span>账号生命周期与身份属性</span>
            </div>
            <a-descriptions size="small" :column="2">
              <a-descriptions-item label="显示名称">{{ displayName }}</a-descriptions-item>
              <a-descriptions-item label="邮箱">{{ user.email || 'No Email' }}</a-descriptions-item>
              <a-descriptions-item label="创建时间">{{ formatDate(user.createTime) }}</a-descriptions-item>
              <a-descriptions-item label="过期时间">{{ user.expiresAt ? formatDate(user.expiresAt) : '长期有效' }}</a-descriptions-item>
              <a-descriptions-item label="用户类型">
                <a-tag :color="userTypeColor(user.userType)">{{ userTypeLabel(user.userType) }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="身份来源">{{ sourceLabel }}</a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card :bordered="false" class="section-card">
            <div class="section-heading">
              <h3>授权状态</h3>
              <span>当前集群授权适配器</span>
            </div>
            <a-alert
              v-if="capability"
              class="capability-banner"
              :type="capability.status === 'READY' ? 'info' : 'warning'"
              show-icon
            >
              <template slot="message">
                {{ capability.engineType || '-' }} / {{ capability.authBackend || '-' }}
                <span class="capability-extra">
                  {{ capability.requiresLdap ? '身份侧依赖 LDAP' : '授权不依赖 LDAP' }}
                </span>
              </template>
              <template slot="description" v-if="capability.warnings && capability.warnings.length">
                {{ capability.warnings.join('；') }}
              </template>
            </a-alert>
            <a-empty v-else description="暂无授权适配器信息" />
          </a-card>
        </div>
      </a-tab-pane>

      <a-tab-pane key="permissions" tab="权限管理">
        <ranger-card
          ref="rangerCard"
          :username="user.username"
          :cluster="effectiveCluster"
          :title="permissionCardTitle"
        />
      </a-tab-pane>

      <a-tab-pane v-if="showLdapTab" key="ldap" tab="LDAP属性">
        <div class="ldap-workspace">
          <a-card :bordered="false" class="section-card membership-card">
            <div class="section-heading">
              <h3>{{ accountIdentity.hasPosix ? '当前用户组关系' : 'LDAP 身份属性' }}</h3>
              <span>{{ accountIdentity.hasPosix ? '以当前用户为中心维护主组与附加组' : '当前账号是目录身份，未识别为可登录操作系统账号' }}</span>
            </div>
            <a-alert
              v-if="!accountIdentity.hasPosix"
              class="ldap-identity-note"
              type="info"
              show-icon
              message="该账号未识别 POSIX 系统属性"
              description="纯 LDAP 目录用户不展示主组和附加组维护；如果需要作为服务器/Hive 可登录账号，请先补齐 uidNumber、gidNumber、homeDirectory、loginShell 等系统属性。"
            />
            <div v-if="accountIdentity.hasPosix" class="membership-content">
              <div class="membership-primary">
                <span>主组</span>
                <strong>{{ ldapGroupName || user.primaryGroupName || '未识别主组' }}</strong>
                <a-tag v-if="ldapProfile && ldapProfile.locked" color="red">已锁定</a-tag>
                <a-tag v-else color="green">正常</a-tag>
              </div>
              <div class="membership-extra">
                <span>附加组</span>
                <div class="supplementary-tags">
                  <a-tag
                    v-for="groupName in supplementaryGroupNames"
                    :key="groupName"
                    color="purple"
                  >
                    {{ groupName }}
                  </a-tag>
                  <span v-if="!supplementaryGroupNames.length" class="muted">暂无附加组</span>
                </div>
              </div>
              <div class="membership-actions">
                <a-button icon="team" @click="openLdapManager">调整用户组</a-button>
              </div>
              <a-collapse v-if="currentLdapDn || ldapAttributesPreview" class="compact-collapse membership-advanced">
                <a-collapse-panel key="dn" header="DN 与原始属性">
                  <div v-if="currentLdapDn" class="attr-value">{{ currentLdapDn }}</div>
                  <pre v-if="ldapAttributesPreview" class="attr-json">{{ ldapAttributesPreview }}</pre>
                </a-collapse-panel>
              </a-collapse>
            </div>
            <a-collapse v-else-if="currentLdapDn || ldapAttributesPreview" class="compact-collapse membership-advanced ldap-attributes-only">
              <a-collapse-panel key="dn" header="DN 与原始属性">
                <div v-if="currentLdapDn" class="attr-value">{{ currentLdapDn }}</div>
                <pre v-if="ldapAttributesPreview" class="attr-json">{{ ldapAttributesPreview }}</pre>
              </a-collapse-panel>
            </a-collapse>
          </a-card>
          <ldap-group-manager
            v-if="accountIdentity.hasPosix"
            compact
            :primary-group-name="ldapGroupName"
            :supplementary-group-names="supplementaryGroupNames"
            :username="user.username"
            :cluster="effectiveCluster"
            @refresh-profile="loadLdapGroup"
          />
        </div>
      </a-tab-pane>

      <a-tab-pane key="audit" tab="审计记录">
        <a-card :bordered="false" class="section-card">
          <div class="section-heading">
            <h3>审计时间线</h3>
            <span>聚合当前页面可追溯的账号、LDAP 与授权动作</span>
          </div>
          <a-spin :spinning="loadingAudit">
            <a-timeline class="audit-timeline">
              <a-timeline-item v-for="item in visibleAuditRows" :key="item.key" :color="item.color">
                <div class="audit-title">{{ item.title }}</div>
                <div class="audit-meta">
                  <span>{{ item.time }}</span>
                  <span v-if="item.operator">操作人：{{ item.operator }}</span>
                </div>
                <div class="audit-desc">{{ item.description }}</div>
              </a-timeline-item>
            </a-timeline>
            <div v-if="auditRows.length > collapsedAuditRows.length" class="audit-toggle">
              <a-button type="link" size="small" @click="auditExpanded = !auditExpanded">
                {{ auditExpanded ? '收起操作记录' : `展开全部 ${auditRows.length} 条操作记录` }}
              </a-button>
            </div>
          </a-spin>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      :visible="ldapManagerVisible"
      title="调整用户组"
      width="680px"
      :confirm-loading="ldapSaving"
      @ok="submitLdapManager"
      @cancel="ldapManagerVisible = false"
    >
      <a-alert
        class="ldap-manager-note"
        type="info"
        show-icon
        message="这里只调整 LDAP 组关系，不修改用户密码或原始属性。"
      />
      <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="主组">
          <a-select
            v-model="ldapForm.primaryGroup"
            show-search
            option-filter-prop="children"
            placeholder="请选择主组"
          >
            <a-select-option v-for="group in ldapGroups" :key="group.name" :value="group.name">
              {{ group.name }} (gid={{ group.gidNumber }})
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="附加组">
          <a-select
            v-model="ldapForm.supplementaryGroups"
            mode="multiple"
            show-search
            option-filter-prop="children"
            placeholder="请选择附加组"
          >
            <a-select-option v-for="group in supplementaryCandidates" :key="group.name" :value="group.name">
              {{ group.name }} (gid={{ group.gidNumber }})
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="当前状态">
          <a-tag v-if="ldapProfile && ldapProfile.locked" color="red">已锁定</a-tag>
          <a-tag v-else color="green">正常</a-tag>
          <span class="form-hint">锁定/解锁与密码重置请从更多操作单独处理。</span>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal
      :visible="passwordModalVisible"
      title="重置 LDAP 密码"
      width="520px"
      :confirm-loading="passwordSaving"
      @ok="submitPasswordReset"
      @cancel="closePasswordModal"
    >
      <a-alert
        class="ldap-manager-note"
        type="warning"
        show-icon
        message="LDAP 不会返回当前明文密码。这里仅设置新密码，不展示或校验旧密码。"
      />
      <a-form-model :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="账号">
          <span>{{ user.username }}</span>
        </a-form-model-item>
        <a-form-model-item label="新密码">
          <a-input-password
            v-model="passwordForm.password"
            autocomplete="new-password"
            placeholder="请输入新的 LDAP 密码"
          />
        </a-form-model-item>
        <a-form-model-item label="确认密码">
          <a-input-password
            v-model="passwordForm.confirmPassword"
            autocomplete="new-password"
            placeholder="请再次输入新密码"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>

  <!-- Empty State -->
  <div v-else class="empty-state">
    <a-empty description="请选择左侧用户查看详情" />
  </div>
</template>

<script>
import moment from 'moment';
import { store } from '../../../store';
import RangerCard from './RangerCard.vue';
import LdapGroupManager from './LdapGroupManager.vue';
import axios from 'axios';
import { canDelete, isRootAdmin } from '../../../utils/currentUser';

const PROTECTED_BIGDATA_USERS = [
  'alading',
  'bf_hpt',
  'bf_hpt1',
  'md_bf',
  'hdfs',
  'hive',
  'yarn',
  'spark',
  'hbase',
  'impala',
  'sentry',
  'ranger'
];

export default {
  name: 'PermissionPanel',
  components: { RangerCard, LdapGroupManager },
  props: {
    user: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      activeDetailTab: 'basic',
      capability: null,
      auditEvents: [],
      auditExpanded: false,
      loadingAudit: false,
      repairingLdap: false,
      ldapGroup: null,
      ldapProfile: null,
      ldapGroups: [],
      ldapManagerVisible: false,
      ldapSaving: false,
      passwordModalVisible: false,
      passwordSaving: false,
      passwordForm: {
        password: '',
        confirmPassword: ''
      },
      ldapForm: {
        primaryGroup: '',
        supplementaryGroups: [],
        locked: false
      }
    };
  },
  watch: {
    user: {
      immediate: true,
      handler: async function() {
        this.auditExpanded = false;
        await this.loadCapability();
        this.loadLdapGroup();
        this.loadAuditTimeline();
      }
    },
    effectiveCluster: async function() {
      await this.loadCapability();
      this.loadLdapGroup();
      this.loadAuditTimeline();
    },
    showLdapTab(value) {
      if (!value && this.activeDetailTab === 'ldap') {
        this.activeDetailTab = 'basic';
      }
    }
  },
  computed: {
    currentCluster() {
      return store.currentCluster;
    },
    effectiveCluster() {
      if (this.currentCluster) return this.currentCluster;
      if (this.user && this.user.clusterName) return this.user.clusterName;
      if (this.user && this.user.cluster) return this.user.cluster;
      return '';
    },
    permissionCardTitle() {
      if (this.capability && this.capability.engineType) {
        return `${this.capability.engineType} 数据权限 (${this.capability.authBackend || 'UNKNOWN'})`;
      }
      return '数据权限';
    },
    displayName() {
      const name = `${this.user.firstName || ''} ${this.user.lastName || ''}`.trim();
      return name || this.user.role || '未维护姓名';
    },
    hasSecondaryActions() {
      return this.canRepairLdapUser || this.canManageLdapGroup || this.canManageLdapLock || this.canManageProtection || this.canDeleteUser;
    },
    canDeleteUser() {
      return isRootAdmin();
    },
    canRepairLdapUser() {
      if (!this.user || !canDelete()) return false;
      return this.isLdapManagedUser && !this.isSqlAuthorizationUser;
    },
    isLdapManagedUser() {
      if (!this.user) return false;
      const strategy = String(this.user.creationStrategy || '').toUpperCase();
      return strategy === 'OPENLDAP' || strategy === 'LDAP' || strategy === 'LDAP_IMPORT';
    },
    canManageLdapGroup() {
      return canDelete() && this.isLdapManagedUser && !this.isSqlAuthorizationUser;
    },
    canManageLdapLock() {
      return canDelete() && this.isLdapManagedUser && !this.isSqlAuthorizationUser;
    },
    canManageProtection() {
      return isRootAdmin();
    },
    isProtectedUser() {
      return this.isProtectedBigDataUser(this.user);
    },
    accountIdentity() {
      return this.getAccountIdentity(this.user, this.ldapProfile);
    },
    sourceLabel() {
      if (this.isSqlAuthorizationUser) {
        return `${this.sqlEngineLabel} 用户`;
      }
      return this.getStrategyLabel(this.user && this.user.creationStrategy);
    },
    sqlEngineType() {
      const backend = String(this.capability && this.capability.authBackend ? this.capability.authBackend : '').toUpperCase();
      const engine = String(this.capability && this.capability.engineType ? this.capability.engineType : '').toUpperCase();
      const strategy = String(this.user && this.user.creationStrategy ? this.user.creationStrategy : '').toUpperCase();
      const combined = `${backend} ${engine} ${strategy}`;
      if (combined.includes('DORIS')) return 'DORIS';
      if (combined.includes('STARROCKS') || combined.includes('STAR_ROCKS') || combined.includes('STAR')) return 'STARROCKS';
      return '';
    },
    sqlEngineLabel() {
      if (this.sqlEngineType === 'DORIS') return 'Doris';
      if (this.sqlEngineType === 'STARROCKS') return 'StarRocks';
      return '授权后端';
    },
    isSqlAuthorizationUser() {
      return Boolean(this.sqlEngineType);
    },
    showLdapTab() {
      return !this.isSqlAuthorizationUser && this.isLdapManagedUser;
    },
    ldapGroupName() {
      return this.ldapGroup && this.ldapGroup.name ? this.ldapGroup.name : '';
    },
    supplementaryGroupNames() {
      const groups = this.ldapProfile && Array.isArray(this.ldapProfile.supplementaryGroups)
        ? this.ldapProfile.supplementaryGroups
        : [];
      return groups.map(group => group.name).filter(Boolean);
    },
    currentLdapDn() {
      return this.ldapProfile && this.ldapProfile.dn ? this.ldapProfile.dn : '';
    },
    supplementaryCandidates() {
      return this.ldapGroups.filter(group => group.name !== this.ldapForm.primaryGroup);
    },
    ldapAttributesPreview() {
      const attrs = this.ldapProfile && this.ldapProfile.attributes ? this.ldapProfile.attributes : {};
      if (!Object.keys(attrs).length) return '';
      try {
        return JSON.stringify(attrs, null, 2);
      } catch (e) {
        return '';
      }
    },
    visibleAuditRows() {
      return this.auditExpanded ? this.auditRows : this.collapsedAuditRows;
    },
    collapsedAuditRows() {
      const created = this.auditRows.find(item => item.key === 'created');
      const auditEventKeys = new Set(this.auditEvents.map(item => item.key));
      const recentOperations = this.auditRows.filter(item => auditEventKeys.has(item.key)).slice(0, 2);
      const fallbackOperations = this.auditRows.filter(item => !['created', 'capability'].includes(item.key) && !auditEventKeys.has(item.key));
      const rows = [];
      if (created) {
        rows.push(created);
      }
      [...recentOperations, ...fallbackOperations].slice(0, 2).forEach(item => {
        if (!rows.some(row => row.key === item.key)) {
          rows.push(item);
        }
      });
      return rows;
    },
    auditRows() {
      const rows = [];
      rows.push({
        key: 'created',
        color: 'blue',
        title: '账号创建',
        time: this.formatDate(this.user.createTime),
        operator: this.user.createdBy || this.user.creator || '',
        description: `${this.sourceLabel}，所属集群 ${this.effectiveCluster || '未选择'}`
      });
      if (this.user.lastActiveAt) {
        rows.push({
          key: 'active',
          color: 'green',
          title: '最近活跃',
          time: this.formatDate(this.user.lastActiveAt),
          description: this.user.lastActiveSource ? `来源 ${this.user.lastActiveSource}` : '来源暂未记录'
        });
      }
      if (!this.isSqlAuthorizationUser && this.ldapGroupName) {
        rows.push({
          key: 'ldap',
          color: this.ldapProfile && this.ldapProfile.locked ? 'red' : 'purple',
          title: 'LDAP 组同步',
          time: this.formatDate(this.user.updateTime || this.user.createTime),
          description: `主组 ${this.ldapGroupName}${this.ldapProfile && this.ldapProfile.locked ? '，账号已锁定' : ''}`
        });
      }
      this.auditEvents.forEach(item => {
        rows.push({
          key: item.key,
          color: item.color || 'blue',
          title: item.title || '授权审计',
          time: this.formatDate(item.time),
          operator: item.operator || '',
          description: item.description || '-'
        });
      });
      if (this.capability) {
        rows.push({
          key: 'capability',
          color: this.capability.status === 'READY' ? 'green' : 'orange',
          title: '授权适配器检查',
          time: '当前',
          operator: '',
          description: `${this.capability.engineType || '-'} / ${this.capability.authBackend || '-'}，${this.capability.requiresLdap ? '依赖 LDAP' : '不依赖 LDAP'}`
        });
      }
      return rows;
    }
  },
  methods: {
    handleMoreAction({ key }) {
      if (key === 'repair') {
        this.repairLdapUser();
      } else if (key === 'ldap-refresh') {
        this.loadLdapGroup();
      } else if (key === 'ldap') {
        this.openLdapManager();
      } else if (key === 'password') {
        this.openPasswordModal();
      } else if (key === 'ldap-lock') {
        this.confirmToggleLdapLock();
      } else if (key === 'protection' && this.canManageProtection) {
        this.$emit('toggle-protection', this.user);
      } else if (key === 'delete' && this.canDeleteUser && !this.isProtectedUser) {
        this.$emit('delete', this.user);
      }
    },
    async loadCapability() {
      if (!this.user) {
        this.capability = null;
        return;
      }
      try {
        const params = {};
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const res = await axios.get('/api/access/capabilities', { params });
        this.capability = res.data || null;
      } catch (e) {
        this.capability = null;
      }
    },
    async loadAuditTimeline() {
      if (!this.user || !this.user.username) {
        this.auditEvents = [];
        return;
      }
      this.loadingAudit = true;
      try {
        const params = {};
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const { data } = await axios.get(`/api/access/users/${encodeURIComponent(this.user.username)}/audit-timeline`, { params });
        this.auditEvents = Array.isArray(data) ? data : [];
      } catch (e) {
        this.auditEvents = [];
      } finally {
        this.loadingAudit = false;
      }
    },
    async loadLdapGroup() {
      if (!this.user || !this.user.username || !this.effectiveCluster || !this.canManageLdapGroup) {
        this.ldapGroup = null;
        this.ldapProfile = null;
        return;
      }
      try {
        const { data } = await axios.get(`/api/access/user/${encodeURIComponent(this.user.username)}/ldap-profile`, {
          params: { cluster: this.effectiveCluster }
        });
        this.ldapProfile = data || null;
        this.ldapGroup = data && data.primaryGroup ? data.primaryGroup : null;
      } catch (e) {
        this.ldapGroup = null;
        this.ldapProfile = null;
      }
    },
    async openLdapManager() {
      if (!this.user || !this.effectiveCluster) {
        this.$message.warning('请先选择用户和集群');
        return;
      }
      try {
        const [{ data: groups }, { data: profile }] = await Promise.all([
          axios.get('/api/access/ldap-groups', { params: { cluster: this.effectiveCluster } }),
          axios.get(`/api/access/user/${encodeURIComponent(this.user.username)}/ldap-profile`, {
            params: { cluster: this.effectiveCluster }
          })
        ]);
        this.ldapGroups = Array.isArray(groups) ? groups : [];
        this.ldapProfile = profile || null;
        this.ldapGroup = profile && profile.primaryGroup ? profile.primaryGroup : null;
        this.ldapForm.primaryGroup = profile && profile.primaryGroup && profile.primaryGroup.name ? profile.primaryGroup.name : '';
        this.ldapForm.supplementaryGroups = Array.isArray(profile && profile.supplementaryGroups)
          ? profile.supplementaryGroups.map(group => group.name).filter(Boolean)
          : [];
        this.ldapForm.locked = Boolean(profile && profile.locked);
        this.ldapManagerVisible = true;
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载 LDAP 组信息失败');
      }
    },
    async submitLdapManager() {
      if (!this.ldapForm.primaryGroup) {
        this.$message.warning('请选择主组');
        return;
      }
      this.ldapSaving = true;
      try {
        const username = encodeURIComponent(this.user.username);
        const params = { cluster: this.effectiveCluster };
        if (this.ldapGroupName !== this.ldapForm.primaryGroup) {
          await axios.put(`/api/access/user/${username}/ldap-group`, { groupName: this.ldapForm.primaryGroup }, { params });
        }
        await axios.put(`/api/access/user/${username}/ldap-supplementary-groups`, {
          groupNames: this.ldapForm.supplementaryGroups
        }, { params });
        await this.loadLdapGroup();
        this.ldapManagerVisible = false;
        this.$message.success('LDAP 用户组已更新');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '更新 LDAP 用户组失败');
      } finally {
        this.ldapSaving = false;
      }
    },
    openPasswordModal() {
      if (!this.user || !this.user.username || !this.effectiveCluster) {
        this.$message.warning('请先选择用户和集群');
        return;
      }
      this.passwordForm.password = '';
      this.passwordForm.confirmPassword = '';
      this.passwordModalVisible = true;
    },
    closePasswordModal() {
      this.passwordModalVisible = false;
      this.passwordForm.password = '';
      this.passwordForm.confirmPassword = '';
    },
    async submitPasswordReset() {
      if (!this.passwordForm.password) {
        this.$message.warning('请输入新的 LDAP 密码');
        return;
      }
      if (this.passwordForm.password !== this.passwordForm.confirmPassword) {
        this.$message.warning('两次输入的密码不一致');
        return;
      }
      this.passwordSaving = true;
      try {
        await axios.put(`/api/access/user/${encodeURIComponent(this.user.username)}/ldap-password`, {
          password: this.passwordForm.password
        }, { params: { cluster: this.effectiveCluster } });
        this.$message.success('LDAP 密码已重置');
        this.closePasswordModal();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '重置 LDAP 密码失败');
      } finally {
        this.passwordSaving = false;
      }
    },
    confirmToggleLdapLock() {
      if (!this.user || !this.user.username || !this.effectiveCluster) {
        this.$message.warning('请先选择用户和集群');
        return;
      }
      const nextLocked = !(this.ldapProfile && this.ldapProfile.locked);
      this.$confirm({
        title: nextLocked ? '确认锁定 LDAP 用户？' : '确认解锁 LDAP 用户？',
        content: nextLocked
          ? `锁定后 ${this.user.username} 将不能继续作为 LDAP 账号登录。`
          : `解锁后 ${this.user.username} 将恢复 LDAP 登录能力。`,
        okText: nextLocked ? '锁定' : '解锁',
        okType: nextLocked ? 'danger' : 'primary',
        cancelText: '取消',
        onOk: () => this.toggleLdapLock(nextLocked)
      });
    },
    async toggleLdapLock(locked) {
      try {
        await axios.put(`/api/access/user/${encodeURIComponent(this.user.username)}/ldap-lock`, {
          locked
        }, { params: { cluster: this.effectiveCluster } });
        await this.loadLdapGroup();
        this.$message.success(locked ? 'LDAP 用户已锁定' : 'LDAP 用户已解锁');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '更新 LDAP 锁定状态失败');
      }
    },
    formatDate(date) {
      return date ? moment(date).format('YYYY-MM-DD HH:mm') : 'N/A';
    },
    getAvatarColor(username) {
      const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#1890ff'];
      let hash = 0;
      for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      return colors[Math.abs(hash) % colors.length];
    },
    isProtectedBigDataUser(user) {
      if (!user || !user.username) return false;
      if (user.protectedUser !== undefined && user.protectedUser !== null) {
        return Boolean(user.protectedUser);
      }
      const username = String(user.username).toLowerCase();
      const role = String(user.role || user.userRole || '').toLowerCase();
      return PROTECTED_BIGDATA_USERS.includes(username)
        || role.includes('important')
        || role.includes('protected')
        || role.includes('bigdata');
    },
    getStrategyLabel(strategy) {
      const s = (strategy || '').toUpperCase();
      const labels = {
        OPENLDAP: 'OpenLDAP 创建',
        LDAP: 'LDAP 创建',
        LDAP_IMPORT: 'LDAP 导入',
        IPA_HTTP: 'IPA 创建',
        IPA_IMPORT: 'IPA 导入',
        IPA_SSH: 'IPA SSH',
        INIT_USER: '初始化用户',
        STARROCKS: 'StarRocks 用户',
        STARROCKS_IMPORT: 'StarRocks 导入',
        DORIS: 'Doris 用户',
        DORIS_IMPORT: 'Doris 导入',
        LIVE_AUTH_BACKEND: '授权后端用户'
      };
      return labels[s] || (strategy || '未知来源');
    },
    getStrategyClass(strategy) {
      const s = (strategy || '').toUpperCase();
      if (s.includes('LDAP')) return 'source-ldap';
      if (s.includes('IPA')) return 'source-ipa';
      if (s.includes('STAR') || s.includes('DORIS') || s.includes('LIVE_AUTH')) return 'source-sql';
      if (s.includes('SELF')) return 'source-self';
      if (s.includes('INIT')) return 'source-init';
      return 'source-default';
    },
    getAccountIdentity(user, ldapProfile) {
      if (this.isSqlAuthorizationUser) {
        return {
          label: `${this.sqlEngineLabel} 用户`,
          color: this.sqlEngineType === 'DORIS' ? 'volcano' : 'blue',
          description: `${this.sqlEngineLabel} 授权后端账号，不依赖 LDAP/POSIX 属性`,
          hasPosix: false
        };
      }
      const attrs = ldapProfile && ldapProfile.attributes ? ldapProfile.attributes : {};
      const uidNumber = user && (user.uidNumber || attrs.uidNumber);
      const gidNumber = user && (user.gidNumber || attrs.gidNumber);
      const homeDirectory = user && (user.homeDirectory || attrs.homeDirectory);
      const loginShell = user && (user.loginShell || attrs.loginShell);
      const hasPosix = Boolean(uidNumber && gidNumber && homeDirectory && loginShell);
      if (hasPosix) {
        return {
          label: '操作系统用户',
          color: 'geekblue',
          description: `系统账号：uid=${uidNumber}，gid=${gidNumber}`,
          hasPosix: true
        };
      }
      if (this.isLdapManagedUser) {
        return {
          label: 'LDAP用户',
          color: 'cyan',
          description: '目录身份账号，未识别系统登录属性',
          hasPosix: false
        };
      }
      return {
        label: '平台用户',
        color: 'default',
        description: '平台侧账号，未识别 LDAP/POSIX 属性',
        hasPosix: false
      };
    },
    userTypeLabel(userType) {
      const value = (userType || 'INTERNAL').toUpperCase();
      const labels = {
        INTERNAL: '内部用户',
        OUTSOURCER: '外包用户',
        TEMPORARY: '临时用户',
        SERVICE: '服务账号'
      };
      return labels[value] || value;
    },
    userTypeColor(userType) {
      const value = (userType || 'INTERNAL').toUpperCase();
      const colors = {
        INTERNAL: 'default',
        OUTSOURCER: 'volcano',
        TEMPORARY: 'orange',
        SERVICE: 'purple'
      };
      return colors[value] || 'default';
    },
    async repairLdapUser() {
      if (!this.user || !this.user.username || !this.effectiveCluster) {
        this.$message.warning('请先选择需要修复的用户和集群');
        return;
      }
      this.repairingLdap = true;
      try {
        const { data } = await axios.post(
          `/api/access/user/${encodeURIComponent(this.user.username)}/repair-ldap`,
          null,
          { params: { cluster: this.effectiveCluster } }
        );
        this.$message.success(data?.message || '已补齐系统账号属性');
        await this.loadLdapGroup();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '修复系统账号失败');
      } finally {
        this.repairingLdap = false;
      }
    },
    refresh() {
      this.loadCapability();
      this.loadLdapGroup();
      if (this.$refs.rangerCard) {
        this.$refs.rangerCard.loadUserAccess();
      }
    }
  }
};
</script>

<style scoped>
.control-panel {
  min-height: 100%;
}
.header-card,
.section-card {
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(16, 24, 40, 0.04);
}
.user-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
}
.info-content {
  margin-left: 16px;
  min-width: 0;
}
.eyebrow {
  color: #667085;
  font-size: 12px;
  margin-bottom: 4px;
}
.info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #101828;
}
.info-desc {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
  color: #667085;
}
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.detail-tabs {
  margin-top: 16px;
  padding: 0 16px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(16, 24, 40, 0.04);
}
.detail-tabs ::v-deep .ant-tabs-bar {
  position: sticky;
  top: 0;
  z-index: 5;
  margin-bottom: 16px;
  background: #fff;
}
.tab-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.section-heading h3 {
  margin: 0;
  color: #101828;
  font-size: 16px;
  font-weight: 600;
}
.section-heading span {
  color: #98a2b3;
  font-size: 12px;
}
.muted {
  color: #98a2b3;
}
.ldap-profile {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ldap-profile-main {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ldap-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.membership-card {
  border: 1px solid #edf0f5;
}
.membership-content {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.2fr) auto;
  gap: 16px;
  align-items: center;
}
.membership-primary,
.membership-extra {
  min-width: 0;
}
.membership-primary > span,
.membership-extra > span {
  display: block;
  margin-bottom: 6px;
  color: #667085;
  font-size: 12px;
}
.membership-primary strong {
  margin-right: 8px;
  color: #111827;
  font-size: 20px;
}
.supplementary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.supplementary-tags .ant-tag {
  margin-right: 0;
}
.membership-actions {
  justify-self: end;
}
.membership-advanced {
  grid-column: 1 / -1;
}
.ldap-identity-note {
  margin-bottom: 16px;
}
.ldap-attributes-only {
  margin-top: 12px;
}
.compact-collapse {
  background: #fff;
}
.attr-value {
  padding: 8px 12px;
  color: #344054;
  background: #f9fafb;
  border-radius: 6px;
  word-break: break-all;
}
.attr-json {
  max-height: 260px;
  margin: 8px 0 0;
  padding: 12px;
  overflow: auto;
  color: #344054;
  background: #f9fafb;
  border-radius: 6px;
}
.ldap-manager-note {
  margin-bottom: 16px;
}
.form-hint {
  margin-left: 8px;
  color: #98a2b3;
  font-size: 12px;
}
.audit-timeline {
  margin-top: 4px;
}
.audit-title {
  color: #101828;
  font-weight: 600;
}
.audit-meta,
.audit-desc {
  color: #667085;
  font-size: 12px;
}
.audit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.audit-toggle {
  margin-top: 8px;
  padding-left: 18px;
}
.danger-menu-item {
  color: #cf1322;
}
.source-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
  border: 1px solid transparent;
}
.source-ldap {
  color: #067647;
  background: #ecfdf3;
  border-color: #abefc6;
}
.source-ipa {
  color: #6941c6;
  background: #f4f3ff;
  border-color: #d9d6fe;
}
.source-self {
  color: #175cd3;
  background: #eff8ff;
  border-color: #b2ddff;
}
.source-init {
  color: #b54708;
  background: #fffaeb;
  border-color: #fedf89;
}
.source-default {
  color: #344054;
  background: #f2f4f7;
  border-color: #d0d5dd;
}
.capability-banner {
  margin-bottom: 0;
}
.capability-extra {
  margin-left: 12px;
  color: #667085;
}
@media (min-width: 1280px) {
  .tab-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 960px) {
  .membership-content {
    grid-template-columns: 1fr;
  }
  .membership-actions {
    justify-self: start;
  }
}
@media (max-width: 768px) {
  .user-header,
  .section-heading {
    flex-direction: column;
  }
  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
