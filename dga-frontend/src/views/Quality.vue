<template>
  <div class="quality-view">
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" tab="规则配置">
        <div class="table-operations">
          <a-button type="primary" icon="plus" @click="showModal">新建规则</a-button>
        </div>
        <a-table :columns="ruleColumns" :data-source="rules" :loading="loadingRules" row-key="id" :scroll="{ x: 800 }">
          <span slot="action" slot-scope="text, record">
            <a-button type="link" @click="executeRule(record.id)">立即执行</a-button>
          </span>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="2" tab="执行记录">
        <a-table :columns="executionColumns" :data-source="executions" :loading="loadingExecutions" row-key="id" :scroll="{ x: 800 }">
          <span slot="status" slot-scope="text">
            <a-badge :status="text === 'SUCCESS' ? 'success' : 'error'" :text="text" />
          </span>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal v-model="visible" title="新建质量规则" @ok="handleOk">
      <a-form-model :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="规则类型">
          <a-select v-model="form.ruleType">
            <a-select-option value="NULL_CHECK">空值检测</a-select-option>
            <a-select-option value="UNIQUENESS">唯一性检测</a-select-option>
            <a-select-option value="VALUE_RANGE">数值范围</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="表ID">
          <a-input v-model="form.tableId" />
        </a-form-model-item>
        <a-form-model-item label="字段名">
          <a-input v-model="form.columnName" />
        </a-form-model-item>
        <a-form-model-item label="阈值">
          <a-input-number v-model="form.threshold" :min="0" :max="1" step="0.01" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import moment from 'moment';

export default {
  data() {
    return {
      loadingRules: false,
      loadingExecutions: false,
      rules: [],
      executions: [],
      visible: false,
      form: {
        ruleType: 'NULL_CHECK',
        tableId: '',
        columnName: '',
        threshold: 0.05
      },
      ruleColumns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '规则类型', dataIndex: 'ruleType' },
        { title: '表ID', dataIndex: 'tableId' },
        { title: '字段', dataIndex: 'columnName' },
        { title: '阈值', dataIndex: 'threshold' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      executionColumns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '规则ID', dataIndex: 'ruleId' },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
        { title: '结果值', dataIndex: 'resultValue' },
        { title: '执行时间', dataIndex: 'executedAt', customRender: (text) => moment(text).format('YYYY-MM-DD HH:mm:ss') }
      ]
    };
  },
  mounted() {
    this.fetchRules();
    this.fetchExecutions();
  },
  methods: {
    async fetchRules() {
      this.loadingRules = true;
      try {
        const response = await axios.get('/api/quality/rules');
        this.rules = response.data;
      } catch (e) {
        this.$message.error('获取规则失败');
      } finally {
        this.loadingRules = false;
      }
    },
    async fetchExecutions() {
      this.loadingExecutions = true;
      try {
        const response = await axios.get('/api/quality/executions');
        this.executions = response.data;
      } catch (e) {
        this.$message.error('获取执行记录失败');
      } finally {
        this.loadingExecutions = false;
      }
    },
    showModal() {
      this.visible = true;
    },
    async handleOk() {
      try {
        await axios.post('/api/quality/rules', this.form);
        this.$message.success('规则创建成功');
        this.visible = false;
        this.fetchRules();
      } catch (e) {
        this.$message.error('创建失败');
      }
    },
    async executeRule(id) {
      try {
        await axios.post(`/api/quality/execute/${id}`);
        this.$message.success('执行指令已发送');
        this.fetchExecutions();
      } catch (e) {
        this.$message.error('执行失败');
      }
    }
  }
};
</script>

<style scoped>
.table-operations {
  margin-bottom: 16px;
}
</style>
