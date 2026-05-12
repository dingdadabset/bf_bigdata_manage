<template>
  <div class="ldap-group-manager" :class="{ compact: compact }">
    <div class="group-toolbar">
      <div>
        <h2>可分配组</h2>
        <p>面向当前用户选择组，技术标识默认收起。</p>
      </div>
      <div class="toolbar-actions">
        <a-input-search
          v-model="searchKeyword"
          class="group-search"
          allow-clear
          placeholder="搜索组名"
        />
        <a-select v-model="statusFilter" class="status-filter">
          <a-select-option value="ALL">全部状态</a-select-option>
          <a-select-option value="ACTIVE">有成员</a-select-option>
          <a-select-option value="EMPTY">空组</a-select-option>
        </a-select>
        <a-select
          v-model="selectedCluster"
          class="cluster-select"
          placeholder="选择集群"
          @change="fetchGroups"
        >
          <a-select-option
            v-for="cluster in clusters"
            :key="cluster.id"
            :value="cluster.clusterCode || cluster.clusterName"
          >
            {{ cluster.clusterName }} ({{ cluster.clusterCode || cluster.type }})
          </a-select-option>
        </a-select>
        <a-button icon="reload" @click="fetchGroups">刷新</a-button>
        <a-button type="primary" icon="plus" @click="openCreateModal">新建组</a-button>
      </div>
    </div>

    <a-card :bordered="false" class="group-card" :body-style="{ padding: '16px' }">
      <a-spin :spinning="loading">
        <a-table
          v-if="filteredGroups.length"
          row-key="name"
          size="middle"
          class="relation-table"
          :columns="columns"
          :data-source="filteredGroups"
          :loading="relationSaving"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :row-class-name="groupRowClassName"
        >
          <template slot="group" slot-scope="text, record">
            <div class="group-main">
              <a-avatar class="group-avatar" size="small" icon="team" />
              <div class="group-title-content">
                <div class="group-title-line">
                  <h3>{{ record.name }}</h3>
                </div>
                <p>{{ record.description || '未维护描述' }}</p>
              </div>
            </div>
          </template>
          <template slot="type">
            <span class="soft-text">LDAP 组</span>
          </template>
          <template slot="status" slot-scope="text, record">
            <a-tag :color="memberStatusColor(record)">{{ memberStatusLabel(record) }}</a-tag>
          </template>
          <template slot="members" slot-scope="text, record">
            <strong class="member-count">{{ record.memberCount || 0 }}</strong>
          </template>
          <template slot="source" slot-scope="text, record">
            <div class="zone-tags">
              <a-tag
                v-for="zone in record.directoryZones"
                :key="zone"
                :color="zoneTagColor(zone)"
              >
                {{ zone }}
              </a-tag>
            </div>
          </template>
          <template slot="relation" slot-scope="text, record">
            <a-tag v-if="isPrimaryGroup(record)" color="blue">主组</a-tag>
            <a-tag v-else-if="isSupplementaryGroup(record)" color="purple">附加组</a-tag>
            <span v-else class="soft-text">未加入</span>
          </template>
          <template slot="actions" slot-scope="text, record">
            <a-dropdown placement="bottomRight">
              <a-button class="row-action" shape="circle" icon="more" />
              <a-menu slot="overlay">
                <a-menu-item
                  v-if="!isPrimaryGroup(record)"
                  key="primary"
                  @click="setAsPrimaryGroup(record)"
                >
                  <a-icon type="pushpin" /> 设为主组
                </a-menu-item>
                <a-menu-item
                  v-if="!isPrimaryGroup(record) && !isSupplementaryGroup(record)"
                  key="join"
                  @click="addSupplementaryGroup(record)"
                >
                  <a-icon type="plus" /> 加入附加组
                </a-menu-item>
                <a-menu-item
                  v-if="isSupplementaryGroup(record)"
                  key="remove"
                  @click="removeSupplementaryGroup(record)"
                >
                  <a-icon type="minus" /> 移除附加组
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="edit" @click="openEditModal(record)">
                  <a-icon type="edit" /> 编辑组
                </a-menu-item>
                <a-menu-item key="detail" @click="openTechDrawer(record)">
                  <a-icon type="code" /> 高级属性
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete">
                  <a-popconfirm
                    title="确认删除该 LDAP 组？如果仍是用户主组会被后端拦截。"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="deleteGroup(record)"
                  >
                    <span class="danger-action"><a-icon type="delete" /> 删除组</span>
                  </a-popconfirm>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </template>
        </a-table>
        <a-empty v-else description="暂无 LDAP 组" />
      </a-spin>
    </a-card>

    <a-drawer
      :visible="techDrawerVisible"
      title="LDAP 高级属性"
      width="520"
      @close="techDrawerVisible = false"
    >
      <div v-if="currentTechGroup" class="detail-list">
        <div>
          <span>组名</span>
          <strong>{{ currentTechGroup.name }}</strong>
        </div>
        <div>
          <span>DN</span>
          <strong>{{ currentTechGroup.dn || '未返回 DN' }}</strong>
        </div>
        <div>
          <span>gidNumber</span>
          <strong>{{ currentTechGroup.gidNumber || '自动分配' }}</strong>
        </div>
        <div>
          <span>成员概览</span>
          <strong>{{ membersPreview(currentTechGroup) }}</strong>
        </div>
      </div>
    </a-drawer>

    <a-modal
      :visible="modalVisible"
      :title="editingGroup ? '编辑 LDAP 组' : '新建 LDAP 组'"
      width="680px"
      :confirm-loading="saving"
      @ok="submitGroup"
      @cancel="modalVisible = false"
    >
      <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="组名">
          <a-input
            v-model="form.name"
            :disabled="!!editingGroup"
            placeholder="例如 admins"
          />
        </a-form-model-item>
        <a-form-model-item label="gidNumber">
          <a-input-number
            v-model="form.gidNumber"
            :min="1"
            style="width: 100%"
            placeholder="留空则自动分配"
          />
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-input v-model="form.description" placeholder="可选描述" />
        </a-form-model-item>
        <a-form-model-item label="成员">
          <a-textarea
            v-model="form.membersText"
            :rows="7"
            placeholder="每行一个 uid，也支持逗号分隔"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import { store } from '../../../store';

