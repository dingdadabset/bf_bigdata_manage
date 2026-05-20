<template>
  <div class="verification-panel">
    <div class="panel-header">
      <div class="header-copy">
        <div class="panel-title">权限校验 / Live vs Recorded</div>
        <span>
          <strong>{{ verificationUser || '未选择验证用户' }}</strong>
          对照实时授权后端权限与 DGA 记录。
        </span>
      </div>
      <div class="panel-actions">
        <a-button
          v-if="!verificationUser && subjectName"
          icon="user"
          @click="$emit('use-subject-as-verification')"
        >
          使用目标主体作为验证用户
        </a-button>
        <a-button icon="sync" :loading="loading" :disabled="!verificationUser" @click="$emit('sync')">
          同步后端权限
        </a-button>
        <a-button icon="reload" :loading="loading" :disabled="!verificationUser" @click="$emit('refresh')">
          刷新
        </a-button>
      </div>
    </div>

    <div v-if="verificationUser && snapshot" class="summary-grid">
      <div class="summary-pill">
        <span>总条目</span>
        <strong>{{ summary.total }}</strong>
      </div>
      <div class="summary-pill">
        <span>完全一致</span>
        <strong>{{ summary.matched }}</strong>
      </div>
      <div class="summary-pill warning">
        <span>仅后端存在</span>
        <strong>{{ summary.liveOnly }}</strong>
      </div>
      <div class="summary-pill info">
        <span>仅 DGA 记录</span>
        <strong>{{ summary.recordedOnly }}</strong>
      </div>
    </div>

    <a-alert
      v-if="verificationUser && snapshot"
      class="status-help"
      type="info"
      show-icon
      message="状态说明"
      description="MATCHED=后端与 DGA 记录一致；LIVE_ONLY=仅授权后端存在，DGA 没有对应记录，常见于历史授权、手工授权或 LDAP 组继承；RECORDED_ONLY=仅 DGA 有记录，授权后端已没有对应权限。来源显示为历史接管记录时，只代表 DGA 已记录该历史权限，不代表接管动作修改过后端授权。"
    />

    <div v-if="verificationUser && snapshot" class="toolbar">
      <a-input-search v-model="keyword" allow-clear placeholder="搜索资源、权限或原始返回" style="width: 280px" />
      <a-select v-model="statusFilter" style="width: 180px">
        <a-select-option value="ALL">全部结果</a-select-option>
        <a-select-option value="MATCHED">完全一致</a-select-option>
        <a-select-option value="LIVE_ONLY">仅后端存在</a-select-option>
        <a-select-option value="RECORDED_ONLY">仅 DGA 记录</a-select-option>
      </a-select>
    </div>

    <a-table
      v-if="verificationUser && snapshot"
      size="small"
      row-key="key"
      :columns="columns"
      :data-source="filteredRows"
      :loading="loading"
      :pagination="{ pageSize: 8 }"
      :locale="{ emptyText: '当前验证用户暂无可对照权限' }"
    >
      <template slot="status" slot-scope="text">
        <a-tooltip :title="verificationStatusDescription(text)">
          <a-tag :color="verificationStatusColor(text)">{{ verificationStatusLabel(text) }}</a-tag>
        </a-tooltip>
      </template>
      <template slot="resource" slot-scope="text, record">
        <div class="resource-cell">
          <a-tag color="blue">{{ resourceTypeLabel(record.resourceType) }}</a-tag>
          <span>{{ formatResource(record) }}</span>
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

    <a-empty v-else description="选择验证用户后，查看 live 权限与 DGA 记录差异" />
  </div>
</template>

<script>
import {
  resourceTypeLabel,
  verificationDiffRows,
  verificationStatusColor,
  verificationSummary
} from '../authorizationCenterHelpers';

