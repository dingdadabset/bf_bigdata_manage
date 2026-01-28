<template>
  <div class="metadata-view">
    <div class="table-operations">
      <a-button type="primary" @click="syncMetadata">
        <a-icon type="sync" /> 同步元数据
      </a-button>
      <a-input-search placeholder="搜索表名" style="width: 200px; margin-left: 16px" @search="onSearch" />
    </div>
    
    <a-table :columns="columns" :data-source="data" :loading="loading" row-key="id" style="margin-top: 16px" :scroll="{ x: 1000 }">
      <span slot="storageFormat" slot-scope="text">
        <a-tag color="blue">{{ text }}</a-tag>
      </span>
      <span slot="size" slot-scope="text">
        {{ formatSize(text) }}
      </span>
    </a-table>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      loading: false,
      data: [],
      columns: [
        { title: '数据库', dataIndex: 'dbName', key: 'dbName' },
        { title: '表名', dataIndex: 'tableName', key: 'tableName' },
        { title: '负责人', dataIndex: 'owner', key: 'owner' },
        { title: '存储格式', dataIndex: 'storageFormat', key: 'storageFormat', scopedSlots: { customRender: 'storageFormat' } },
        { title: '大小', dataIndex: 'totalSize', key: 'totalSize', scopedSlots: { customRender: 'size' } },
        { title: '记录数', dataIndex: 'recordCount', key: 'recordCount' },
        { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' }
      ]
    };
  },
  mounted() {
    this.fetchMetadata();
  },
  methods: {
    async fetchMetadata() {
      this.loading = true;
      try {
        const response = await axios.get('/api/metadata/tables');
        this.data = response.data;
      } catch (e) {
        this.$message.error('获取元数据失败');
      } finally {
        this.loading = false;
      }
    },
    async syncMetadata() {
      this.$message.loading({ content: '同步中...', key: 'sync' });
      try {
        await axios.post('/api/metadata/sync');
        this.$message.success({ content: '同步成功', key: 'sync' });
        this.fetchMetadata();
      } catch (e) {
        this.$message.error({ content: '同步失败', key: 'sync' });
      }
    },
    onSearch(val) {
      // Implement local search or backend search
      // For now, simple console log
      console.log('Search:', val);
    },
    formatSize(bytes) {
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
  }
};
</script>

<style scoped>
.table-operations {
  margin-bottom: 16px;
}
</style>