export default {
  name: 'LdapGroupManager',
  props: {
    compact: {
      type: Boolean,
      default: false
    },
    primaryGroupName: {
      type: String,
      default: ''
    },
    supplementaryGroupNames: {
      type: Array,
      default: () => []
    },
    username: {
      type: String,
      default: ''
    },
    cluster: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      store,
      clusters: [],
      selectedCluster: '',
      searchKeyword: '',
      statusFilter: 'ALL',
      groups: [],
      loading: false,
      saving: false,
      relationSaving: false,
      techDrawerVisible: false,
      currentTechGroup: null,
      modalVisible: false,
      editingGroup: null,
      form: {
        name: '',
        gidNumber: null,
        description: '',
        membersText: ''
      },
      columns: [
        { title: '组名', key: 'group', scopedSlots: { customRender: 'group' } },
        { title: '类型', key: 'type', scopedSlots: { customRender: 'type' }, width: 100 },
        { title: '状态', key: 'status', scopedSlots: { customRender: 'status' }, width: 100 },
        { title: '成员数', key: 'members', scopedSlots: { customRender: 'members' }, width: 90 },
        { title: '来源', key: 'source', scopedSlots: { customRender: 'source' }, width: 150 },
        { title: '当前关系', key: 'relation', scopedSlots: { customRender: 'relation' }, width: 110 },
        { title: '', key: 'actions', scopedSlots: { customRender: 'actions' }, width: 64 }
      ]
    };
  },
  watch: {
    cluster(value) {
      if (value && value !== this.selectedCluster) {
        this.selectedCluster = value;
        this.fetchGroups();
      }
    },
    'store.headerSelectedCluster'(value) {
      if (!this.cluster && value && value !== this.selectedCluster) {
        this.selectedCluster = value;
        this.fetchGroups();
      }
    }
  },
  computed: {
    processedGroups() {
      const byName = new Map();
      (this.groups || []).forEach(group => {
        const name = group.name || '-';
        if (!byName.has(name)) {
          byName.set(name, {
            ...group,
            directoryZones: [],
            memberCount: group.memberCount || (Array.isArray(group.members) ? group.members.length : 0)
          });
        }
        const current = byName.get(name);
        current.directoryZones = Array.from(new Set([
          ...current.directoryZones,
          ...this.directoryZones(group.dn)
        ]));
        const count = group.memberCount || (Array.isArray(group.members) ? group.members.length : 0);
        current.memberCount = Math.max(current.memberCount || 0, count);
        if (!current.dn && group.dn) current.dn = group.dn;
      });
      return Array.from(byName.values()).map(group => ({
        ...group,
        directoryZones: group.directoryZones.length ? group.directoryZones : ['accounts']
      }));
    },
    filteredGroups() {
      const keyword = this.searchKeyword.trim().toLowerCase();
      return this.processedGroups.filter(group => {
        const count = group.memberCount || 0;
        if (this.statusFilter === 'ACTIVE' && count <= 0) return false;
        if (this.statusFilter === 'EMPTY' && count > 0) return false;
        if (!keyword) return true;
        return String(group.name || '').toLowerCase().includes(keyword)
          || String(group.description || '').toLowerCase().includes(keyword);
      });
    }
  },
  async mounted() {
    await this.fetchClusters();
    if (this.cluster) {
      this.selectedCluster = this.cluster;
    } else if (store.headerSelectedCluster) {
      this.selectedCluster = store.headerSelectedCluster;
    }
    if (!this.selectedCluster && this.clusters.length) {
      this.selectedCluster = this.clusters[0].clusterCode || this.clusters[0].clusterName;
    }
    this.fetchGroups();
  },
  methods: {
    async fetchClusters() {
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = (res.data || []).filter(item => item.status !== 'DELETED');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载集群失败');
      }
    },
    async fetchGroups() {
      if (!this.selectedCluster) {
        this.groups = [];
        return;
      }
      this.loading = true;
      try {
        const res = await axios.get('/api/access/ldap-groups', {
          params: { cluster: this.selectedCluster }
        });
        this.groups = Array.isArray(res.data) ? res.data : [];
      } catch (e) {
        this.groups = [];
        this.$message.error(e.response?.data?.message || '加载 LDAP 组失败');
      } finally {
        this.loading = false;
      }
    },
    currentSupplementaryGroupNames() {
      return Array.from(new Set(this.supplementaryGroupNames || []));
    },
    relationParams() {
      const cluster = this.cluster || this.selectedCluster;
      if (!this.username || !cluster) {
        this.$message.warning('请先选择用户和集群');
        return null;
      }
      return {
        username: encodeURIComponent(this.username),
        params: { cluster }
      };
    },
    async setAsPrimaryGroup(record) {
      const context = this.relationParams();
      if (!context) return;
      this.relationSaving = true;
      try {
        await axios.put(`/api/access/user/${context.username}/ldap-group`, {
          groupName: record.name
        }, { params: context.params });
        this.$message.success('主组已更新');
        this.$emit('refresh-profile');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '更新主组失败');
      } finally {
        this.relationSaving = false;
      }
    },
    async addSupplementaryGroup(record) {
      const context = this.relationParams();
      if (!context) return;
      const groupNames = Array.from(new Set([...this.currentSupplementaryGroupNames(), record.name]));
      this.relationSaving = true;
      try {
        await axios.put(`/api/access/user/${context.username}/ldap-supplementary-groups`, {
          groupNames
        }, { params: context.params });
        this.$message.success('已加入附加组');
        this.$emit('refresh-profile');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加入附加组失败');
      } finally {
        this.relationSaving = false;
      }
    },
    async removeSupplementaryGroup(record) {
      const context = this.relationParams();
      if (!context) return;
      const groupNames = this.currentSupplementaryGroupNames().filter(name => name !== record.name);
      this.relationSaving = true;
      try {
        await axios.put(`/api/access/user/${context.username}/ldap-supplementary-groups`, {
          groupNames
        }, { params: context.params });
        this.$message.success('已移除附加组');
        this.$emit('refresh-profile');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '移除附加组失败');
      } finally {
        this.relationSaving = false;
      }
    },
    openCreateModal() {
      if (!this.selectedCluster) {
        this.$message.warning('请先选择集群');
        return;
      }
      this.editingGroup = null;
      this.form = { name: '', gidNumber: null, description: '', membersText: '' };
      this.modalVisible = true;
    },
    openEditModal(record) {
      this.editingGroup = record;
      this.form = {
        name: record.name,
        gidNumber: record.gidNumber,
        description: record.description || '',
        membersText: Array.isArray(record.members) ? record.members.join('\n') : ''
      };
      this.modalVisible = true;
    },
    parseMembers() {
      return String(this.form.membersText || '')
        .split(/[\n,]+/)
        .map(item => item.trim())
        .filter(Boolean);
    },
    directoryZones(dn) {
      const value = String(dn || '').toLowerCase();
      const zones = [];
      if (value.includes('accounts')) zones.push('accounts');
      if (value.includes('compat')) zones.push('compat');
      return zones;
    },
    zoneTagColor(zone) {
      if (zone === 'accounts') return 'blue';
      if (zone === 'compat') return 'purple';
      return 'default';
    },
    memberStatusLabel(record) {
      return (record.memberCount || 0) > 0 ? 'Active' : 'Empty';
    },
    memberStatusColor(record) {
      return (record.memberCount || 0) > 0 ? 'green' : 'orange';
    },
    isPrimaryGroup(group) {
      return group && group.name && group.name === this.primaryGroupName;
    },
    isSupplementaryGroup(group) {
      return group && group.name && this.supplementaryGroupNames.includes(group.name);
    },
    groupRowClassName(record) {
      if (this.isPrimaryGroup(record)) return 'primary-group-row';
      if (!record.memberCount) return 'empty-group-row';
      return '';
    },
    membersPreview(record) {
      const members = Array.isArray(record.members) ? record.members : [];
      if (!members.length) return '暂无成员';
      const preview = members.slice(0, 8).join(', ');
      return members.length > 8 ? `${preview} 等 ${members.length} 人` : preview;
    },
    openTechDrawer(record) {
      this.currentTechGroup = record;
      this.techDrawerVisible = true;
    },
    async submitGroup() {
      if (!this.form.name || !this.form.name.trim()) {
        this.$message.warning('请输入组名');
        return;
      }
      this.saving = true;
      try {
        const payload = {
          name: this.form.name.trim(),
          gidNumber: this.form.gidNumber,
          description: this.form.description,
          members: this.parseMembers()
        };
        if (this.editingGroup) {
          await axios.put(`/api/access/ldap-groups/${encodeURIComponent(this.editingGroup.name)}`, payload, {
            params: { cluster: this.selectedCluster }
          });
        } else {
          await axios.post('/api/access/ldap-groups', payload, {
            params: { cluster: this.selectedCluster }
          });
          if (payload.members.length) {
            await axios.put(`/api/access/ldap-groups/${encodeURIComponent(payload.name)}/members`, {
              members: payload.members
            }, { params: { cluster: this.selectedCluster } });
          }
        }
        this.modalVisible = false;
        this.$message.success('LDAP 组已保存');
        this.fetchGroups();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存 LDAP 组失败');
      } finally {
        this.saving = false;
      }
    },
    async deleteGroup(record) {
      try {
        await axios.delete(`/api/access/ldap-groups/${encodeURIComponent(record.name)}`, {
          params: { cluster: this.selectedCluster, force: true }
        });
        this.$message.success('LDAP 组已删除');
        this.fetchGroups();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '删除 LDAP 组失败');
      }
    }
  }
};
</script>

