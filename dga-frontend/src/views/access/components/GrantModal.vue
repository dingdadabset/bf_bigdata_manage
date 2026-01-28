<template>
  <a-modal
    :visible="visible"
    title="Hive 权限管理"
    width="800px"
    :footer="null"
    :destroyOnClose="true"
    @cancel="$emit('cancel')"
  >
    <a-form-model :model="hiveForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="操作类型">
        <a-radio-group v-model="hiveForm.actionType">
          <a-radio value="GRANT">授权 (Grant)</a-radio>
          <a-radio value="REVOKE">回收 (Revoke)</a-radio>
        </a-radio-group>
      </a-form-model-item>
      <a-form-model-item label="目标用户">
        <a-tag color="blue">{{ username }}</a-tag>
      </a-form-model-item>
      <a-form-model-item label="授权维度">
          <a-radio-group v-model="hiveForm.level">
          <a-radio-button value="DATABASE">库级授权</a-radio-button>
          <a-radio-button value="TABLE">表级授权</a-radio-button>
        </a-radio-group>
      </a-form-model-item>
      <a-form-model-item label="选择数据库" v-if="hiveForm.level === 'DATABASE'">
        <a-select
          v-model="hiveForm.selectedDatabases"
          mode="multiple"
          style="width: 100%"
          placeholder="请选择一个或多个数据库"
          :loading="loadingHiveMeta"
        >
          <a-select-option v-for="db in databases" :key="db" :value="db">
            {{ db }}
          </a-select-option>
        </a-select>
      </a-form-model-item>
      <template v-else>
        <a-form-model-item label="数据库">
          <a-select
            v-model="hiveForm.currentDatabase"
            style="width: 100%"
            placeholder="请选择数据库"
            :loading="loadingHiveMeta"
            @change="onTableDatabaseChange"
          >
            <a-select-option v-for="db in databases" :key="db" :value="db">
              {{ db }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="选择表">
          <a-select
            v-model="hiveForm.selectedTables"
            mode="multiple"
            style="width: 100%"
            :disabled="!hiveForm.currentDatabase"
            placeholder="请选择一个或多个表"
            :loading="loadingHiveMeta"
          >
            <a-select-option v-for="table in tables" :key="table" :value="table">
              {{ table }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
      </template>
      <a-form-model-item label="权限类型">
        <a-select v-model="hiveForm.permission" style="width: 100%">
          <a-select-option value="ALL">ALL</a-select-option>
          <a-select-option value="SELECT">SELECT</a-select-option>
          <a-select-option value="INSERT">INSERT</a-select-option>
          <a-select-option value="CREATE">CREATE</a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 6 }">
        <a-button @click="$emit('cancel')" style="margin-right: 8px">
          取消
        </a-button>
        <a-button type="primary" @click="submitGrant" :loading="granting">
          执行授权
        </a-button>
      </a-form-model-item>
    </a-form-model>
  </a-modal>
</template>

<script>
import axios from 'axios';
import { store, mutations } from '../../../store';

export default {
  name: 'GrantModal',
  props: {
    visible: Boolean,
    username: String
  },
  data() {
    return {
      loadingHiveMeta: false,
      granting: false,
      databases: [],
      tables: [],
      hiveForm: {
        actionType: 'GRANT',
        level: 'DATABASE',
        permission: 'SELECT',
        selectedDatabases: [],
        currentDatabase: '',
        selectedTables: []
      }
    };
  },
  watch: {
    visible(val) {
      if (val) {
        if (this.hiveForm.actionType === 'REVOKE') {
          this.loadUserAccess();
        } else {
          this.loadDatabases();
        }
      }
    },
    'hiveForm.actionType'(val) {
      if (val === 'REVOKE') {
        this.loadUserAccess();
        this.hiveForm.currentDatabase = '';
        this.hiveForm.selectedDatabases = [];
        this.hiveForm.selectedTables = [];
      } else {
        this.loadDatabases();
      }
    }
  },
  methods: {
    async loadUserAccess() {
      this.loadingHiveMeta = true;
      try {
        const params = { username: this.username };
        if (store.currentCluster) {
          params.cluster = store.currentCluster;
        }
        const res = await axios.get('/api/access/user/access', { params });
        const accessList = res.data;
        
        // Extract unique databases
        const dbs = new Set();
        accessList.forEach(item => {
          if (item.databaseName) dbs.add(item.databaseName);
        });
        this.databases = Array.from(dbs);
        
        // Store access list for table filtering
        this.userAccessList = accessList;
        
      } catch (e) {
        this.databases = [];
        this.$message.error('加载用户已有权限失败');
      } finally {
        this.loadingHiveMeta = false;
      }
    },
    async loadDatabases() {
      if (this.hiveForm.actionType === 'REVOKE') return; // Should not happen but safety check
      this.loadingHiveMeta = true;
      try {
        const params = {};
        if (store.currentCluster) {
          params.cluster = store.currentCluster;
        }
        const res = await axios.get('/api/access/hive/databases', { params });
        this.databases = res.data;
      } catch (e) {
        // Mock
        this.databases = ['finance_db', 'ods_db', 'default'];
      } finally {
        this.loadingHiveMeta = false;
      }
    },
    async onTableDatabaseChange(val) {
      if(!val) return;
      this.loadingHiveMeta = true;
      
      if (this.hiveForm.actionType === 'REVOKE') {
        try {
          // Filter tables from loaded user access list
          const tables = new Set();
          if (this.userAccessList) {
            this.userAccessList.forEach(item => {
              if (item.databaseName === val && item.tableName) {
                tables.add(item.tableName);
              }
            });
          }
          this.tables = Array.from(tables);
        } finally {
          this.loadingHiveMeta = false;
        }
        return;
      }

      try {
        const params = { database: val };
        if (store.currentCluster) {
          params.cluster = store.currentCluster;
        }
        const res = await axios.get('/api/access/hive/tables', { params });
        this.tables = res.data;
      } catch (e) {
        // Mock
        this.tables = ['table_01', 'table_02', 'user_info', 'transactions'];
      } finally {
        this.loadingHiveMeta = false;
      }
    },
    async submitGrant() {
      this.granting = true;
      try {
        const payload = {
          username: this.username,
          permission: this.hiveForm.permission,
          level: this.hiveForm.level,
          cluster: store.currentCluster || 'CDH-Cluster-01'
        };
        if (this.hiveForm.level === 'DATABASE') {
          payload.databases = this.hiveForm.selectedDatabases && this.hiveForm.selectedDatabases.length
            ? this.hiveForm.selectedDatabases
            : [this.hiveForm.currentDatabase || 'default'];
        } else {
          payload.tables = (this.hiveForm.selectedTables || []).map(t => ({ database: this.hiveForm.currentDatabase, table: t }));
        }
        
        const apiUrl = this.hiveForm.actionType === 'REVOKE' 
          ? '/api/access/revoke/batch' 
          : '/api/access/grant/batch';
          
        await axios.post(apiUrl, payload);
        
        if (this.hiveForm.actionType === 'GRANT') {
          const storePayload = {
            level: this.hiveForm.level,
            perms: this.hiveForm.permission,
            database: this.hiveForm.level === 'DATABASE' ? (payload.databases[0] || 'default') : this.hiveForm.currentDatabase,
            tables: this.hiveForm.level === 'TABLE' ? this.hiveForm.selectedTables : []
          };
          mutations.addPermission(this.username, storePayload);
        }
        // For REVOKE, we might want to refresh the list, which emits 'ok' will do if parent handles it
        
        this.$message.success(this.hiveForm.actionType === 'REVOKE' ? '权限回收成功' : '授权成功，已记录授权明细');
        this.$emit('ok');
      } catch (e) {
        this.$message.error(this.hiveForm.actionType === 'REVOKE' ? '回收失败' : '授权失败');
      } finally {
        this.granting = false;
      }
    }
  }
};
</script>
