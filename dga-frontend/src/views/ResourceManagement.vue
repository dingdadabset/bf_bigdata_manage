<template>
  <div class="resource-management">
    <!-- Hero Header -->
    <div class="hero-header">
      <div class="header-content">
        <h1 class="page-title">资源导航</h1>
        <div class="search-bar">
          <a-input-search
            v-model="searchQuery"
            placeholder="搜索资源名称、描述或链接..."
            size="large"
            allow-clear
            @search="onSearch"
          >
            <a-icon slot="prefix" type="search" />
          </a-input-search>
        </div>
        <div class="header-actions">
          <a-button type="primary" size="large" icon="plus" @click="showAddModal" class="action-btn">添加资源</a-button>
          <a-button size="large" icon="thunderbolt" @click="showQuickAddModal" class="action-btn glass-btn">快速添加</a-button>
        </div>
      </div>
      
      <!-- Elegant Tabs -->
      <div class="env-tabs">
        <div 
          v-for="tab in envTabs" 
          :key="tab.key" 
          :class="['env-tab', { active: envFilter === tab.key }]"
          @click="envFilter = tab.key"
        >
          {{ tab.label }}
          <span class="tab-indicator" v-if="envFilter === tab.key"></span>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="content-wrapper">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <a-spin size="large" />
      </div>

      <!-- Empty State -->
      <div v-else-if="resources.length === 0" class="empty-state">
        <a-empty description="暂无资源，开始添加您的第一个链接吧" />
        <a-button type="primary" @click="showAddModal" style="margin-top: 16px;">立即添加</a-button>
      </div>

      <!-- Resource Grid -->
      <div v-else>
        <transition-group name="list" tag="div">
          <div
            v-for="section in groupedByCategory"
            :key="section.key"
            class="resource-section"
          >
            <div class="section-header">
              <span class="section-title">{{ section.title }}</span>
              <a-badge :count="section.items.length" :number-style="{ backgroundColor: '#f0f2f5', color: '#999', boxShadow: 'none' }" />
            </div>
            
            <div class="resource-grid">
              <div
                v-for="item in section.items"
                :key="item.id"
                class="resource-card-wrapper"
              >
                <div class="glass-card">
                  <div class="card-body">
                    <div class="card-top">
                      <div class="logo-wrapper">
                        <img
                          v-if="logoSrc(item)"
                          :src="logoSrc(item)"
                          class="card-logo"
                          @error="onLogoError"
                        />
                        <a-avatar v-else shape="square" :size="48" class="default-logo">{{ item.name.charAt(0) }}</a-avatar>
                      </div>
                      <div class="info-wrapper">
                        <div class="title-row">
                          <a :href="item.url" target="_blank" class="card-title" :title="item.name">{{ item.name }}</a>
                          <a-icon type="link" class="visit-icon" />
                        </div>
                        <div class="tags-row">
                          <span :class="['env-badge', item.env]">{{ envLabel(item.env) }}</span>
                          <span v-if="item.category" class="category-badge">{{ item.category }}</span>
                          <span v-if="item.recommended" class="recommend-badge"><a-icon type="star" theme="filled" /> 推荐</span>
                        </div>
                      </div>
                      <div class="card-actions-hover">
                        <a-tooltip title="复制链接">
                          <a-button type="link" icon="copy" size="small" @click.stop="copyLink(item.url)" />
                        </a-tooltip>
                        <a-dropdown :trigger="['click']">
                          <a-button type="link" icon="more" size="small" @click.stop />
                          <a-menu slot="overlay">
                            <a-menu-item @click="editResource(item)">
                              <a-icon type="edit" /> 编辑
                            </a-menu-item>
                            <a-menu-item @click="deleteResource(item.id)" class="danger-text">
                              <a-icon type="delete" /> 删除
                            </a-menu-item>
                          </a-menu>
                        </a-dropdown>
                      </div>
                    </div>
                    <div class="card-desc" v-if="item.description">
                      {{ item.description }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition-group>
        
        <div v-if="filteredResources.length === 0 && !loading" class="empty-search">
           <a-empty description="没有找到匹配的资源" image="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" />
        </div>
      </div>
    </div>

    <!-- Modals (Kept mostly same but cleaned up) -->
    <a-modal
      v-model="modalVisible"
      :title="editingId ? '编辑资源' : '添加资源'"
      @ok="handleModalOk"
      :confirmLoading="modalLoading"
      centered
      class="elegant-modal"
    >
      <a-form-model ref="form" :model="form" :rules="rules" layout="vertical">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" placeholder="资源名称" />
        </a-form-model-item>
        <a-form-model-item label="链接" prop="url">
          <a-input v-model="form.url" placeholder="https://" />
        </a-form-model-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-model-item label="分类" prop="category">
              <a-select v-model="form.category" mode="tags" placeholder="选择或输入分类">
                <a-select-option v-for="cat in categories" :key="cat">{{ cat }}</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="环境" prop="env">
              <a-select v-model="form.env">
                <a-select-option value="PROD">生产环境</a-select-option>
                <a-select-option value="TEST">测试环境</a-select-option>
                <a-select-option value="DEV">开发环境</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-form-model-item label="描述" prop="description">
          <a-textarea v-model="form.description" :rows="3" />
        </a-form-model-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-model-item label="排序权重">
              <a-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
            </a-form-model-item>
          </a-col>
          <a-col :span="12">
            <a-form-model-item label="选项" class="checkbox-item">
              <a-checkbox v-model="form.recommended">设为推荐资源</a-checkbox>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </a-modal>

    <a-modal
      v-model="quickAddVisible"
      title="快速添加"
      @ok="handleQuickAddOk"
      :confirmLoading="quickAddLoading"
      centered
      class="elegant-modal"
    >
      <div style="margin-bottom: 16px; color: #666;">
        输入 URL，自动抓取标题、图标和描述。
      </div>
      <a-form-model ref="quickForm" :model="quickForm" :rules="quickRules" layout="vertical">
        <a-form-model-item label="链接地址" prop="url">
          <a-input v-model="quickForm.url" placeholder="https://..." size="large" />
        </a-form-model-item>
        <a-row :gutter="16">
           <a-col :span="12">
             <a-form-model-item label="分类" prop="category">
                <a-select v-model="quickForm.category" mode="tags">
                  <a-select-option v-for="cat in categories" :key="cat">{{ cat }}</a-select-option>
                </a-select>
             </a-form-model-item>
           </a-col>
           <a-col :span="12">
             <a-form-model-item label="环境" prop="env">
                <a-select v-model="quickForm.env">
                  <a-select-option value="PROD">生产</a-select-option>
                  <a-select-option value="TEST">测试</a-select-option>
                </a-select>
             </a-form-model-item>
           </a-col>
        </a-row>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      loading: false,
      resources: [],
      envFilter: 'ALL',
      searchQuery: '',
      
      modalVisible: false,
      modalLoading: false,
      editingId: null,
      form: {
        name: '',
        url: '',
        category: '',
        env: 'PROD',
        description: '',
        sortOrder: 0,
        recommended: false
      },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        url: [{ required: true, message: '请输入链接', trigger: 'blur' }]
      },
      quickAddVisible: false,
      quickAddLoading: false,
      quickForm: {
        url: '',
        category: '常用工具',
        env: 'PROD'
      },
      quickRules: {
        url: [{ required: true, message: '请输入链接', trigger: 'blur' }]
      },
      envTabs: [
        { key: 'ALL', label: '全部' },
        { key: 'PROD', label: '生产环境' },
        { key: 'TEST', label: '测试环境' },
        { key: 'DEV', label: '开发环境' }
      ]
    };
  },
  computed: {
    categories() {
      const cats = new Set(this.resources.map(r => r.category).filter(Boolean));
      return Array.from(cats);
    },
    filteredResources() {
      let result = this.resources;
      
      // Filter by Environment
      if (this.envFilter !== 'ALL') {
        result = result.filter(r => r.env === this.envFilter);
      }
      
      // Filter by Search Query
      if (this.searchQuery) {
        const q = this.searchQuery.toLowerCase();
        result = result.filter(r => 
          (r.name && r.name.toLowerCase().includes(q)) || 
          (r.description && r.description.toLowerCase().includes(q)) ||
          (r.url && r.url.toLowerCase().includes(q)) ||
          (r.category && r.category.toLowerCase().includes(q))
        );
      }
      
      return result;
    },
    groupedByCategory() {
      const groups = {};
      this.filteredResources.forEach(r => {
        const key = r.category || '未分类';
        if (!groups[key]) {
          groups[key] = [];
        }
        groups[key].push(r);
      });
      return Object.keys(groups)
        .sort()
        .map(key => ({
          key,
          title: key,
          items: groups[key]
        }));
    }
  },
  mounted() {
    this.fetchResources();
  },
  methods: {
    async fetchResources() {
      this.loading = true;
      try {
        const res = await axios.get('/api/resources');
        this.resources = res.data;
      } catch (e) {
        this.$message.error('获取资源列表失败');
      } finally {
        this.loading = false;
      }
    },
    onSearch() {
      // Triggered by search input, logic handled by computed
    },
    copyLink(url) {
      if (!url) return;
      const el = document.createElement('textarea');
      el.value = url;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
      this.$message.success('链接已复制');
    },
    
    showAddModal() {
      this.editingId = null;
      this.form = {
        name: '',
        url: '',
        category: '',
        env: 'PROD',
        description: '',
        sortOrder: 0,
        recommended: false
      };
      this.modalVisible = true;
    },
    
    editResource(record) {
      this.editingId = record.id;
      this.form = { ...record };
      this.modalVisible = true;
    },
    
    handleModalOk() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          this.modalLoading = true;
          try {
            if (this.editingId) {
              await axios.put(`/api/resources/${this.editingId}`, this.form);
              this.$message.success('更新成功');
            } else {
              await axios.post('/api/resources', this.form);
              this.$message.success('创建成功');
            }
            this.modalVisible = false;
            this.fetchResources();
          } catch (e) {
            this.$message.error('操作失败');
          } finally {
            this.modalLoading = false;
          }
        }
      });
    },
    
    async deleteResource(id) {
      this.$confirm({
        title: '确定要删除这个资源吗?',
        content: '删除后无法恢复',
        okText: '删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          try {
            await axios.delete(`/api/resources/${id}`);
            this.$message.success('删除成功');
            this.fetchResources();
          } catch (e) {
            this.$message.error('删除失败');
          }
        }
      });
    },
    
    showQuickAddModal() {
      this.quickForm = { url: '', category: '常用工具', env: 'PROD' };
      this.quickAddVisible = true;
    },
    
    handleQuickAddOk() {
      this.$refs.quickForm.validate(async valid => {
        if (valid) {
          this.quickAddLoading = true;
          try {
            await axios.post('/api/resources/quick-add', this.quickForm);
            this.$message.success('快速添加成功');
            this.quickAddVisible = false;
            this.fetchResources();
          } catch (e) {
            this.$message.error('快速添加失败，请检查URL是否可访问');
          } finally {
            this.quickAddLoading = false;
          }
        }
      });
    },
    logoSrc(item) {
      if (item.logoUrl) {
        return item.logoUrl;
      }
      if (item.url) {
        const encoded = encodeURIComponent(item.url);
        const name = item.name ? encodeURIComponent(item.name) : '';
        const v = 4;
        const nameParam = name ? `&name=${name}` : '';
        return `/api/resources/favicon?url=${encoded}${nameParam}&v=${v}`;
      }
      return '';
    },
    onLogoError(e) {
      if (e && e.target) {
        e.target.style.display = 'none';
      }
    },
    envLabel(env) {
      if (env === 'PROD') return '生产';
      if (env === 'TEST') return '测试';
      if (env === 'DEV') return '开发';
      return env || '';
    }
  }
};
</script>

