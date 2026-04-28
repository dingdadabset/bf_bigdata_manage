<template>
  <div class="cluster-management">
    <div class="module-header">
      <div>
        <h1>环境资源注册</h1>
        <p>统一维护 CDH、HDP、Hive Metastore、LDAP、Ranger 等资源端点</p>
      </div>
      <a-button type="primary" icon="plus" size="large" @click="showCreateModal">新建环境</a-button>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span class="summary-label">环境总数</span>
        <strong>{{ clusters.length }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">StarRocks</span>
        <strong>{{ starRocksCount }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">CDH/Hive</span>
        <strong>{{ hiveCount }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">资源端点</span>
        <strong>{{ endpointCount }}</strong>
      </div>
    </div>

    <a-table
      class="cluster-table"
      :columns="columns"
      :data-source="clusters"
      :loading="loading"
      rowKey="id"
      :scroll="{ x: 1280, y: 'calc(100vh - 430px)' }"
    >
      <template slot="endpoints" slot-scope="endpoints">
        <a-space v-if="endpoints && endpoints.length" size="small">
          <a-tag v-for="endpoint in endpoints" :key="endpoint.id || endpoint.endpointType" :color="getEndpointColor(endpoint.endpointType)">
            {{ endpoint.endpointType }}
          </a-tag>
        </a-space>
        <span v-else class="muted-text">未配置</span>
      </template>
      <template slot="capability" slot-scope="text, record">
        <div class="capability-cell">
          <div class="capability-top">
            <a-tag :color="getCapabilityColor(getCapability(record).status)">
              {{ getCapability(record).status || 'UNKNOWN' }}
            </a-tag>
          </div>
          <div class="capability-main" :title="`${getCapability(record).engineType || '-'} / ${getCapability(record).authBackend || '-'}`">
            {{ getCapability(record).engineType || '-' }} / {{ getCapability(record).authBackend || '-' }}
          </div>
          <div v-if="getCapability(record).warnings && getCapability(record).warnings.length" class="capability-warning">
            {{ getCapability(record).warnings[0] }}
          </div>
        </div>
      </template>
      <template slot="status" slot-scope="text">
        <a-tag :color="text === 'ACTIVE' ? 'green' : 'red'">
          {{ text }}
        </a-tag>
      </template>
      <template slot="action" slot-scope="text, record">
        <a-space>
          <a-button type="link" @click="editCluster(record)">编辑</a-button>
          <a-button type="link" @click="editEndpoints(record)">端点配置</a-button>
          <a-popconfirm v-if="canDeleteEnvironment" title="确定要删除这个集群吗?" @confirm="deleteCluster(record.id)">
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
        <a-form-model-item label="集群编码" prop="clusterCode">
          <a-input v-model="form.clusterCode" placeholder="例如: CDH_PROD_01" :disabled="!!editingId && !!form.clusterCode" />
        </a-form-model-item>
        <a-form-model-item label="类型" prop="type">
          <a-select v-model="form.type" placeholder="选择集群类型">
            <a-select-option value="CDH">CDH</a-select-option>
            <a-select-option value="HDP">HDP</a-select-option>
            <a-select-option value="EMR">EMR</a-select-option>
            <a-select-option value="StarRocks">StarRocks</a-select-option>
            <a-select-option value="Doris">Doris</a-select-option>
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

    <a-drawer
      title="环境资源端点配置"
      :visible="endpointModalVisible"
      :width="920"
      :body-style="{ padding: 0 }"
      @close="endpointModalVisible = false"
    >
      <div v-if="currentCluster" class="endpoint-drawer">
        <div class="endpoint-drawer-header">
          <div>
            <div class="drawer-title">{{ currentCluster.clusterName }}</div>
            <div class="drawer-meta">
              <a-tag>{{ currentCluster.clusterCode || '待自动生成编码' }}</a-tag>
              <a-tag color="blue">{{ currentCluster.type }}</a-tag>
              <span>{{ endpointForm.length }} 个端点</span>
            </div>
          </div>
          <a-button type="primary" icon="plus" @click="addEndpoint">新增端点</a-button>
        </div>

        <div class="endpoint-workbench">
          <div class="endpoint-list">
            <div
              v-for="(endpoint, index) in endpointForm"
              :key="endpoint._rowKey"
              class="endpoint-list-item"
              :class="{ active: activeEndpointIndex === index }"
              @click="activeEndpointIndex = index"
            >
              <div class="endpoint-list-main">
                <a-tag :color="getEndpointColor(endpoint.endpointType)">
                  {{ endpoint.endpointType || '未选择类型' }}
                </a-tag>
                <span class="endpoint-name">{{ endpoint.url || endpoint.serviceName || '待配置端点' }}</span>
              </div>
              <span class="endpoint-status">{{ endpoint.status || 'ACTIVE' }}</span>
            </div>
            <a-empty v-if="!endpointForm.length" description="暂无端点" class="endpoint-empty" />
          </div>

          <div class="endpoint-editor">
            <template v-if="activeEndpoint">
              <div class="editor-title">
                <span>{{ activeEndpoint.id ? '编辑端点' : '新增端点' }}</span>
                <a-tag :color="getEndpointColor(activeEndpoint.endpointType)">
                  {{ activeEndpoint.endpointType || 'NEW' }}
                </a-tag>
              </div>

              <a-form-model layout="vertical" :model="activeEndpoint">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-model-item label="端点类型">
                      <a-select v-model="activeEndpoint.endpointType" @change="onEndpointTypeChange(activeEndpoint)">
                        <a-select-option value="HIVE_SERVER2">HIVE_SERVER2</a-select-option>
                        <a-select-option value="HIVE_METASTORE_DB">HIVE_METASTORE_DB</a-select-option>
                        <a-select-option value="AZKABAN_DB">AZKABAN_DB</a-select-option>
                        <a-select-option value="DOLPHINSCHEDULER_DB">DOLPHINSCHEDULER_DB</a-select-option>
                        <a-select-option value="STARROCKS_JDBC">STARROCKS_JDBC</a-select-option>
                        <a-select-option value="DORIS_JDBC">DORIS_JDBC</a-select-option>
                        <a-select-option value="LDAP">LDAP</a-select-option>
                        <a-select-option value="RANGER">RANGER</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-model-item label="状态">
                      <a-select v-model="activeEndpoint.status">
                        <a-select-option value="ACTIVE">ACTIVE</a-select-option>
                        <a-select-option value="INACTIVE">INACTIVE</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                </a-row>

                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-model-item label="授权后端">
                      <a-select v-model="activeEndpoint.authBackend" :allowClear="true" :disabled="metadataEndpointTypes.includes(activeEndpoint.endpointType)">
                        <a-select-option value="SENTRY">SENTRY</a-select-option>
                        <a-select-option value="STARROCKS_SQL">STARROCKS_SQL</a-select-option>
                        <a-select-option value="DORIS_SQL">DORIS_SQL</a-select-option>
                        <a-select-option value="RANGER">RANGER</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-model-item label="服务名">
                      <a-input v-model="activeEndpoint.serviceName" placeholder="Ranger serviceName，可选" />
                    </a-form-model-item>
                  </a-col>
                </a-row>

                <a-form-model-item label="连接地址">
                  <a-input v-model="activeEndpoint.url" :placeholder="endpointUrlPlaceholder" />
                </a-form-model-item>

                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-model-item label="账号">
                      <a-input v-model="activeEndpoint.username" placeholder="admin 或 cn=admin,dc=example,dc=com" />
                    </a-form-model-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-model-item label="密码">
                      <a-input-password v-model="activeEndpoint.password" placeholder="留空表示不修改" />
                    </a-form-model-item>
                  </a-col>
                </a-row>

                <template v-if="activeEndpoint.endpointType === 'LDAP'">
                  <a-row :gutter="16">
                    <a-col :span="12">
                      <a-form-model-item label="Base DN">
                        <a-input v-model="activeEndpoint.baseDn" placeholder="dc=example,dc=com" />
                      </a-form-model-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-model-item label="User Base DN">
                        <a-input v-model="activeEndpoint.userBaseDn" placeholder="ou=People 或 cn=users,cn=accounts" />
                      </a-form-model-item>
                    </a-col>
                  </a-row>
                </template>

                <a-form-model-item label="描述">
                  <a-textarea v-model="activeEndpoint.description" :rows="3" placeholder="可选描述信息" />
                </a-form-model-item>
              </a-form-model>

              <div class="editor-footer">
                <a-popconfirm v-if="canDeleteEnvironment" title="确定删除这个端点吗?" @confirm="removeEndpoint(activeEndpoint, activeEndpointIndex)">
                  <a-button type="danger" ghost>删除</a-button>
                </a-popconfirm>
                <a-button icon="thunderbolt" :loading="activeEndpoint._testing" @click="testEndpoint(activeEndpoint)">测试连通性</a-button>
                <a-button type="primary" :loading="activeEndpoint._saving" @click="saveEndpoint(activeEndpoint)">保存端点</a-button>
              </div>
            </template>
            <a-empty v-else description="请选择或新增一个端点" />
          </div>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script>
import axios from 'axios';
import { canDelete, deleteForbiddenMessage } from '../../utils/currentUser';

export default {
  name: 'ClusterManagement',
  data() {
    return {
      loading: false,
      clusters: [],
      capabilityMap: {},
      modalVisible: false,
      endpointModalVisible: false,
      submitting: false,
      editingId: null,
      currentCluster: null,
      endpointForm: [],
      activeEndpointIndex: -1,
      form: {
        clusterName: '',
        clusterCode: '',
        type: 'CDH',
        status: 'ACTIVE',
        description: ''
      },
      rules: {
        clusterName: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
        clusterCode: [{ required: true, message: '请输入集群编码', trigger: 'blur' }],
        type: [{ required: true, message: '请选择集群类型', trigger: 'change' }]
      },
      columns: [
        { title: 'ID', dataIndex: 'id', width: 80 },
        { title: '集群名称', dataIndex: 'clusterName', width: 120 },
        { title: '集群编码', dataIndex: 'clusterCode', width: 120 },
        { title: '类型', dataIndex: 'type', width: 120 },
        { title: '资源端点', dataIndex: 'endpoints', width: 260, scopedSlots: { customRender: 'endpoints' } },
        { title: '授权能力', key: 'capability', width: 220, scopedSlots: { customRender: 'capability' } },
        { title: '描述', dataIndex: 'description', width: 160 },
        { title: '状态', dataIndex: 'status', width: 120, scopedSlots: { customRender: 'status' } },
        { title: '创建时间', dataIndex: 'createTime', width: 220 },
        { title: '操作', key: 'action', width: 220, scopedSlots: { customRender: 'action' } }
      ],
      endpointColumns: []
    };
  },
  created() {
    this.fetchClusters();
  },
  computed: {
    endpointCount() {
      return this.clusters.reduce((total, item) => total + ((item.endpoints || []).length), 0);
    },
    starRocksCount() {
      return this.clusters.filter(item => (item.type || '').toLowerCase().includes('starrocks')).length;
    },
    hiveCount() {
      return this.clusters.filter(item => {
        const type = (item.type || '').toLowerCase();
        return type.includes('cdh') || type.includes('hive') || type.includes('emr') || type.includes('hdp');
      }).length;
    },
    activeEndpoint() {
      return this.endpointForm[this.activeEndpointIndex] || null;
    },
    endpointUrlPlaceholder() {
      if (!this.activeEndpoint) return '请输入连接地址';
      switch (this.activeEndpoint.endpointType) {
        case 'HIVE_SERVER2': return 'jdbc:hive2://host:10000/default';
        case 'HIVE_METASTORE_DB': return 'jdbc:mysql://host:3306/hive_metastore';
        case 'AZKABAN_DB': return 'jdbc:mysql://host:3306/azkaban';
        case 'DOLPHINSCHEDULER_DB': return 'jdbc:mysql://host:3306/dolphinscheduler';
        case 'STARROCKS_JDBC': return 'jdbc:mysql://host:9030';
        case 'DORIS_JDBC': return 'jdbc:mysql://host:9030';
        case 'LDAP': return 'ldap://host:389';
        case 'RANGER': return 'http://host:6080';
        default: return '请输入连接地址';
      }
    },
    canDeleteEnvironment() {
      return canDelete();
    },
    metadataEndpointTypes() {
      return ['LDAP', 'HIVE_METASTORE_DB', 'AZKABAN_DB', 'DOLPHINSCHEDULER_DB'];
    }
  },
  methods: {
    async fetchClusters() {
      this.loading = true;
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = res.data.filter(c => c.status !== 'DELETED'); // Client-side filter as backup
        this.fetchCapabilities();
      } catch (e) {
        this.$message.error('加载集群列表失败');
      } finally {
        this.loading = false;
      }
    },
    async fetchCapabilities() {
      const requests = this.clusters.map(async cluster => {
        const key = cluster.clusterCode || cluster.clusterName;
        if (!key) return;
        try {
          const res = await axios.get('/api/access/capabilities', { params: { cluster: key } });
          this.$set(this.capabilityMap, cluster.id, res.data || {});
        } catch (e) {
          this.$set(this.capabilityMap, cluster.id, {
            status: 'UNKNOWN',
            warnings: ['授权能力加载失败']
          });
        }
      });
      await Promise.all(requests);
    },
    showCreateModal() {
      this.editingId = null;
      this.form = {
        clusterName: '',
        clusterCode: '',
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
    editEndpoints(record) {
      this.currentCluster = record;
      this.endpointForm = (record.endpoints || []).map(endpoint => ({
        ...endpoint,
        password: '',
        _rowKey: endpoint.id || `${endpoint.endpointType}-${Date.now()}-${Math.random()}`
      }));
      this.activeEndpointIndex = this.endpointForm.length ? 0 : -1;
      this.endpointModalVisible = true;
    },
    addEndpoint() {
      const endpointType = this.currentCluster && this.currentCluster.type === 'StarRocks'
        ? 'STARROCKS_JDBC'
        : this.currentCluster && this.currentCluster.type === 'Doris'
          ? 'DORIS_JDBC'
        : 'HIVE_SERVER2';
      const endpoint = {
        _rowKey: `new-${Date.now()}-${Math.random()}`,
        endpointType,
        authBackend: endpointType === 'STARROCKS_JDBC' ? 'STARROCKS_SQL'
          : endpointType === 'DORIS_JDBC' ? 'DORIS_SQL'
          : 'SENTRY',
        url: '',
        username: '',
        password: '',
        serviceName: '',
        baseDn: '',
        userBaseDn: '',
        status: 'ACTIVE'
      };
      this.endpointForm.push(endpoint);
      this.activeEndpointIndex = this.endpointForm.length - 1;
    },
    onEndpointTypeChange(record) {
      if (record.endpointType === 'STARROCKS_JDBC') {
        record.authBackend = 'STARROCKS_SQL';
      } else if (record.endpointType === 'DORIS_JDBC') {
        record.authBackend = 'DORIS_SQL';
      } else if (record.endpointType === 'HIVE_SERVER2') {
        record.authBackend = 'SENTRY';
      } else if (['HIVE_METASTORE_DB', 'AZKABAN_DB', 'DOLPHINSCHEDULER_DB'].includes(record.endpointType)) {
        record.authBackend = undefined;
      } else if (record.endpointType === 'RANGER') {
        record.authBackend = 'RANGER';
      } else if (record.endpointType === 'LDAP') {
        record.authBackend = undefined;
      }
    },
    generateClusterCode(name) {
      const source = (name || 'CLUSTER').trim().toUpperCase();
      const code = source.replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').replace(/_+/g, '_');
      return code || 'CLUSTER';
    },
    sanitizeEndpointPayload(record) {
      const payload = { ...record };
      delete payload._rowKey;
      delete payload._saving;
      delete payload._testing;
      return payload;
    },
    async ensureClusterCode() {
      if (!this.currentCluster || !this.currentCluster.id) {
        throw new Error('请先保存环境信息');
      }
      if (this.currentCluster.clusterCode) {
        return this.currentCluster.clusterCode;
      }

      const generatedCode = this.generateClusterCode(this.currentCluster.clusterName || this.currentCluster.type);
      const payload = {
        ...this.currentCluster,
        clusterCode: generatedCode
      };
      delete payload.endpoints;
      const res = await axios.put(`/api/clusters/${this.currentCluster.id}`, payload);
      const updated = res.data || payload;
      this.currentCluster = {
        ...this.currentCluster,
        ...updated,
        clusterCode: updated.clusterCode || generatedCode
      };
      const index = this.clusters.findIndex(item => item.id === this.currentCluster.id);
      if (index > -1) {
        this.$set(this.clusters, index, {
          ...this.clusters[index],
          ...this.currentCluster
        });
      }
      this.$message.success(`已为历史环境生成编码: ${this.currentCluster.clusterCode}`);
      return this.currentCluster.clusterCode;
    },
    async saveEndpoint(record) {
      if (!record.endpointType) {
        this.$message.error('请选择端点类型');
        return;
      }
      this.$set(record, '_saving', true);
      try {
        const code = await this.ensureClusterCode();
        const payload = this.sanitizeEndpointPayload(record);
        if (record.id) {
          await axios.put(`/api/clusters/${code}/endpoints/${record.id}`, payload);
        } else {
          await axios.post(`/api/clusters/${code}/endpoints`, payload);
        }
        this.$message.success('端点已保存');
        await this.fetchClusters();
        const refreshed = this.clusters.find(c => c.clusterCode === code);
        if (refreshed) {
          this.currentCluster = refreshed;
          this.endpointForm = (refreshed.endpoints || []).map(endpoint => ({
            ...endpoint,
            password: '',
            _rowKey: endpoint.id || `${endpoint.endpointType}-${Date.now()}-${Math.random()}`
          }));
          const nextIndex = this.endpointForm.findIndex(item => item.id === record.id);
          this.activeEndpointIndex = nextIndex > -1 ? nextIndex : Math.max(0, this.endpointForm.length - 1);
        }
      } catch (e) {
        this.$message.error('端点保存失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.$set(record, '_saving', false);
      }
    },
    async testEndpoint(record) {
      if (!record.endpointType) {
        this.$message.error('请选择端点类型');
        return;
      }
      this.$set(record, '_testing', true);
      try {
        const code = await this.ensureClusterCode();
        const payload = this.sanitizeEndpointPayload(record);
        const res = await axios.post(`/api/clusters/${code}/endpoints/test`, payload);
        if (res.data && res.data.success) {
          this.$message.success(`连通成功 (${res.data.elapsedMs || 0}ms)`);
        } else {
          this.$message.error(`连通失败: ${(res.data && res.data.message) || '未知错误'}`);
        }
      } catch (e) {
        this.$message.error('连通测试失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.$set(record, '_testing', false);
      }
    },
    async removeEndpoint(record, index) {
      if (!this.canDeleteEnvironment) {
        this.$message.warning(deleteForbiddenMessage());
        return;
      }
      if (!record.id) {
        this.endpointForm.splice(index, 1);
        this.activeEndpointIndex = this.endpointForm.length ? Math.min(index, this.endpointForm.length - 1) : -1;
        return;
      }
      try {
        const code = await this.ensureClusterCode();
        await axios.delete(`/api/clusters/${code}/endpoints/${record.id}`);
        this.$message.success('端点已删除');
        this.endpointForm.splice(index, 1);
        this.activeEndpointIndex = this.endpointForm.length ? Math.min(index, this.endpointForm.length - 1) : -1;
        this.fetchClusters();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '端点删除失败');
      }
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
      if (!this.canDeleteEnvironment) {
        this.$message.warning(deleteForbiddenMessage());
        return;
      }
      try {
        await axios.delete(`/api/clusters/${id}`);
        this.$message.success('删除成功');
        this.fetchClusters();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '删除失败');
      }
    },
    getEndpointColor(type) {
      switch (type) {
        case 'HIVE_SERVER2': return 'orange';
        case 'HIVE_METASTORE_DB': return 'volcano';
        case 'AZKABAN_DB': return 'magenta';
        case 'DOLPHINSCHEDULER_DB': return 'purple';
        case 'STARROCKS_JDBC': return 'blue';
        case 'DORIS_JDBC': return 'geekblue';
        case 'LDAP': return 'cyan';
        case 'RANGER': return 'green';
        default: return 'default';
      }
    },
    getCapability(record) {
      return this.capabilityMap[record.id] || {};
    },
    getCapabilityColor(status) {
      switch (status) {
        case 'READY': return 'green';
        case 'PLANNED': return 'gold';
        case 'UNCONFIGURED': return 'red';
        default: return 'default';
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
.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.module-header h1 {
  margin-bottom: 6px;
}
.module-header p {
  color: #667085;
  margin: 0;
}
.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.summary-item {
  border: 1px solid #edf0f5;
  border-radius: 4px;
  padding: 16px;
  background: #fafcff;
}
.summary-label {
  display: block;
  color: #667085;
  margin-bottom: 8px;
}
.summary-item strong {
  font-size: 24px;
  color: #1f2d3d;
}
.muted-text {
  color: #999;
}
.cluster-table {
  width: 100%;
}
.cluster-table >>> .ant-table-body {
  scrollbar-gutter: stable;
}
.cluster-table >>> .ant-table-thead > tr > th,
.cluster-table >>> .ant-table-tbody > tr > td {
  white-space: nowrap;
}
.cluster-table >>> .ant-table-tbody > tr > td {
  vertical-align: top;
}
.cluster-table >>> .ant-space {
  flex-wrap: wrap;
  row-gap: 4px;
}
.capability-cell {
  width: 200px;
  min-height: 44px;
}
.capability-top {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.capability-main {
  display: block;
  color: #344054;
  font-size: 12px;
  line-height: 18px;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.capability-warning {
  color: #b7791f;
  font-size: 12px;
  line-height: 18px;
  margin-top: 4px;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.endpoint-drawer {
  min-height: 100%;
  background: #f4f7f9;
}
.endpoint-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #edf0f5;
}
.drawer-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.drawer-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #667085;
}
.endpoint-workbench {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
}
.endpoint-list,
.endpoint-editor {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px;
}
.endpoint-list {
  padding: 8px;
  min-height: 520px;
}
.endpoint-list-item {
  padding: 12px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.endpoint-list-item:hover {
  background: #f5f8ff;
}
.endpoint-list-item.active {
  background: #e6f7ff;
  border-color: #91d5ff;
}
.endpoint-list-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.endpoint-name {
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.endpoint-status {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  margin-top: 8px;
}
.endpoint-empty {
  margin-top: 120px;
}
.endpoint-editor {
  padding: 20px;
  min-width: 0;
}
.editor-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}
.editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #edf0f5;
}
@media (max-width: 900px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
  .endpoint-workbench {
    grid-template-columns: 1fr;
  }
  .endpoint-list {
    min-height: auto;
  }
}
</style>
