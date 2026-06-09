<template>
  <a-modal
    :visible="visible"
    title="StarRocks 权限盘点"
    width="1120px"
    :confirm-loading="loading"
    ok-text="开始盘点"
    @ok="$emit('run')"
    @cancel="$emit('cancel')"
  >
    <a-alert
      class="inventory-alert"
      type="info"
      show-icon
      message="权限盘点原型"
      :description="`面向 ${clusterLabel || '当前集群'} 拉取用户当前 live 权限，并按权限组合输出模板候选。`"
    />

    <div class="inventory-toolbar">
      <a-input-search
        :value="form.keyword"
        allow-clear
        placeholder="按用户名过滤，可为空"
        style="max-width: 300px"
        @change="updateField('keyword', $event && $event.target ? $event.target.value : '')"
        @search="$emit('run')"
      />
      <a-input-number
        :value="form.maxUsers"
        :min="1"
        :max="200"
        style="width: 120px"
        @change="updateField('maxUsers', $event)"
      />
      <a-checkbox
        :checked="form.includeRecordedGrants"
        @change="updateField('includeRecordedGrants', $event && $event.target ? $event.target.checked : false)"
      >
        对比 recorded 权限
      </a-checkbox>
      <a-button icon="reload" :loading="loading" @click="$emit('run')">刷新</a-button>
    </div>

    <a-empty v-if="!result" description="点击“开始盘点”查看用户权限画像和模板候选" />

    <div v-else class="inventory-result">
      <div class="inventory-summary">
        <a-tag color="blue">集群 {{ result.cluster || '-' }}</a-tag>
        <a-tag color="purple">后端 {{ result.authBackend || '-' }}</a-tag>
        <a-tag color="green">成功 {{ result.successCount || 0 }}</a-tag>
        <a-tag color="orange">失败 {{ result.failedCount || 0 }}</a-tag>
        <a-tag color="cyan">模板候选 {{ result.templateCandidateCount || 0 }}</a-tag>
      </div>

      <a-alert
        class="inventory-message"
        :type="result.failedCount ? 'warning' : 'success'"
        show-icon
        :message="result.message || '盘点完成'"
      />

      <a-tabs default-active-key="templates">
        <a-tab-pane key="templates" :tab="`模板候选 (${templateCandidates.length})`">
          <a-table
            size="small"
            row-key="permissionSignature"
            :columns="templateColumns"
            :data-source="templateCandidates"
            :pagination="{ pageSize: 6 }"
          >
            <template slot="templateCode" slot-scope="text, record">
              <div class="template-code">{{ record.templateCodeSuggestion }}</div>
              <div class="template-subtitle">{{ record.permissionCategory || '-' }} / 权限 {{ record.grantCount || 0 }}</div>
            </template>
            <template slot="users" slot-scope="text, record">
              <div class="tag-list">
                <a-tag
                  v-for="item in (record.usernames || []).slice(0, 5)"
                  :key="item"
                  color="blue"
                >
                  {{ item }}
                </a-tag>
                <a-tag v-if="(record.usernames || []).length > 5">+{{ (record.usernames || []).length - 5 }}</a-tag>
              </div>
            </template>
            <template slot="summary" slot-scope="text, record">
              <div class="summary-list">
                <div
                  v-for="item in (record.permissionSummary || []).slice(0, 4)"
                  :key="item"
                  class="summary-item"
                >
                  {{ item }}
                </div>
                <div v-if="(record.permissionSummary || []).length > 4" class="summary-more">
                  另有 {{ (record.permissionSummary || []).length - 4 }} 项
                </div>
              </div>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="users" :tab="`用户画像 (${users.length})`">
          <a-table
            size="small"
            row-key="username"
            :columns="userColumns"
            :data-source="users"
            :pagination="{ pageSize: 8 }"
          >
            <template slot="username" slot-scope="text, record">
              <div class="template-code">{{ record.username }}</div>
              <div class="template-subtitle">{{ record.permissionCategory || '-' }} / 风险 {{ record.highRiskGrantCount || 0 }}</div>
            </template>
            <template slot="counts" slot-scope="text, record">
              <a-tag color="blue">live {{ record.grantCount || 0 }}</a-tag>
              <a-tag color="green">匹配 {{ record.matchedRecordedCount || 0 }}</a-tag>
              <a-tag color="orange">live-only {{ record.liveOnlyCount || 0 }}</a-tag>
              <a-tag color="default">recorded-only {{ record.recordedOnlyCount || 0 }}</a-tag>
            </template>
            <template slot="summary" slot-scope="text, record">
              <div class="summary-list">
                <div
                  v-for="item in (record.permissionSummary || []).slice(0, 4)"
                  :key="item"
                  class="summary-item"
                >
                  {{ item }}
                </div>
                <div v-if="(record.permissionSummary || []).length > 4" class="summary-more">
                  另有 {{ (record.permissionSummary || []).length - 4 }} 项
                </div>
              </div>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane v-if="failures.length" key="failures" :tab="`失败项 (${failures.length})`">
          <a-table
            size="small"
            row-key="username"
            :columns="failureColumns"
            :data-source="failures"
            :pagination="{ pageSize: 6 }"
          />
        </a-tab-pane>
      </a-tabs>
    </div>
  </a-modal>
</template>

<script>
export default {
  name: 'StarRocksInventoryModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    clusterLabel: {
      type: String,
      default: ''
    },
    form: {
      type: Object,
      default: () => ({})
    },
    result: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      templateColumns: [
        { title: '模板建议', key: 'templateCode', scopedSlots: { customRender: 'templateCode' } },
        { title: '覆盖用户', key: 'users', width: 260, scopedSlots: { customRender: 'users' } },
        { title: '高风险项', dataIndex: 'highRiskGrantCount', key: 'highRiskGrantCount', width: 100 },
        { title: '权限摘要', key: 'summary', scopedSlots: { customRender: 'summary' } }
      ],
      userColumns: [
        { title: '用户', key: 'username', width: 220, scopedSlots: { customRender: 'username' } },
        { title: '权限计数', key: 'counts', width: 340, scopedSlots: { customRender: 'counts' } },
        { title: '权限摘要', key: 'summary', scopedSlots: { customRender: 'summary' } }
      ],
      failureColumns: [
        { title: '用户', dataIndex: 'username', key: 'username', width: 180 },
        { title: '失败原因', dataIndex: 'message', key: 'message' }
      ]
    };
  },
  computed: {
    templateCandidates() {
      return Array.isArray(this.result?.templateCandidates) ? this.result.templateCandidates : [];
    },
    users() {
      return Array.isArray(this.result?.users) ? this.result.users : [];
    },
    failures() {
      return Array.isArray(this.result?.failures) ? this.result.failures : [];
    }
  },
  methods: {
    updateField(field, value) {
      this.$emit('change-form', { field, value });
    }
  }
};
</script>

<style scoped>
.inventory-alert {
  margin-bottom: 12px;
}
.inventory-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.inventory-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.inventory-message {
  margin-bottom: 12px;
}
.template-code {
  color: #1f2d3d;
  font-weight: 600;
}
.template-subtitle {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.summary-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.summary-item {
  color: #344054;
  font-size: 12px;
  line-height: 1.5;
}
.summary-more {
  color: #98a2b3;
  font-size: 12px;
}
</style>