<style scoped>
/* Main Layout */
.resource-management {
  min-height: 100vh;
  background-color: #f8f9fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* Hero Header */
.hero-header {
  background: #fff;
  padding: 32px 40px 0;
  border-bottom: 1px solid #eaeaea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  position: sticky;
  top: 0;
  z-index: 10;
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

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
}

.glass-btn {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
}

/* Elegant Tabs */
.env-tabs {
  display: flex;
  gap: 32px;
}

.env-tab {
  padding-bottom: 16px;
  color: #666;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
}

.env-tab:hover {
  color: #1890ff;
}

.env-tab.active {
  color: #1890ff;
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: #1890ff;
  border-radius: 3px 3px 0 0;
  box-shadow: 0 -2px 6px rgba(24, 144, 255, 0.2);
}

/* Content */
.content-wrapper {
  padding: 32px 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.resource-section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background: linear-gradient(to bottom, #1890ff, #69c0ff);
  border-radius: 2px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

/* Glass Card */
.glass-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid rgba(0,0,0,0.04);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  height: 100%;
  position: relative;
  overflow: hidden;
  cursor: default;
}

.glass-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.08);
  border-color: rgba(24, 144, 255, 0.2);
}

.card-body {
  padding: 20px;
}

.card-top {
  display: flex;
  gap: 16px;
  position: relative;
}

