<template>
  <a-modal
    :visible="visible"
    title="历史权限接管"
    width="980px"
    :confirm-loading="loading"
    ok-text="仅接管到 DGA 记录"
    @ok="confirm"
    @cancel="$emit('cancel')"
  >
    <a-alert
      class="adoption-alert"
      type="info"
      show-icon
      message="非破坏性接管"
      description="此操作只写入 DGA 本地权限记录和 LOCAL_ONLY 角色绑定，不会向 Hive/Sentry 下发、回收或创建任何后端权限。"
    />

    <div v-if="preview" class="adoption-summary">
      <a-tag :color="reconciliationStatusColor(preview.reconciliationStatus)">
        {{ reconciliationStatusLabel(preview.reconciliationStatus) }}
      </a-tag>
      <a-tag color="blue">角色权限 {{ preview.summary && preview.summary.rolePermissionCount || 0 }}</a-tag>
      <a-tag color="green">用户直授权 {{ directLiveCount }}</a-tag>
      <a-tag color="purple">组继承 {{ preview.summary && preview.summary.liveGroupInheritedCount || 0 }}</a-tag>
      <a-tag color="cyan">可接管 {{ preview.summary && preview.summary.adoptableCount || 0 }}</a-tag>
    </div>

    <a-alert
      v-for="warning in warnings"
      :key="warning"
      class="adoption-warning"
      type="warning"
      show-icon
      :message="warning"
    />

    <div class="adoption-toolbar">
      <a-input-search v-model="keyword" placeholder="搜索库、表、权限或来源" allow-clear style="max-width: 320px" />
      <a-select v-model="statusFilter" style="width: 180px">
        <a-select-option value="ALL">全部状态</a-select-option>
        <a-select-option value="DIRECT_MATCH">可直接接管</a-select-option>
        <a-select-option value="GROUP_INHERITED_MATCH">LDAP 组继承</a-select-option>
        <a-select-option value="ROLE_MISSING_LIVE">角色权限未出现</a-select-option>
        <a-select-option value="LIVE_EXTRA_DIRECT">角色外历史权限</a-select-option>
        <a-select-option value="ALREADY_RECORDED">已有记录</a-select-option>
      </a-select>
    </div>

    <a-table
      size="small"
      row-key="key"
      :columns="columns"
      :data-source="filteredItems"
      :pagination="{ pageSize: 8 }"
      :row-selection="{ selectedRowKeys: localSelectedKeys, onChange: changeSelection, getCheckboxProps: checkboxProps }"
    >
      <template slot="resource" slot-scope="text, record">
        <div class="adoption-resource">{{ record.databaseName || '*' }}{{ record.tableName ? `.${record.tableName}` : '.*' }}</div>
        <div class="adoption-resource-sub">{{ record.resourceType || '-' }} / {{ record.authBackend || '-' }}</div>
      </template>
      <template slot="permission" slot-scope="text, record">
        <a-tag color="blue">{{ record.permission }}</a-tag>
      </template>
      <template slot="status" slot-scope="text, record">
        <a-tag :color="adoptionStatusColor(record.adoptionStatus)">{{ adoptionStatusLabel(record.adoptionStatus) }}</a-tag>
      </template>
      <template slot="source" slot-scope="text, record">
        <div>{{ sourceText(record) }}</div>
        <div v-if="record.grantText" class="adoption-resource-sub">{{ record.grantText }}</div>
      </template>
      <template slot="warning" slot-scope="text, record">
        <span v-if="!record.warning">-</span>
        <a-tooltip v-else :title="record.warning">
          <a-tag color="orange">提示</a-tag>
        </a-tooltip>
      </template>
    </a-table>

    <div v-if="reverseBindableItems.length" class="reverse-bind-section">
      <a-divider orientation="left">可反向绑定到角色</a-divider>
      <a-alert
        class="adoption-alert"
        type="info"
        show-icon
        message="反向绑定"
        description="以下权限存在于用户 live 权限中但不在角色范围内。勾选后可将其添加到角色的权限范围。"
      />
      <a-checkbox-group v-model="reverseBindSelectedKeys" class="reverse-bind-checkbox-group">
        <div v-for="item in reverseBindableItems" :key="item.key" class="reverse-bind-item">
          <a-checkbox :value="item.key">
            <a-tag color="purple">可反向绑定到角色</a-tag>
            <span class="reverse-bind-resource">{{ item.databaseName || '*' }}{{ item.tableName ? `.${item.tableName}` : '.*' }}</span>
            <a-tag color="blue">{{ item.permission }}</a-tag>
            <span class="adoption-resource-sub">{{ item.resourceType || '-' }}</span>
          </a-checkbox>
        </div>
      </a-checkbox-group>
    </div>

    <div class="adoption-acknowledgements">
      <a-checkbox v-if="needsRoleMissing" v-model="ack.roleMissing">
        我确认角色中缺失的后端权限不会自动补发，后续需要单独通过子集授权显式下发。
      </a-checkbox>
      <a-checkbox v-if="needsExtra" v-model="ack.extra">
        我确认角色外历史权限将继续保持 live-only/unmanaged，不会自动塞入当前角色。
      </a-checkbox>
      <a-checkbox v-if="needsPartial" v-model="ack.partial">
        我确认本次只接管历史权限与角色范围的重叠部分。
      </a-checkbox>
      <a-checkbox v-if="needsGroup" v-model="ack.groupInherited">
        我确认所选权限来自 LDAP 组继承，将按组来源记录，不作为用户私有权限接管。
      </a-checkbox>
    </div>

    <div class="adoption-governance">
      <a-input v-model="adoptionReason" placeholder="接管原因，例如：历史 Sentry 权限纳入 DGA 管理" />
      <a-input v-model="ticketNo" placeholder="工单号（可选）" />
      <a-input v-model="approver" placeholder="审批人（可选）" />
    </div>
  </a-modal>
