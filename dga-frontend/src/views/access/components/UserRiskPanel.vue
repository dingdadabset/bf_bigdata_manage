<template>
  <div class="user-risk-panel">
    <div class="risk-summary-grid">
      <div class="risk-stat">
        <span>待处理风险</span>
        <strong>{{ openCount }}</strong>
      </div>
      <div class="risk-stat high">
        <span>高风险</span>
        <strong>{{ highCount }}</strong>
      </div>
      <div class="risk-stat">
        <span>无人负责</span>
        <strong>{{ typeCount('UNOWNED_PERMISSION') }}</strong>
      </div>
      <div class="risk-stat">
        <span>高权限复核</span>
        <strong>{{ typeCount('HIGH_PRIVILEGE_REVIEW') }}</strong>
      </div>
    </div>

    <a-card :bordered="false" class="risk-card">
      <div class="card-toolbar">
        <div>
          <h3>风险列表</h3>
          <p>只展示当前用户相关的核心风险，证据和建议在详情里处理。</p>
        </div>
        <div class="toolbar-actions">
          <a-select v-model="status" class="status-select" @change="fetchIssues">
            <a-select-option value="OPEN">待处理</a-select-option>
            <a-select-option value="CLAIMED">已认领</a-select-option>
            <a-select-option value="RESOLVED">已关闭</a-select-option>
            <a-select-option value="ALL">全部</a-select-option>
          </a-select>
          <a-button icon="reload" :loading="loading" @click="fetchIssues">刷新</a-button>
        </div>
      </div>

      <a-table
        row-key="id"
        class="modern-table"
        :columns="columns"
        :data-source="issues"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="riskRowClassName"
        :scroll="{ x: 760 }"
        @change="onTableChange"
      >
        <template slot="expandedRowRender" slot-scope="record">
          <div class="expanded-risk">
            <a-collapse>
              <a-collapse-panel key="evidence" header="证据">
                <p>{{ record.evidence || '暂无证据' }}</p>
              </a-collapse-panel>
              <a-collapse-panel key="recommendation" header="建议">
                <p>{{ record.recommendation || '暂无建议' }}</p>
              </a-collapse-panel>
            </a-collapse>
          </div>
        </template>
        <template slot="issueType" slot-scope="text">
          <a-tag :color="issueTypeColor(text)">{{ issueTypeLabel(text) }}</a-tag>
        </template>
        <template slot="severity" slot-scope="text">
          <a-tag :class="{ 'high-risk-tag': String(text || '').toUpperCase() === 'HIGH' }" :color="severityColor(text)">
            {{ severityLabel(text) }}
          </a-tag>
        </template>
        <template slot="resource" slot-scope="text, record">
          <div class="resource-name">{{ resourceName(record) }}</div>
          <div class="muted">{{ record.username }}</div>
        </template>
        <template slot="permission" slot-scope="text">
          <a-tag v-if="text" color="orange">{{ text }}</a-tag>
          <span v-else class="muted">-</span>
        </template>
        <template slot="status" slot-scope="text">
          <a-tag :color="statusColor(text)">{{ statusLabel(text) }}</a-tag>
        </template>
        <template slot="action" slot-scope="text, record">
          <a-button type="link" icon="eye" @click="openDrawer(record)">详情</a-button>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      :visible="drawerVisible"
      width="520"
      title="风险详情"
      @close="drawerVisible = false"
    >
      <template v-if="currentIssue">
        <div class="drawer-title-row">
          <a-tag :color="issueTypeColor(currentIssue.issueType)">{{ issueTypeLabel(currentIssue.issueType) }}</a-tag>
          <a-tag :color="severityColor(currentIssue.severity)">{{ severityLabel(currentIssue.severity) }}</a-tag>
        </div>
        <a-descriptions size="small" :column="1" bordered>
          <a-descriptions-item label="账号">{{ currentIssue.username || '-' }}</a-descriptions-item>
          <a-descriptions-item label="资源">{{ resourceName(currentIssue) }}</a-descriptions-item>
          <a-descriptions-item label="权限">{{ currentIssue.permission || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ statusLabel(currentIssue.status) }}</a-descriptions-item>
          <a-descriptions-item label="发现时间">{{ formatTime(currentIssue.detectedAt) }}</a-descriptions-item>
        </a-descriptions>
        <a-collapse class="detail-collapse" :default-active-key="['evidence']">
          <a-collapse-panel key="evidence" header="证据">
            <p>{{ currentIssue.evidence || '暂无证据' }}</p>
          </a-collapse-panel>
          <a-collapse-panel key="recommendation" header="建议">
            <p>{{ currentIssue.recommendation || '暂无建议' }}</p>
          </a-collapse-panel>
        </a-collapse>
      </template>
    </a-drawer>
  </div>
</template>

<script>
import axios from 'axios';
import moment from 'moment';

