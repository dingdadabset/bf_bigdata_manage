<template>
  <a-card :title="title" :bordered="false" class="perm-card">
    <div slot="extra">
      <a-button type="link" icon="sync" @click="handleSync" :loading="syncing" style="margin-right: 8px;">同步权限</a-button>
      <a-button type="link" icon="edit" @click="$emit('edit')">编辑策略</a-button>
    </div>
    <div v-if="(dbList && dbList.length) || (revokedDbList && revokedDbList.length)" class="access-sections">
      <div class="section-title">当前授权</div>
      <div v-if="dbList && dbList.length" class="db-list">
        <div class="db-row" v-for="db in dbList" :key="db.key">
          <div class="db-left">
            <a-icon type="database" style="margin-right: 8px;" />
            <span class="db-name">{{ db.title }}</span>
            <a-tag color="blue" style="margin-left: 8px; font-size: 10px">库</a-tag>
            <a-tag v-if="db.perms" color="orange" style="margin-left: 8px; font-size: 10px">{{ db.perms }}</a-tag>
          </div>
          <div class="db-actions">
            <a-button class="db-action" type="link" icon="table" @click="openTables(db.title, db.perms)">查看表</a-button>
            <a-button
              v-if="db.children && db.children.length"
              class="db-action"
              type="link"
              icon="bars"
              @click="openGrantedTables(db)"
            >
              已授权表
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无当前授权" />

      <a-divider style="margin: 12px 0" />

      <div class="section-title">已回收</div>
      <div v-if="revokedDbList && revokedDbList.length" class="db-list revoked-list">
        <div class="db-row" v-for="db in revokedDbList" :key="db.key">
          <div class="db-left">
            <a-icon type="database" style="margin-right: 8px;" />
            <span class="db-name">{{ db.title }}</span>
            <a-tag style="margin-left: 8px; font-size: 10px">库</a-tag>
            <a-tag v-if="db.perms" color="orange" style="margin-left: 8px; font-size: 10px">{{ db.perms }}</a-tag>
            <a-tag color="red" style="margin-left: 8px; font-size: 10px">已回收</a-tag>
          </div>
          <div class="db-actions">
            <a-button
              v-if="db.children && db.children.length"
              class="db-action"
              type="link"
              icon="bars"
              @click="openGrantedTables(db)"
            >
              已授权表
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无回收记录" />
    </div>
    <a-empty v-else description="暂无权限策略" />
    <a-modal :visible="tablesVisible" :title="`库 ${tablesDb} 的表`" @cancel="tablesVisible=false" @ok="tablesVisible=false" width="520px">
      <a-list :data-source="tablesData" bordered size="small">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-icon type="table" style="margin-right: 8px;" />
          <span class="table-name">{{ item }}</span>
          <a-tag color="blue" style="margin-left: 8px; font-size: 10px">表</a-tag>
          <a-tag v-if="tablesPerm" color="orange" style="margin-left: 8px; font-size: 10px">{{ tablesPerm }}</a-tag>
        </a-list-item>
        <div slot="header">共 {{ tablesData.length }} 张表</div>
      </a-list>
    </a-modal>
    <a-modal
      :visible="grantedTablesVisible"
      :title="`库 ${grantedTablesDb} 的已授权表`"
      @cancel="grantedTablesVisible=false"
      @ok="grantedTablesVisible=false"
      width="520px"
    >
      <a-list :data-source="grantedTablesData" bordered size="small">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-icon type="table" style="margin-right: 8px;" />
          <span class="table-name">{{ item.tableName }}</span>
          <a-tag color="blue" style="margin-left: 8px; font-size: 10px">表</a-tag>
          <a-tag v-if="item.permission" color="orange" style="margin-left: 8px; font-size: 10px">{{ item.permission }}</a-tag>
        </a-list-item>
        <div slot="header">共 {{ grantedTablesData.length }} 张表</div>
      </a-list>
    </a-modal>
  </a-card>
</template>

<script>
import axios from 'axios';
import { store } from '../../../store';

