<template>
  <div class="ds-management">
    <a-card title="Data Source Management" :bordered="false">
      <div class="table-operations">
        <a-button type="primary" @click="showAddModal">Add Data Source</a-button>
      </div>
      
      <a-table :columns="columns" :data-source="dataSources" :rowKey="record => record.id" :scroll="{ x: 1000 }">
        <span slot="action" slot-scope="text, record">
          <a-button type="link" @click="testConnection(record)">Test Connection</a-button>
          <a-divider type="vertical" />
          <a-button type="link" @click="collectMetadata(record)">Sync Metadata</a-button>
        </span>
      </a-table>
    </a-card>

    <a-modal v-model="visible" title="Add Data Source" @ok="handleOk">
      <a-form-model :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="Name">
          <a-input v-model="form.name" />
        </a-form-model-item>
        <a-form-model-item label="Type">
          <a-select v-model="form.type">
            <a-select-option value="HIVE">Hive (MySQL Metastore)</a-select-option>
            <a-select-option value="STARROCKS">StarRocks</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="JDBC URL">
          <a-input v-model="form.url" placeholder="jdbc:mysql://host:3306/hive_metastore" />
        </a-form-model-item>
        <a-form-model-item label="Username">
          <a-input v-model="form.username" />
        </a-form-model-item>
        <a-form-model-item label="Password">
          <a-input-password v-model="form.password" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      dataSources: [],
      visible: false,
      form: {
        name: '',
        type: 'HIVE',
        url: '',
        username: '',
        password: ''
      },
      columns: [
        { title: 'ID', dataIndex: 'id', key: 'id' },
        { title: 'Name', dataIndex: 'name', key: 'name' },
        { title: 'Type', dataIndex: 'type', key: 'type' },
        { title: 'URL', dataIndex: 'url', key: 'url' },
        { title: 'Created At', dataIndex: 'createdAt', key: 'createdAt' },
        { title: 'Action', key: 'action', scopedSlots: { customRender: 'action' } },
      ],
    };
  },
  mounted() {
    this.fetchDataSources();
  },
  methods: {
    async fetchDataSources() {
      try {
        const response = await axios.get('/api/datasource');
        this.dataSources = response.data;
      } catch (error) {
        this.$message.error('Failed to load data sources');
      }
    },
    showAddModal() {
      this.visible = true;
      this.form = { name: '', type: 'HIVE', url: '', username: '', password: '' };
    },
    async handleOk() {
      try {
        await axios.post('/api/datasource', this.form);
        this.$message.success('Data source added successfully');
        this.visible = false;
        this.fetchDataSources();
      } catch (error) {
        this.$message.error('Failed to add data source');
      }
    },
    async testConnection(record) {
      const hide = this.$message.loading('Testing connection...', 0);
      try {
        const response = await axios.post('/api/datasource/test-connection', record);
        hide();
        if (response.data) {
          this.$message.success('Connection successful!');
        } else {
          this.$message.error('Connection failed!');
        }
      } catch (error) {
        hide();
        this.$message.error('Error testing connection');
      }
    },
    async collectMetadata(record) {
      const hide = this.$message.loading('Triggering collection...', 0);
      try {
        await axios.post(`/api/datasource/collect/${record.id}`);
        hide();
        this.$message.success('Metadata collection started!');
      } catch (error) {
        hide();
        this.$message.error('Failed to start collection');
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