<style scoped>
.ldap-group-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.group-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.ldap-group-manager.compact .group-toolbar {
  padding: 16px;
}
.group-toolbar h2 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}
.group-toolbar p {
  margin: 0;
  color: #5f6b7a;
}
.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.group-search {
  width: 200px;
}
.status-filter {
  width: 120px;
}
.cluster-select {
  width: 260px;
}
.group-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.relation-table ::v-deep .ant-table-thead > tr > th {
  color: #667085;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
}
.relation-table ::v-deep .ant-table-tbody > tr > td {
  padding: 10px 12px;
  border-bottom: 1px solid #edf0f5;
}
.relation-table ::v-deep .ant-table-tbody > tr:hover > td {
  background: #f6faff !important;
}
.relation-table ::v-deep .primary-group-row > td {
  background: #eff8ff;
}
.relation-table ::v-deep .empty-group-row > td {
  background: #fffaf0;
}
.group-main {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}
.group-avatar {
  background: #eef4ff;
  color: #175cd3;
  flex: 0 0 auto;
}
.group-title-content {
  min-width: 0;
}
.group-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.group-title-line h3 {
  margin: 0;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-title-content p {
  margin: 2px 0 0;
  color: #667085;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.zone-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.zone-tags .ant-tag {
  margin-right: 0;
}
.row-action {
  border: 0;
  color: #667085;
  box-shadow: none;
}
.member-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #667085;
}
.member-status .ant-tag {
  margin-right: 0;
}
.member-status strong {
  color: #111827;
  font-size: 16px;
  line-height: 20px;
}
.soft-text {
  color: #667085;
}
.member-count {
  color: #111827;
}
.muted {
  color: #98a2b3;
}
.danger-action {
  color: #cf1322;
}
.group-details-collapse {
  grid-column: 1 / -1;
  margin-top: 2px;
  border: 0;
  background: transparent;
}
.group-details-collapse ::v-deep .ant-collapse-item {
  border-bottom: 0;
}
.group-details-collapse ::v-deep .ant-collapse-header {
  padding: 4px 0 4px 24px;
  color: #667085;
  font-size: 12px;
}
.group-details-collapse ::v-deep .ant-collapse-content {
  border-top: 0;
  background: transparent;
}
.group-details-collapse ::v-deep .ant-collapse-content-box {
  padding: 8px 0 0;
}
.detail-list {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) 120px minmax(0, 1fr);
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  background: #fff;
}
.detail-list span {
  display: block;
  margin-bottom: 4px;
  color: #667085;
  font-size: 12px;
}
.detail-list strong {
  display: block;
  color: #344054;
  font-weight: 500;
  word-break: break-all;
}
@media (max-width: 768px) {
  .group-toolbar {
    flex-direction: column;
  }
  .cluster-select {
    width: 100%;
  }
  .group-search,
  .status-filter {
    width: 100%;
  }
  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .detail-list {
    grid-template-columns: 1fr;
  }
}
</style>
