import Vue from 'vue';

export const store = Vue.observable({
  currentCluster: '',
  currentUser: null,
  // Mock Data Store
  users: [
    { username: 'dingquan', creationStrategy: 'SELF_REG', role: 'Admin', createTime: new Date(), cluster: 'CDH-Cluster-01', status: 'ACTIVE' },
    { username: 'test_user', creationStrategy: 'LDAP', role: 'Data Analyst', createTime: new Date(), cluster: 'HDP-Production', status: 'ACTIVE' },
    { username: 'ipa_user', creationStrategy: 'IPA_HTTP', role: 'Developer', createTime: new Date(), cluster: 'CDH-Cluster-01', status: 'ACTIVE' },
    { username: 'sr_user', creationStrategy: 'LDAP', role: 'Analyst', createTime: new Date(), cluster: 'StarRocks-Financial', status: 'ACTIVE' }
  ],
  permissions: {
    'dingquan': [
      {
        title: 'finance_db',
        key: 'db-1',
        slots: { icon: 'database' },
        children: [
          { title: 'transactions', key: 'tbl-1', slots: { icon: 'table' }, scopedSlots: { title: 'custom' }, perms: 'SELECT, UPDATE' }
        ]
      }
    ],
    'test_user': []
  }
});

export const mutations = {
  setCluster(cluster) {
    store.currentCluster = cluster;
  },
  setUser(user) {
    store.currentUser = user;
  },
  setUsers(users) {
    store.users = users;
  },
  addUser(user) {
    store.users.unshift(user);
    // Initialize empty permissions
    if (!store.permissions[user.username]) {
      Vue.set(store.permissions, user.username, []);
    }
  },
  deleteUser(username) {
    store.users = store.users.filter(u => u.username !== username);
    Vue.delete(store.permissions, username);
  },
  addPermission(username, permission) {
    if (!store.permissions[username]) {
      Vue.set(store.permissions, username, []);
    }
    
    const userPerms = store.permissions[username];
    // Simple logic: check if db exists, if so add table, else add db
    // For simplicity in this mock, we just append or update
    
    // Check if DB node exists
    let dbNode = userPerms.find(p => p.title === permission.database);
    if (!dbNode) {
      dbNode = {
        title: permission.database,
        key: `db-${Date.now()}`,
        slots: { icon: 'database' },
        children: []
      };
      userPerms.push(dbNode);
    }
    
    if (permission.level === 'TABLE') {
       permission.tables.forEach(tbl => {
         const existingTbl = dbNode.children.find(t => t.title === tbl);
         if (existingTbl) {
           existingTbl.perms = permission.perms; // Update
         } else {
           dbNode.children.push({
             title: tbl,
             key: `tbl-${Date.now()}-${Math.random()}`,
             slots: { icon: 'table' },
             scopedSlots: { title: 'custom' },
             perms: permission.perms
           });
         }
       });
    } else {
       // DB Level permission - maybe add a special node or property
       // For visualization, we can just mark the DB node
       dbNode.perms = permission.perms;
    }
  }
};
