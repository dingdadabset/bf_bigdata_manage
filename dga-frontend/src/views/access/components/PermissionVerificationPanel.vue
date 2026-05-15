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
        <a-tag :color="verificationStatusColor(text)">{{ text }}</a-tag>
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
