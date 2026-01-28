<template>
  <a-card title="StarRocks 权限" :bordered="false" class="perm-card">
    <a-button slot="extra" type="link" icon="edit">管理角色</a-button>
    <a-list item-layout="horizontal" :data-source="mockStarRocksData">
      <a-list-item slot="renderItem" slot-scope="item">
        <a-list-item-meta :description="item.scope">
          <span slot="title">{{ item.roleName }}</span>
          <a-avatar slot="avatar" style="backgroundColor: #722ed1" icon="lock" />
        </a-list-item-meta>
        <div>
          <a-tag color="purple" v-for="p in item.privileges" :key="p">{{ p }}</a-tag>
        </div>
      </a-list-item>
    </a-list>
  </a-card>
</template>

<script>
export default {
  name: 'StarRocksCard',
  data() {
    return {
      mockStarRocksData: [
        { roleName: 'db_admin', scope: 'Global', privileges: ['GRANT', 'DROP', 'ALTER'] },
        { roleName: 'finance_analyst', scope: 'Database: finance', privileges: ['SELECT'] }
      ]
    };
  }
};
</script>