export default {
  name: 'RangerCard',
  props: {
    username: String,
    cluster: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: 'Hive 数据权限 (Ranger)'
    }
  },
  data() {
    return {
      treeData: [],
      revokedTreeData: [],
      syncing: false,
      tablesVisible: false,
      tablesDb: '',
      tablesPerm: '',
      tablesData: [],
      grantedTablesVisible: false,
      grantedTablesDb: '',
      grantedTablesData: []
    };
  },
  watch: {
    username: {
      immediate: true,
      handler() {
        this.loadUserAccess();
      }
    },
    cluster() {
      if (this.username) {
        this.loadUserAccess();
      }
    },
    currentClusterFromStore() {
      if (this.username) {
        this.loadUserAccess();
      }
    }
  },
  computed: {
    currentClusterFromStore() {
      return store.currentCluster;
    },
    effectiveCluster() {
      return this.cluster || this.currentClusterFromStore;
    },
    dbList() {
      return this.treeData || [];
    },
    revokedDbList() {
      return this.revokedTreeData || [];
    }
  },
  methods: {
    async loadUserAccess() {
      try {
        const buildTree = records => {
          const byDb = {};
          records.forEach(r => {
            if (!byDb[r.databaseName]) {
              byDb[r.databaseName] = {
                title: r.databaseName,
                key: `db-${r.databaseName}`,
                slots: { icon: 'database' },
                scopedSlots: { title: 'custom' },
                levelTag: '库',
                perms: null,
                children: []
              };
            }
            if (r.tableName) {
              byDb[r.databaseName].children.push({
                title: r.tableName,
                key: `tbl-${r.databaseName}-${r.tableName}`,
                slots: { icon: 'table' },
                scopedSlots: { title: 'custom' },
                levelTag: '表',
                perms: r.permission
              });
            } else {
              byDb[r.databaseName].perms = r.permission;
            }
          });
          return Object.values(byDb).map(node => {
            if (node.children && node.children.length === 0) {
              delete node.children;
            }
            return node;
          });
        };

        const activeParams = { username: this.username, status: 'ACTIVE' };
        const revokedParams = { username: this.username, status: 'REVOKED', includeDeleted: true };
        if (this.effectiveCluster) {
          activeParams.cluster = this.effectiveCluster;
          revokedParams.cluster = this.effectiveCluster;
        }

        const [activeRes, revokedRes] = await Promise.all([
          axios.get('/api/access/user/access', { params: activeParams }),
          axios.get('/api/access/user/access', { params: revokedParams })
        ]);

        const activeRecords = activeRes.data || [];
        const revokedRecordsRaw = revokedRes.data || [];
        const revokedRecords = revokedRecordsRaw.filter(r => r.deleted === true);

        this.treeData = buildTree(activeRecords);
        this.revokedTreeData = buildTree(revokedRecords);
      } catch (e) {
        const mock = store.permissions[this.username] || [];
        this.treeData = mock.map(db => ({
          ...db,
          scopedSlots: { title: 'custom' },
          levelTag: '库',
          children: (db.children || []).map(t => ({ ...t, levelTag: '表' }))
        }));
        this.revokedTreeData = [];
      }
    },
    async openTables(dbName, perms) {
      this.tablesVisible = true;
      this.tablesDb = dbName;
      this.tablesPerm = perms || '';
      try {
        const params = { database: dbName };
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const res = await axios.get('/api/access/resources/tables', { params });
        this.tablesData = res.data || [];
      } catch (e) {
        this.tablesData = [];
      }
    },
    openGrantedTables(db) {
      this.grantedTablesVisible = true;
      this.grantedTablesDb = db.title;
      this.grantedTablesData = (db.children || []).map(t => ({
        tableName: t.title,
        permission: t.perms
      }));
    },
    async handleSync() {
      this.syncing = true;
      try {
        const params = {};
        if (this.effectiveCluster) {
            params.cluster = this.effectiveCluster;
        }
        const res = await axios.post(`/api/access/sync/${this.username}`, null, { params });
        this.$message.success(res.data);
        this.loadUserAccess();
      } catch (e) {
        this.$message.error('同步失败: ' + (e.response ? e.response.data.message : e.message));
      } finally {
        this.syncing = false;
      }
    }
  }
};
</script>

<style scoped>
.perm-card .ant-card-body {
  text-align: left;
  overflow-x: hidden;
}
.db-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.db-row {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  width: 100%;
}
.db-left {
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  min-width: 0;
}
.db-action {
  margin-left: auto;
}
.db-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}
.section-title {
  font-weight: 600;
  font-size: 13px;
  margin: 0 0 8px 8px;
}
.revoked-list .db-name {
  color: rgba(0, 0, 0, 0.45);
}
.db-name, .table-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-split {
  display: flex;
  gap: 16px;
}
.left-tree, .right-tree {
  flex: 1 1 50%;
  min-width: 280px;
}
.perm-card .ant-tree {
  margin-left: 0;
  padding-left: 0;
}
.perm-card >>> .ant-tree .ant-tree-treenode {
  display: grid;
  grid-template-columns: 20px auto;
  align-items: center;
  justify-content: start;
  padding: 0 0 4px 0;
  width: 100%;
}
.perm-card >>> .ant-tree .ant-tree-iconEle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  line-height: 24px;
  width: 20px;
  min-width: 20px;
}
.perm-card >>> .ant-tree .ant-tree-switcher {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  line-height: 24px;
  width: 20px;
  min-width: 20px;
  margin-right: 4px;
}
.perm-card >>> .ant-tree .ant-tree-switcher-noop {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  line-height: 24px;
  width: 20px;
  min-width: 20px;
  margin-right: 4px;
}
.perm-card >>> .ant-tree .ant-tree-node-content-wrapper {
  display: grid;
  grid-template-columns: 20px auto;
  align-items: center;
  min-height: 24px;
  height: 24px;
  padding: 0;
  cursor: pointer;
  line-height: 24px;
  justify-content: flex-start;
  text-align: left;
  width: auto;
  margin-left: 0;
}
.perm-card >>> .ant-tree .ant-tree-title {
  display: inline-flex;
  align-items: center;
  height: 100%;
}
.perm-card >>> .ant-tree .ant-tree-child-tree,
.perm-card >>> .ant-tree .ant-tree-child-tree-open {
  padding-left: 0;
  margin-left: 0;
}
.node-title {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  height: 100%;
}
.node-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-title .ant-tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  line-height: 20px;
  margin-top: 0;
  margin-bottom: 0;
}
.el-node-title {
  display: inline-flex;
  align-items: center;
}
.el-node-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