.logo-wrapper {
  flex-shrink: 0;
}

.card-logo {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: contain;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.default-logo {
  background-color: #f0f2f5;
  color: #1890ff;
  font-weight: 600;
  font-size: 20px;
  border-radius: 10px;
}

.info-wrapper {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-decoration: none;
  transition: color 0.2s;
}

.card-title:hover {
  color: #1890ff;
}

.visit-icon {
  font-size: 12px;
  color: #ccc;
  margin-left: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}

.glass-card:hover .visit-icon {
  opacity: 1;
}

.tags-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.env-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: #f5f5f5;
  color: #666;
}

.env-badge.PROD { background: #fff1f0; color: #f5222d; border: 1px solid #ffa39e; }
.env-badge.TEST { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
.env-badge.DEV { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }

.category-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: #f0f2f5;
  color: #595959;
  border: 1px solid #d9d9d9;
}

.recommend-badge {
  font-size: 12px;
  color: #faad14;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-desc {
  margin-top: 12px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Hover Actions */
.card-actions-hover {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.glass-card:hover .card-actions-hover {
  opacity: 1;
  pointer-events: auto;
}

.danger-text {
  color: #ff4d4f;
}

/* Transitions */
.list-enter-active, .list-leave-active {
  transition: all 0.3s;
}
.list-enter, .list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.empty-state, .empty-search {
  text-align: center;
  padding: 60px 0;
}

.checkbox-item {
  margin-top: 36px;
}
</style>