</template>

<script>
import {
  adoptionStatusColor,
  adoptionStatusLabel,
  defaultReverseBindKeys,
  historicalAdoptionNeedsExtraAcknowledgement,
  historicalAdoptionNeedsGroupAcknowledgement,
  historicalAdoptionNeedsPartialAcknowledgement,
  historicalAdoptionNeedsRoleMissingAcknowledgement,
  isReverseBindable,
  reconciliationStatusColor,
  reconciliationStatusLabel
} from '../authorizationCenterHelpers';

export default {
  name: 'HistoricalPermissionAdoptionModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    preview: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    selectedKeys: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      keyword: '',
      statusFilter: 'ALL',
      localSelectedKeys: [],
      reverseBindSelectedKeys: [],
      adoptionReason: '',
      ticketNo: '',
      approver: '',
      ack: {
        roleMissing: false,
        extra: false,
        partial: false,
        groupInherited: false
      },
      columns: [
        { title: '资源', key: 'resource', scopedSlots: { customRender: 'resource' } },
        { title: '权限', key: 'permission', width: 90, scopedSlots: { customRender: 'permission' } },
        { title: '接管状态', key: 'status', width: 150, scopedSlots: { customRender: 'status' } },
        { title: '来源', key: 'source', scopedSlots: { customRender: 'source' } },
        { title: '提示', key: 'warning', width: 90, scopedSlots: { customRender: 'warning' } }
      ]
    };
  },
  computed: {
    warnings() {
      return Array.isArray(this.preview?.warnings) ? this.preview.warnings : [];
    },
    directLiveCount() {
      const summary = this.preview?.summary || {};
      return Number(summary.liveDirectCount || 0) + Number(summary.liveUserRoleCount || 0);
    },
    filteredItems() {
      const normalizedKeyword = String(this.keyword || '').trim().toLowerCase();
      const normalizedStatus = String(this.statusFilter || 'ALL').toUpperCase();
      return (this.preview?.items || []).filter(item => {
        if (normalizedStatus !== 'ALL' && String(item?.adoptionStatus || '').toUpperCase() !== normalizedStatus) return false;
        if (!normalizedKeyword) return true;
        return [
          item.databaseName,
          item.tableName,
          item.permission,
          item.resourceType,
          item.authBackend,
          item.adoptionStatus,
          item.source,
          item.sourceRole,
          item.sourceGroup,
          item.grantText,
          item.warning
        ].some(field => String(field || '').toLowerCase().includes(normalizedKeyword));
      });
    },
    needsRoleMissing() {
      return historicalAdoptionNeedsRoleMissingAcknowledgement(this.preview);
    },
    needsExtra() {
      return historicalAdoptionNeedsExtraAcknowledgement(this.preview);
    },
    needsPartial() {
      return historicalAdoptionNeedsPartialAcknowledgement(this.preview);
    },
    needsGroup() {
      return historicalAdoptionNeedsGroupAcknowledgement(this.preview, this.localSelectedKeys);
    },
    reverseBindableItems() {
      return (this.preview?.items || []).filter(item => isReverseBindable(item));
    }
  },
  watch: {
    visible(value) {
      if (value) this.resetLocalState();
    },
    selectedKeys: {
      immediate: true,
      handler(value) {
        this.localSelectedKeys = Array.isArray(value) ? value.slice() : [];
      }
    }
  },
  methods: {
    adoptionStatusColor,
    adoptionStatusLabel,
    reconciliationStatusColor,
    reconciliationStatusLabel,
    resetLocalState() {
      this.keyword = '';
      this.statusFilter = 'ALL';
      this.localSelectedKeys = Array.isArray(this.selectedKeys) ? this.selectedKeys.slice() : [];
      this.reverseBindSelectedKeys = defaultReverseBindKeys(this.preview);
      this.adoptionReason = '';
      this.ticketNo = '';
      this.approver = '';
      this.ack = { roleMissing: false, extra: false, partial: false, groupInherited: false };
    },
    checkboxProps(record) {
      const status = String(record?.adoptionStatus || '').toUpperCase();
      const disabled = !record?.adoptable && status !== 'GROUP_INHERITED_MATCH';
      return { props: { disabled } };
    },
    changeSelection(keys) {
      this.localSelectedKeys = keys || [];
      this.$emit('change-selected-keys', this.localSelectedKeys);
    },
    sourceText(record) {
      const source = String(record?.source || '').toUpperCase();
      if (source === 'GROUP_ROLE') return `LDAP 组 ${record.sourceGroup || '-'} / 角色 ${record.sourceRole || '-'}`;
      if (source === 'USER_ROLE') return `用户私有角色 ${record.sourceRole || '-'}`;
      if (source === 'USER') return '用户直授权';
      return source || '-';
    },
    confirm() {
      const hasAdoptionSelection = this.localSelectedKeys.length > 0;
      const hasReverseBindSelection = this.reverseBindSelectedKeys.length > 0;
      if (!hasAdoptionSelection && !hasReverseBindSelection) {
        this.$message.warning('请选择至少一项可接管或可反向绑定权限');
        return;
      }
      if (this.needsRoleMissing && !this.ack.roleMissing) {
        this.$message.warning('请确认缺失的角色权限不会自动补发');
        return;
      }
      if (this.needsExtra && !this.ack.extra) {
        this.$message.warning('请确认角色外历史权限将继续保持未接管状态');
        return;
      }
      if (this.needsPartial && !this.ack.partial) {
        this.$message.warning('请确认本次只接管重叠部分');
        return;
      }
      if (this.needsGroup && !this.ack.groupInherited) {
        this.$message.warning('请确认 LDAP 组继承权限按组来源记录');
        return;
      }
      this.$emit('confirm', {
        selectedKeys: this.localSelectedKeys,
        reverseBindSelectedKeys: this.reverseBindSelectedKeys,
        acknowledgements: { ...this.ack },
        adoptionReason: this.adoptionReason,
        ticketNo: this.ticketNo,
        approver: this.approver
      });
    }
  }
};
</script>

<style scoped>
.adoption-alert,
.adoption-warning {
  margin-bottom: 12px;
}

.adoption-summary,
.adoption-toolbar,
.adoption-governance {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.adoption-resource {
  color: #1f2d3d;
  font-weight: 600;
}

.adoption-resource-sub {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.adoption-acknowledgements {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.adoption-governance {
  margin-top: 12px;
}

.adoption-governance .ant-input {
  max-width: 300px;
}

.reverse-bind-section {
  margin-top: 16px;
}

.reverse-bind-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.reverse-bind-item {
  padding: 4px 0;
}

.reverse-bind-resource {
  font-weight: 600;
  margin: 0 6px;
}
</style>
