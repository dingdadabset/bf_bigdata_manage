<template>
  <div class="cluster-management">
    <div class="page-header">
      <h1>集群管理</h1>
      <a-button type="primary" icon="plus" @click="showCreateModal">新建集群</a-button>
    </div>

    <a-table :columns="columns" :data-source="clusters" :loading="loading" rowKey="id">
      <template slot="status" slot-scope="text">
        <a-tag :color="text === 'ACTIVE' ? 'green' : 'red'">
          {{ text }}
        </a-tag>
      </template>
      <template slot="action" slot-scope="text, record">
        <a-space>
          <a-button type="link" @click="editCluster(record)">编辑</a-button>
          <a-popconfirm title="确定要删除这个集群吗?" @confirm="deleteCluster(record.id)">
            <a-button type="link" style="color: red">删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </a-table>

    <!-- Create/Edit Modal -->
    <a-modal
      :title="editingId ? '编辑集群' : '新建集群'"
      :visible="modalVisible"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
      :confirmLoading="submitting"
    >
      <a-form-model :model="form" :rules="rules" ref="clusterForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="集群名称" prop="clusterName">
          <a-input v-model="form.clusterName" placeholder="例如: CDH-Cluster-01" :disabled="!!editingId" />
        </a-form-model-item>
        <a-form-model-item label="类型" prop="type">
          <a-select v-model="form.type" placeholder="选择集群类型">
            <a-select-option value="CDH">CDH</a-select-option>
            <a-select-option value="HDP">HDP</a-select-option>
            <a-select-option value="EMR">EMR</a-select-option>
            <a-select-option value="StarRocks">StarRocks</a-select-option>
            <a-select-option value="K8s">Kubernetes</a-select-option>
            <a-select-option value="Other">Other</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status">
            <a-select-option value="ACTIVE">ACTIVE</a-select-option>
            <a-select-option value="INACTIVE">INACTIVE</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-textarea v-model="form.description" :rows="3" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ClusterManagement',
  data() {
    return {
      loading: false,
      clusters: [],
      modalVisible: false,
      submitting: false,
      editingId: null,
      form: {
        clusterName: '',
        type: 'CDH',
        status: 'ACTIVE',
        description: ''
      },
      rules: {
        clusterName: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择集群类型', trigger: 'change' }]
      },
      columns: [
        { title: 'ID', dataIndex: 'id', width: 80 },
        { title: '集群名称', dataIndex: 'clusterName' },
        { title: '类型', dataIndex: 'type' },
        { title: '描述', dataIndex: 'description' },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
        { title: '创建时间', dataIndex: 'createTime' },
        { title: '操作', key: 'action', scopedSlots: { customRender: 'action' } }
      ]
    };
  },
  created() {
    this.fetchClusters();
  },
  methods: {
    async fetchClusters() {
      this.loading = true;
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = res.data.filter(c => c.status !== 'DELETED'); // Client-side filter as backup
      } catch (e) {
        this.$message.error('加载集群列表失败');
      } finally {
        this.loading = false;
      }
    },
    showCreateModal() {
      this.editingId = null;
      this.form = {
        clusterName: '',
        type: 'CDH',
        status: 'ACTIVE',
        description: ''
      };
      this.modalVisible = true;
    },
    editCluster(record) {
      this.editingId = record.id;
      this.form = { ...record };
      this.modalVisible = true;
    },
    handleSubmit() {
      this.$refs.clusterForm.validate(async valid => {
        if (valid) {
          this.submitting = true;
          try {
            if (this.editingId) {
              await axios.put(`/api/clusters/${this.editingId}`, this.form);
              this.$message.success('更新成功');
            } else {
              await axios.post('/api/clusters', this.form);
              this.$message.success('创建成功');
            }
            this.modalVisible = false;
            this.fetchClusters();
          } catch (e) {
            this.$message.error('操作失败: ' + (e.response?.data?.message || e.message));
          } finally {
            this.submitting = false;
          }
        }
      });
    },
    async deleteCluster(id) {
      try {
        await axios.delete(`/api/clusters/${id}`);
        this.$message.success('删除成功');
        this.fetchClusters();
      } catch (e) {
        this.$message.error('删除失败');
      }
    }
  }
};
</script>

<style scoped>
.cluster-management {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
</style>
