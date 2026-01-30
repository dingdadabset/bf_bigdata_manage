<template>
  <a-card :title="title" :bordered="false" class="perm-card">
    <div slot="extra">
      <a-button type="link" icon="sync" @click="handleSync" :loading="syncing" style="margin-right: 8px;">同步权限</a-button>
      <a-button type="link" icon="edit" @click="$emit('edit')">编辑策略</a-button>
    </div>
    <a-tree
      v-if="treeData && treeData.length"
      :tree-data="treeData"
      :load-data="loadTables"
      show-icon
      :expanded-keys="expandedKeys"
      :auto-expand-parent="autoExpandParent"
      @expand="onExpand"
      @select="onSelect"
    >
      <a-icon slot="database" type="database" />
      <a-icon slot="table" type="table" />
      <template slot="custom" slot-scope="item">
        <span class="node-title">
          <span class="node-text">{{ item.title }}</span>
          <a-tag v-if="item.levelTag" color="blue" style="margin-left: 8px; font-size: 10px">{{ item.levelTag }}</a-tag>
          <a-tag v-if="item.perms" color="orange" style="margin-left: 8px; font-size: 10px">{{ item.perms }}</a-tag>
        </span>
      </template>
    </a-tree>
    <a-empty v-else description="暂无权限策略" />
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
      expandedKeys: [],
      autoExpandParent: false,
      syncing: false
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
    }
  },
  methods: {
    async loadUserAccess() {
      try {
        const params = { username: this.username };
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const res = await axios.get('/api/access/user/access', {
          params
        });
        const records = res.data || [];
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
        this.treeData = Object.values(byDb);
      } catch (e) {
        const mock = store.permissions[this.username] || [];
        this.treeData = mock.map(db => ({
          ...db,
          scopedSlots: { title: 'custom' },
          levelTag: '库',
          children: (db.children || []).map(t => ({ ...t, levelTag: '表' }))
        }));
      }
    },
    async loadTables(treeNode) {
      if (treeNode.dataRef && treeNode.dataRef.children && treeNode.dataRef.children.length) {
        return;
      }
      const db = treeNode.dataRef.title;
      try {
        const params = { database: db };
        if (this.effectiveCluster) {
          params.cluster = this.effectiveCluster;
        }
        const res = await axios.get('/api/access/hive/tables', { params });
        const tables = res.data || [];
        treeNode.dataRef.children = tables.map(t => ({
          title: t,
          key: `tbl-${db}-${t}`,
          slots: { icon: 'table' },
          scopedSlots: { title: 'custom' },
          levelTag: '表'
        }));
      } catch (e) {
        treeNode.dataRef.children = [];
      }
    },
    onExpand() {},
    onSelect(selectedKeys, info) {
      console.log('selected', selectedKeys, info);
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
.perm-card .ant-tree {
  margin-left: 0;
}
.perm-card >>> .ant-tree .ant-tree-treenode {
  min-height: 24px;
}
.perm-card >>> .ant-tree .ant-tree-iconEle {
  display: inline-flex;
  align-items: center;
}
.perm-card >>> .ant-tree .ant-tree-switcher {
  display: none;
}
.perm-card >>> .ant-tree .ant-tree-node-content-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  min-height: 24px;
  height: 24px;
  padding: 0;
  cursor: pointer;
}
.node-title {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
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
  height: 24px;
  line-height: 24px;
}
</style>