export default {
  name: 'PermissionVerificationPanel',
  props: {
    verificationUser: {
      type: String,
      default: ''
    },
    subjectName: {
      type: String,
      default: ''
    },
    snapshot: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      keyword: '',
      statusFilter: 'ALL',
      columns: [
        { title: '对照结果', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' } },
        { title: '资源范围', key: 'resource', scopedSlots: { customRender: 'resource' } },
        { title: '权限', dataIndex: 'permission', key: 'permission', scopedSlots: { customRender: 'permission' } },
        { title: '来源', key: 'source', customRender: (_, record) => this.sourceText(record) },
        { title: '原始返回', dataIndex: 'grantText', key: 'grantText', scopedSlots: { customRender: 'grantText' } }
      ]
    };
  },
  computed: {
    rows() {
      return verificationDiffRows(this.snapshot);
    },
    filteredRows() {
      const keyword = String(this.keyword || '').trim().toLowerCase();
      const statusFilter = String(this.statusFilter || 'ALL').toUpperCase();
      return this.rows.filter(row => {
        if (statusFilter !== 'ALL' && String(row.status || '').toUpperCase() !== statusFilter) return false;
        if (!keyword) return true;
        return [
          row.databaseName,
          row.tableName,
          row.permission,
          row.resourceType,
          row.source,
          row.sourceRole,
          row.sourceGroup,
          row.grantText,
          row.status
        ].some(field => String(field || '').toLowerCase().includes(keyword));
      });
    },
    summary() {
      return verificationSummary(this.snapshot);
    }
  },
  methods: {
    resourceTypeLabel,
    verificationStatusColor,
    verificationStatusLabel(status) {
      const normalized = String(status || '').toUpperCase();
      if (normalized === 'MATCHED') return '完全一致';
      if (normalized === 'LIVE_ONLY') return '仅后端存在';
      if (normalized === 'RECORDED_ONLY') return '仅 DGA 记录';
      return status || '-';
    },
    verificationStatusDescription(status) {
      const normalized = String(status || '').toUpperCase();
      if (normalized === 'MATCHED') return 'MATCHED：授权后端与 DGA 本地记录都存在这项权限。';
      if (normalized === 'LIVE_ONLY') return 'LIVE_ONLY：授权后端实时查到了这项权限，但 DGA 本地没有对应记录，常见于历史授权、手工授权或 LDAP 组继承。';
      if (normalized === 'RECORDED_ONLY') return 'RECORDED_ONLY：DGA 本地有记录，但授权后端实时查询不到这项权限。';
      return status || '-';
    },
    sourceText(record) {
      const source = String(record?.source || '').toUpperCase();
      const grantMode = String(record?.grantMode || record?.recorded?.grantMode || '').toUpperCase();
      if (grantMode === 'ROLE_ADOPTION') {
        const subjectType = String(record?.recorded?.subjectType || record?.subjectType || '').toUpperCase();
        const subjectName = record?.recorded?.subjectName || record?.subjectName || record?.sourceGroup || '-';
        const sourceLabel = source === 'GROUP_ROLE' ? `LDAP 组 ${record?.sourceGroup || subjectName}` : '历史用户权限';
        return `历史接管记录（${sourceLabel}，未修改后端授权，${subjectType || '主体'} ${subjectName}）`;
      }
      if (source === 'GROUP_ROLE') {
        const group = record.sourceGroup || '-';
        return record.sourceRole ? `继承自组 ${group} / 角色 ${record.sourceRole}` : `继承自组 ${group}`;
      }
      if (source === 'USER_ROLE') return `用户私有角色 ${record.sourceRole || '-'}`;
      if (source === 'USER') return '用户直授权';
      if (record?.sourceRole) return `角色 ${record.sourceRole}`;
      return record?.source || '-';
    },
    formatResource(record) {
      if (!record) return '-';
      if (record.resourceType === 'TABLE') {
        return `${record.databaseName || '-'}.${record.tableName || '*'}`;
      }
      return record.databaseName || 'ALL DATABASES';
    }
  }
};
</script>

<style scoped>
.verification-panel {
  padding-top: 4px;
}
.panel-header,
.panel-actions,
.toolbar {
  display: flex;
  gap: 12px;
}
.panel-header,
.toolbar {
  justify-content: space-between;
}
.panel-header {
  align-items: flex-start;
  margin-bottom: 16px;
}
.panel-actions {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.panel-title {
  margin-bottom: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}
.header-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #667085;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(100px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-pill {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e2eef5;
  background: #f8fbff;
}
.summary-pill.warning {
  background: #fff9f0;
  border-color: #ffe0b2;
}
.summary-pill.info {
  background: #f4f7ff;
  border-color: #d6e4ff;
}
.summary-pill span {
  display: block;
  font-size: 12px;
  color: #667085;
}
.summary-pill strong {
  display: block;
  margin-top: 6px;
  color: #1f2d3d;
}
.status-help {
  margin-bottom: 12px;
}
.toolbar {
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.resource-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
code {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
