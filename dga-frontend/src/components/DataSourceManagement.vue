<template>
  <div class="ds-management">
    <!-- Hero Header -->
    <div class="hero-header">
      <div class="header-content">
        <h1 class="page-title">数据源管理</h1>
        <div class="search-bar">
          <a-input-search
            v-model="searchQuery"
            placeholder="搜索数据源名称或类型..."
            size="large"
            allow-clear
          >
            <a-icon slot="prefix" type="search" />
          </a-input-search>
        </div>
        <div class="header-actions">
          <a-button type="primary" size="large" icon="plus" @click="showAddDrawer" class="action-btn">
            新建数据源
          </a-button>
        </div>
      </div>
      
      <!-- Stats Overview -->
      <div class="stats-overview">
        <div class="stat-item">
          <span class="stat-label">总数据源</span>
          <span class="stat-value">{{ dataSources.length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正常运行</span>
          <span class="stat-value success">{{ healthyCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">异常状态</span>
          <span class="stat-value error">{{ errorCount }}</span>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="content-wrapper">
      <div v-if="loading" class="loading-state">
        <a-spin size="large" />
      </div>

      <div v-else-if="filteredDataSources.length === 0" class="empty-state">
        <a-empty description="暂无数据源，请点击上方按钮新建" />
      </div>

      <div v-else class="datasource-grid">
        <div v-for="item in filteredDataSources" :key="item.id" class="ds-card-wrapper">
          <div class="ds-card">
            <div class="card-header">
              <div class="ds-logo">
                <img v-if="getIcon(item.type)" :src="getIcon(item.type)" :alt="item.type" />
                <span v-else>{{ item.type.substring(0, 2) }}</span>
              </div>
              <div class="ds-info">
                <div class="ds-name-row">
                  <span class="ds-name" :title="item.name">{{ item.name }}</span>
                  <a-tag :color="getStatusColor(item.status)" class="status-tag">
                    {{ getStatusLabel(item.status) }}
                  </a-tag>
                </div>
                <div class="ds-meta">
                  {{ item.type }} • {{ item.url }}
                </div>
              </div>
              <a-dropdown :trigger="['click']">
                <a-button type="link" icon="more" class="more-btn" @click.stop />
                <a-menu slot="overlay">
                  <a-menu-item @click="showEditDrawer(item)">
                    <a-icon type="edit" /> 编辑配置
                  </a-menu-item>
                  <a-menu-item @click="copyDataSource(item)">
                    <a-icon type="copy" /> 复制配置
                  </a-menu-item>
                  <a-menu-item @click="deleteDataSource(item)">
                    <a-icon type="delete" /> 删除数据源
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
            
            <div class="card-body">
              <div class="desc-text">{{ item.description || '暂无描述' }}</div>
              <div class="sync-info">
                上次同步: {{ item.lastSyncTime || '-' }}
              </div>
            </div>

            <div class="card-footer">
              <a-button type="link" size="small" icon="thunderbolt" @click="testConnection(item)">
                测试连通性
              </a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" icon="sync" @click="collectMetadata(item)">
                同步元数据
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Drawer -->
    <a-drawer
      :title="editingId ? '编辑数据源' : '新建数据源'"
      :width="520"
      :visible="drawerVisible"
      :body-style="{ paddingBottom: '80px' }"
      @close="onDrawerClose"
    >
      <a-form-model ref="form" :model="form" :rules="rules" layout="vertical">
        <a-form-model-item label="数据源名称" prop="name">
          <a-input v-model="form.name" placeholder="请输入数据源名称" />
        </a-form-model-item>
        
        <a-form-model-item label="数据源类型" prop="type">
          <a-select v-model="form.type" placeholder="请选择类型" :disabled="!!editingId">
            <a-select-option value="HIVE">
              <a-icon type="database" /> Hive
            </a-select-option>
            <a-select-option value="STARROCKS">
              <a-icon type="rocket" /> StarRocks
            </a-select-option>
            <a-select-option value="MYSQL">
              <a-icon type="hdd" /> MySQL
            </a-select-option>
          </a-select>
        </a-form-model-item>

        <a-divider orientation="left">连接配置</a-divider>

        <a-form-model-item label="JDBC URL" prop="url">
          <a-input v-model="form.url" placeholder="jdbc:mysql://host:3306/db" />
        </a-form-model-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-model-item label="用户名" prop="username">
              <a-input v-model="form.username" placeholder="Username" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="密码" prop="password">
              <a-input-password v-model="form.password" placeholder="Password" />
            </a-form-model-item>
          </a-col>
        </a-row>

        <a-form-model-item label="描述" prop="description">
          <a-textarea v-model="form.description" :rows="3" placeholder="可选描述信息" />
        </a-form-model-item>
      </a-form-model>

      <div class="drawer-footer">
        <a-button :style="{ marginRight: '8px' }" @click="onDrawerClose">
          取消
        </a-button>
        <a-button type="dashed" :style="{ marginRight: '8px' }" :loading="testingConnection" @click="handleTestConnection">
          测试连通性
        </a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </a-button>
      </div>
    </a-drawer>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'DataSourceManagement',
  data() {
    return {
      loading: false,
      dataSources: [],
      searchQuery: '',
      
      // Drawer state
      drawerVisible: false,
      editingId: null,
      submitting: false,
      testingConnection: false,
      
      form: {
        name: '',
        type: 'HIVE',
        url: '',
        username: '',
        password: '',
        description: ''
      },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择类型', trigger: 'change' }],
        url: [{ required: true, message: '请输入JDBC URL', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
      }
    };
  },
  computed: {
    filteredDataSources() {
      if (!this.searchQuery) return this.dataSources;
      const q = this.searchQuery.toLowerCase();
      return this.dataSources.filter(ds => 
        ds.name.toLowerCase().includes(q) || 
        ds.type.toLowerCase().includes(q)
      );
    },
    healthyCount() {
      return this.dataSources.filter(ds => ds.status === 'HEALTHY').length;
    },
    errorCount() {
      return this.dataSources.filter(ds => ds.status === 'ERROR').length;
    },
    isAdmin() {
      try {
        const userStr = localStorage.getItem('user');
        if (!userStr) return false;
        const data = JSON.parse(userStr);
        // Handle nested user object structure from backend response
        const user = data.user || data;
        
        // Check for various admin indicators based on backend/mock structure
        return user.isAdmin === 1 || user.role === 'Admin' || user.username === 'admin';
      } catch (e) {
        return false;
      }
    }
  },
  mounted() {
    this.fetchDataSources();
  },
  methods: {
    async fetchDataSources() {
      this.loading = true;
      try {
        const response = await axios.get('/api/datasource');
        // Mock status for demo purposes if not present
        this.dataSources = response.data.map(ds => ({
          ...ds,
          status: ds.status || 'UNKNOWN'
        }));
      } catch (error) {
        this.$message.error('获取数据源列表失败');
      } finally {
        this.loading = false;
      }
    },
    
    getIcon(type) {
      const t = (type || '').toUpperCase();
      if (t === 'MYSQL') {
        return 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg';
      }
      if (t === 'HIVE') {
        return 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/apache/apache-original-wordmark.svg';
      }
      if (t === 'STARROCKS') {
        return 'https://avatars.githubusercontent.com/u/56217375?s=200&v=4';
      }
      return null;
    },
    
    getStatusColor(status) {
      if (status === 'HEALTHY') return 'green';
      if (status === 'ERROR') return 'red';
      return 'default';
    },
    
    getStatusLabel(status) {
      if (status === 'HEALTHY') return '正常';
      if (status === 'ERROR') return '异常';
      return '未知';
    },

    showAddDrawer() {
      this.editingId = null;
      this.form = { name: '', type: 'HIVE', url: '', username: '', password: '', description: '' };
      this.drawerVisible = true;
    },

    showEditDrawer(item) {
      this.editingId = item.id;
      this.form = { ...item, password: '' }; // Don't fill password for security, or handle it properly
      this.drawerVisible = true;
    },

    copyDataSource(item) {
      this.editingId = null;
      this.form = {
        ...item,
        id: undefined,
        name: `${item.name}_copy`,
        password: ''
      };
      this.drawerVisible = true;
    },

    onDrawerClose() {
      this.drawerVisible = false;
    },

    async handleTestConnection() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          this.testingConnection = true;
          try {
            const res = await axios.post('/api/datasource/test-connection', this.form);
            if (res.data) {
              this.$message.success('连接测试成功');
            } else {
              this.$message.error('连接测试失败');
            }
          } catch (e) {
            this.$message.error('连接测试发生错误');
          } finally {
            this.testingConnection = false;
          }
        }
      });
    },

    async handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          this.submitting = true;
          try {
            if (this.editingId) {
              // Update logic would go here if API supports it
              await axios.post('/api/datasource', this.form); // Fallback to add for now as update endpoint wasn't explicit
            } else {
              await axios.post('/api/datasource', this.form);
            }
            this.$message.success('保存成功');
            this.drawerVisible = false;
            this.fetchDataSources();
          } catch (e) {
            this.$message.error('保存失败');
          } finally {
            this.submitting = false;
          }
        }
      });
    },

    async testConnection(item) {
      const hide = this.$message.loading('正在测试连接...', 0);
      try {
        const res = await axios.post('/api/datasource/test-connection', item);
        hide();
        if (res.data) {
          this.$message.success('连接正常');
          // Update local status
          item.status = 'HEALTHY';
        } else {
          this.$message.error('连接失败');
          item.status = 'ERROR';
        }
      } catch (e) {
        hide();
        this.$message.error('测试出错');
        item.status = 'ERROR';
      }
    },

    async collectMetadata(item) {
      const hide = this.$message.loading('正在触发采集...', 0);
      try {
        await axios.post(`/api/datasource/collect/${item.id}`);
        hide();
        this.$message.success('元数据采集任务已提交');
      } catch (e) {
        hide();
        this.$message.error('采集触发失败');
      }
    },
    
    async deleteDataSource(item) {
      if (!this.isAdmin) {
        this.$message.warning('只有管理员可以删除数据源');
        return;
      }
      
      this.$confirm({
        title: '确认删除数据源?',
        content: `将删除数据源 "${item.name}" 及其相关的元数据信息，此操作不可恢复。`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          try {
            await axios.delete(`/api/datasource/${item.id}`);
            this.$message.success('数据源已删除');
            this.fetchDataSources();
          } catch (e) {
            this.$message.error('删除失败: ' + (e.response?.data?.message || '未知错误'));
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.ds-management {
  min-height: 100vh;
  background-color: #f8f9fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* Hero Header */
.hero-header {
  background: #fff;
  padding: 32px 40px;
  border-bottom: 1px solid #eaeaea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.search-bar {
  flex: 1;
  max-width: 480px;
  margin: 0 32px;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
}

.stats-overview {
  display: flex;
  gap: 48px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  line-height: 1;
}

.stat-value.success { color: #52c41a; }
.stat-value.error { color: #ff4d4f; }

/* Content */
.content-wrapper {
  padding: 32px 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.datasource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

/* DS Card */
.ds-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ds-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.06);
  border-color: #d9d9d9;
}

.card-header {
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.ds-logo {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #595959;
  flex-shrink: 0;
}

.ds-logo.hive { background: #fffbe6; color: #fa8c16; }
.ds-logo.starrocks { background: #e6f7ff; color: #1890ff; }
.ds-logo.mysql { background: #f0f5ff; color: #2f54eb; }

.ds-info {
  flex: 1;
  min-width: 0;
}

.ds-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.status-tag {
  border-radius: 12px;
  padding: 0 8px;
}

.ds-source-tag {
  font-size: 10px;
  line-height: 18px;
  height: 20px;
  padding: 0 8px;
  border-radius: 12px;
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  color: #fa8c16;
}

.ds-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ds-meta {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-btn {
  color: #8c8c8c;
  padding: 0;
}

.card-body {
  padding: 16px 20px;
  flex: 1;
}

.desc-text {
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px;
}

.sync-info {
  margin-top: 12px;
  font-size: 12px;
  color: #bfbfbf;
}

.card-footer {
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 100%;
  border-top: 1px solid #e9e9e9;
  padding: 10px 16px;
  background: #fff;
  text-align: right;
  z-index: 1;
}
</style>