export default {
  name: 'UserRiskPanel',
  props: {
    user: {
      type: Object,
      default: null
    },
    cluster: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      issues: [],
      loading: false,
      status: 'OPEN',
      drawerVisible: false,
      currentIssue: null,
      pagination: {
        current: 1,
        pageSize: 8,
        total: 0,
        showTotal: total => `共 ${total} 条`
      },
      columns: [
        { title: '风险类型', dataIndex: 'issueType', key: 'issueType', scopedSlots: { customRender: 'issueType' }, width: 160 },
        { title: '级别', dataIndex: 'severity', key: 'severity', scopedSlots: { customRender: 'severity' }, width: 100 },
        { title: '资源', key: 'resource', scopedSlots: { customRender: 'resource' } },
        { title: '权限', dataIndex: 'permission', key: 'permission', scopedSlots: { customRender: 'permission' }, width: 110 },
        { title: '状态', dataIndex: 'status', key: 'status', scopedSlots: { customRender: 'status' }, width: 110 },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' }, width: 110, fixed: 'right' }
      ]
    };
  },
  computed: {
    openCount() {
      return this.issues.filter(item => item.status !== 'RESOLVED').length;
    },
    highCount() {
      return this.issues.filter(item => String(item.severity || '').toUpperCase() === 'HIGH').length;
    }
  },
  watch: {
    user: {
      immediate: true,
      handler() {
        this.pagination.current = 1;
        this.fetchIssues();
      }
    },
    cluster() {
      this.pagination.current = 1;
      this.fetchIssues();
    }
  },
  methods: {
    async fetchIssues() {
      if (!this.user || !this.user.username) {
        this.issues = [];
        this.pagination.total = 0;
        return;
      }
      this.loading = true;
      try {
        const res = await axios.get('/api/access/governance/issues', {
          params: {
            cluster: this.cluster || undefined,
            username: this.user.username,
            status: this.status,
            page: this.pagination.current - 1,
            size: this.pagination.pageSize
          }
        });
        this.issues = res.data?.content || [];
        this.pagination.total = res.data?.totalElements || 0;
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载用户风险失败');
      } finally {
        this.loading = false;
      }
    },
    onTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.fetchIssues();
    },
    openDrawer(record) {
      this.currentIssue = record;
      this.drawerVisible = true;
    },
    riskRowClassName(record) {
      return String(record.severity || '').toUpperCase() === 'HIGH' ? 'high-risk-row' : '';
    },
    typeCount(type) {
      return this.issues.filter(item => item.issueType === type).length;
    },
    resourceName(record) {
      const db = record.databaseName || record.database || '-';
      const table = record.tableName || record.table;
      return table ? `${db}.${table}` : db;
    },
    issueTypeLabel(type) {
      const labels = {
        UNUSED_DATABASE_PERMISSION: '未使用权限',
        HIGH_PRIVILEGE_REVIEW: '高权限复核',
        UNOWNED_PERMISSION: '无人负责'
      };
      return labels[type] || type || '-';
    },
    issueTypeColor(type) {
      if (type === 'HIGH_PRIVILEGE_REVIEW') return 'red';
      if (type === 'UNUSED_DATABASE_PERMISSION') return 'orange';
      return 'blue';
    },
    severityLabel(value) {
      const text = String(value || '').toUpperCase();
      return { HIGH: '高', MEDIUM: '中', LOW: '低' }[text] || text || '-';
    },
    severityColor(value) {
      const text = String(value || '').toUpperCase();
      if (text === 'HIGH') return 'red';
      if (text === 'MEDIUM') return 'orange';
      return 'green';
    },
    statusLabel(value) {
      const text = String(value || '').toUpperCase();
      return { OPEN: '待处理', CLAIMED: '已认领', RESOLVED: '已关闭' }[text] || text || '-';
    },
    statusColor(value) {
      const text = String(value || '').toUpperCase();
      if (text === 'OPEN') return 'red';
      if (text === 'CLAIMED') return 'blue';
      return 'green';
    },
    formatTime(value) {
      return value ? moment(value).format('YYYY-MM-DD HH:mm') : '-';
    }
  }
};
</script>

<style scoped>
.user-risk-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.risk-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
}
.risk-stat {
  padding: 16px;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}
.risk-stat span {
  display: block;
  color: #667085;
  margin-bottom: 8px;
}
.risk-stat strong {
  font-size: 24px;
  color: #1f2937;
}
.risk-stat.high strong {
  color: #cf1322;
}
.risk-stat.high {
  border-color: #ffccc7;
  background: #fff7f6;
}
.risk-card {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.04);
}
.card-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.card-toolbar h3 {
  margin: 0 0 4px;
}
.card-toolbar p,
.muted {
  margin: 0;
  color: #98a2b3;
}
.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.status-select {
  width: 128px;
}
.resource-name {
  font-weight: 600;
  color: #1f2937;
}
.modern-table ::v-deep .ant-table-tbody > tr:nth-child(odd) > td {
  background: #fcfcfd;
}
.modern-table ::v-deep .ant-table-thead > tr > th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #f8fafc;
}
.modern-table ::v-deep .high-risk-row > td {
  background: #fff7f6 !important;
}
.high-risk-tag {
  font-weight: 700;
}
.expanded-risk {
  padding: 8px 24px;
  background: #f9fafb;
}
.expanded-risk p {
  margin: 0;
  color: #475467;
}
.drawer-title-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.detail-collapse {
  margin-top: 16px;
}
@media (max-width: 900px) {
  .risk-summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .card-toolbar {
    flex-direction: column;
  }
}
</style>
