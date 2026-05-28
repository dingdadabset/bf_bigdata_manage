export function parseBatchUsers(text) {
  return String(text || '')
    .split(/[\n,]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

export function batchStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SUCCESS') return 'green';
  if (normalized === 'FAILED') return 'red';
  if (normalized === 'PENDING_GROUP_MAPPING') return 'orange';
  if (normalized === 'SKIPPED') return 'blue';
  return 'default';
}

export function batchActionText(action) {
  const map = {
    WILL_BIND: '将绑定',
    ALREADY_BOUND: '已绑定',
    DUPLICATE: '重复跳过',
    INVALID: '无效',
    BOUND: '已处理',
    SKIPPED: '已跳过'
  };
  return map[action] || action || '-';
}

export function permissionKey(permission) {
  return [
    String(permission?.resourceType || '').toUpperCase(),
    String(permission?.databaseName || '').toLowerCase(),
    String(permission?.tableName || '*').toLowerCase(),
    normalizePermissionName(permission?.permission),
    String(permission?.authBackend || '').toUpperCase()
  ].join('|');
}

export function assignmentKey(assignment) {
  return [
    String(assignment?.subjectType || '').toUpperCase(),
    String(assignment?.subjectName || '').toLowerCase(),
    String(assignment?.authBackend || '').toUpperCase(),
    String(assignment?.backendSyncStatus || '').toUpperCase()
  ].join('|');
}

export function uniqueBy(items, keyFn) {
  const seen = new Set();
  return (items || []).filter(item => {
    const key = keyFn(item);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

export function normalizePermissionName(permission) {
  let normalized = String(permission || '').trim().toUpperCase();
  if (normalized.endsWith('_PRIV')) {
    normalized = normalized.slice(0, -5);
  }
  return normalized === '*' || normalized === 'ALL PRIVILEGES' || normalized === 'ALL_PRIVILEGES' ? 'ALL' : normalized;
}

export function uniqueRolePermissions(roleView) {
  return uniqueBy(roleView && roleView.permissions ? roleView.permissions : [], permissionKey);
}

export function rolePermissionText(permission) {
  const database = permission?.databaseName || '*';
  const table = permission?.tableName || '*';
  return `${permission?.resourceType || 'RESOURCE'} ${database}.${table} ${normalizePermissionName(permission?.permission)}`;
}

export function rolePermissionShortText(permission) {
  const database = permission?.databaseName || '*';
  const table = permission?.tableName ? `.${permission.tableName}` : '.*';
  return `${database}${table} ${normalizePermissionName(permission?.permission)}`;
}

export function operationModeLabel(mode) {
  return String(mode || '').toUpperCase() === 'DIRECT_EXCEPTION' ? '直接例外' : '角色模式';
}

export function filterRolePermissions(permissions, keyword, permissionFilter = 'ALL') {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase();
  const normalizedFilter = String(permissionFilter || 'ALL').toUpperCase();
  return (permissions || []).filter(item => {
    const permission = normalizePermissionName(item?.permission);
    if (normalizedFilter !== 'ALL' && permission !== normalizedFilter) return false;
    if (!normalizedKeyword) return true;
    return [
      item?.databaseName,
      item?.tableName,
      item?.permission,
      item?.resourceType,
      item?.authBackend
    ].some(field => String(field || '').toLowerCase().includes(normalizedKeyword));
  });
}

export function filterRoleAssignments(assignments, keyword, statusFilter = 'ALL', subjectTypeFilter = 'ALL') {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase();
  const normalizedStatus = String(statusFilter || 'ALL').toUpperCase();
  const normalizedSubjectType = String(subjectTypeFilter || 'ALL').toUpperCase();
  return (assignments || []).filter(item => {
    const status = String(item?.backendSyncStatus || 'UNKNOWN').toUpperCase();
    const subjectType = String(item?.subjectType || 'UNKNOWN').toUpperCase();
    if (normalizedStatus !== 'ALL' && status !== normalizedStatus) return false;
    if (normalizedSubjectType !== 'ALL' && subjectType !== normalizedSubjectType) return false;
    if (!normalizedKeyword) return true;
    return [
      item?.subjectName,
      item?.authBackend,
      item?.backendSyncStatus,
      item?.subjectType
    ].some(field => String(field || '').toLowerCase().includes(normalizedKeyword));
  });
}

export function rolePermissionSelection(permission) {
  const selection = {
    resourceType: permission?.resourceType || (permission?.tableName ? 'TABLE' : 'DATABASE'),
    databaseName: permission?.databaseName || '',
    tableName: permission?.tableName || null,
    permission: normalizePermissionName(permission?.permission),
    authBackend: permission?.authBackend || ''
  };
  if (permission?.expandedFromDatabasePermission) {
    selection.expandedFromDatabasePermission = true;
  }
  if (permission?.sourceRole) {
    selection.sourceRole = permission.sourceRole;
  }
  if (permission?.sourceGroup) {
    selection.sourceGroup = permission.sourceGroup;
  }
  return selection;
}

export function identityMode(capability) {
  return capability?.identity?.mode || (capability?.requiresLdap ? 'LDAP' : 'AUTH_BACKEND');
}

export function canCreateProviderUser(capability) {
  return identityMode(capability) === 'AUTH_BACKEND' && Boolean(capability?.identity?.canCreateUser);
}

export function canImportUsers(capability) {
  if (capability?.identity && typeof capability.identity.canImportUsers === 'boolean') {
    return capability.identity.canImportUsers;
  }
  return Boolean(capability);
}

export function importMode(capability) {
  if (capability?.identity?.importMode) return capability.identity.importMode;
  return capability?.requiresLdap ? 'LDAP' : 'AUTH_BACKEND';
}

export function shouldShowLdapGroupInput(capability) {
  if (capability?.identity && typeof capability.identity.supportsLdapGroupInput === 'boolean') {
    return capability.identity.supportsLdapGroupInput;
  }
  return Boolean(capability?.requiresLdap);
}

export function shouldShowLdapPanels(capability) {
  if (capability?.ui && typeof capability.ui.hideLdapPanels === 'boolean') {
    return !capability.ui.hideLdapPanels;
  }
  return identityMode(capability) === 'LDAP';
}

export function allowedSubjectTypes(capability) {
  const configured = capability?.rbac?.allowedSubjectTypes;
  if (Array.isArray(configured) && configured.length) {
    return configured.map(item => String(item || '').toUpperCase()).filter(Boolean);
  }
  const principalTypes = Array.isArray(capability?.principalTypes) ? capability.principalTypes : [];
  const subjects = principalTypes.map(item => String(item || '').toUpperCase()).filter(item => item === 'USER' || item === 'GROUP');
  if (subjects.length) return subjects;
  return capability?.requiresLdap ? ['GROUP'] : ['USER'];
}

export function supportsUserDirectBinding(capability) {
  return allowedSubjectTypes(capability).includes('USER');
}

export function supportsGroupBinding(capability) {
  return allowedSubjectTypes(capability).includes('GROUP');
}

export function defaultSubjectType(capability) {
  const configured = capability?.rbac?.defaultSubjectType;
  if (configured && allowedSubjectTypes(capability).includes(String(configured).toUpperCase())) {
    return String(configured).toUpperCase();
  }
  const subjects = allowedSubjectTypes(capability);
  return subjects[0] || 'USER';
}

export function supportsRoleTemplates(capability) {
  if (capability?.rbac && typeof capability.rbac.supportsRoleTemplates === 'boolean') {
    return capability.rbac.supportsRoleTemplates;
  }
  return true;
}

export function supportsDefaultRole(capability) {
  if (capability?.rbac && typeof capability.rbac.supportsDefaultRole === 'boolean') {
    return capability.rbac.supportsDefaultRole;
  }
  return true;
}

export function supportsRoles(capability) {
  if (capability?.rbac && typeof capability.rbac.supportsRoles === 'boolean') {
    return capability.rbac.supportsRoles;
  }
  return Boolean(capability);
}

export function supportsDirectGrant(capability) {
  if (capability?.grant && typeof capability.grant.supportsDirectGrant === 'boolean') {
    return capability.grant.supportsDirectGrant;
  }
  return Boolean(capability);
}

export function supportsRoleSubsetGrant(capability, subjectType) {
  if (capability?.grant && typeof capability.grant.supportsRoleSubsetGrant === 'boolean' && !capability.grant.supportsRoleSubsetGrant) {
    return false;
  }
  const subject = String(subjectType || defaultSubjectType(capability)).toUpperCase();
  if (subject === 'USER' && capability?.grant && typeof capability.grant.supportsUserRoleSubsetGrant === 'boolean') {
    return capability.grant.supportsUserRoleSubsetGrant;
  }
  if (subject === 'GROUP' && capability?.grant && typeof capability.grant.supportsGroupRoleSubsetGrant === 'boolean') {
    return capability.grant.supportsGroupRoleSubsetGrant;
  }
  return allowedSubjectTypes(capability).includes(subject);
}

export function supportsTablePermission(capability) {
  if (capability?.grant && typeof capability.grant.supportsTablePermission === 'boolean') {
    return capability.grant.supportsTablePermission;
  }
  return (capability?.resourceTypes || []).map(item => String(item).toUpperCase()).includes('TABLE');
}

export function supportsColumnPermission(capability) {
  if (capability?.grant && typeof capability.grant.supportsColumnPermission === 'boolean') {
    return capability.grant.supportsColumnPermission;
  }
  return (capability?.resourceTypes || []).map(item => String(item).toUpperCase()).includes('COLUMN');
}

export function isBackendSupportedPermission(capability, permission) {
  const supported = Array.isArray(capability?.permissions)
    ? capability.permissions.map(item => normalizePermissionName(item)).filter(Boolean)
    : [];
  if (!supported.length) return true;
  return supported.includes(normalizePermissionName(permission?.permission || permission));
}

export function userSourceLabel(capability) {
  if (capability?.ui?.userLabel) return capability.ui.userLabel;
  if (capability?.identity?.userLabel) return capability.identity.userLabel;
  const engine = String(capability?.engineType || '').toUpperCase();
  if (engine === 'STARROCKS') return 'StarRocks 用户';
  if (engine === 'DORIS') return 'Doris 用户';
  return capability?.requiresLdap ? 'LDAP 用户' : '授权后端用户';
}

export function createUserTitle(capability) {
  return capability?.ui?.createUserTitle || `创建${userSourceLabel(capability)}`;
}

export function importUsersLabel(capability) {
  return capability?.ui?.importUsersLabel || `导入${userSourceLabel(capability)}`;
}

export function capabilityStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'READY') return 'green';
  if (normalized === 'PLANNED') return 'gold';
  if (normalized === 'UNCONFIGURED') return 'red';
  return 'default';
}

export function backendOptionLabel(capability) {
  const engine = capability?.engineType || 'UNKNOWN';
  const backend = capability?.authBackend || 'UNSUPPORTED';
  const endpoint = capability?.endpointType || 'NO_ENDPOINT';
  return `${engine} / ${backend} · ${endpoint}`;
}

export function subjectTypeLabel(subjectType) {
  const normalized = String(subjectType || '').toUpperCase();
  if (normalized === 'GROUP') return '组';
  return '用户';
}

export function resourceTypeLabel(type) {
  const normalized = String(type || '').toUpperCase();
  const map = {
    GLOBAL: '全局',
    DATABASE: '库级',
    TABLE: '表级',
    COLUMN: '列级',
    VIEW: '视图',
    MATERIALIZED_VIEW: '物化视图',
    FUNCTION: '函数'
  };
  return map[normalized] || normalized || '未知';
}

export function assignmentStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SUCCESS' || normalized === 'REVOKED') return 'green';
  if (normalized === 'PENDING_GROUP_MAPPING') return 'orange';
  if (normalized === 'FAILED') return 'red';
  if (normalized === 'LOCAL_REVOKED' || normalized === 'LOCAL_ONLY') return 'blue';
  return 'default';
}

export function usableAssignmentStatus(status) {
  const normalized = String(status || 'SUCCESS').toUpperCase();
  return normalized === 'SUCCESS' || normalized === 'PENDING_GROUP_MAPPING' || normalized === 'LOCAL_ONLY';
}

export function findMatchingAssignments(roleView, subjectType, subjectName, authBackend, groupNames = []) {
  const assignments = Array.isArray(roleView?.assignments) ? roleView.assignments : [];
  const normalizedSubjectType = String(subjectType || '').toUpperCase();
  const normalizedSubjectName = String(subjectName || '').trim().toLowerCase();
  const normalizedAuthBackend = String(authBackend || '').trim().toUpperCase();
  const normalizedGroupNames = new Set((groupNames || []).map(item => String(item || '').trim().toLowerCase()).filter(Boolean));
  const directAssignments = [];
  const inheritedAssignments = [];
  if (!normalizedSubjectType || !normalizedSubjectName) {
    return { directAssignments, inheritedAssignments };
  }
  assignments.forEach(item => {
    if (!usableAssignmentStatus(item?.backendSyncStatus)) return;
    const assignmentAuthBackend = String(item?.authBackend || '').trim().toUpperCase();
    if (normalizedAuthBackend && assignmentAuthBackend && assignmentAuthBackend !== normalizedAuthBackend) return;
    const assignmentType = String(item?.subjectType || '').toUpperCase();
    const assignmentName = String(item?.subjectName || '').trim().toLowerCase();
    if (!assignmentName) return;
    if (assignmentType === normalizedSubjectType && assignmentName === normalizedSubjectName) {
      directAssignments.push(item);
      return;
    }
    if (normalizedSubjectType === 'USER' && assignmentType === 'GROUP' && normalizedGroupNames.has(assignmentName)) {
      inheritedAssignments.push(item);
    }
  });
  return { directAssignments, inheritedAssignments };
}

export function boundRoleMeta(roleView, subjectType, subjectName, authBackend, groupNames = []) {
  const { directAssignments, inheritedAssignments } = findMatchingAssignments(roleView, subjectType, subjectName, authBackend, groupNames);
  if (directAssignments.length) {
    return {
      isBound: true,
      bindingMode: 'DIRECT',
      matchedAssignments: directAssignments,
      matchedGroupName: '',
      bindingLabel: '已绑定'
    };
  }
  if (inheritedAssignments.length) {
    return {
      isBound: true,
      bindingMode: 'GROUP_INHERITED',
      matchedAssignments: inheritedAssignments,
      matchedGroupName: String(inheritedAssignments[0]?.subjectName || '').trim(),
      bindingLabel: '来自组'
    };
  }
  return {
    isBound: false,
    bindingMode: 'NONE',
    matchedAssignments: [],
    matchedGroupName: '',
    bindingLabel: ''
  };
}

export function isBoundRole(roleCode, subjectContextRoles = []) {
  const normalizedRoleCode = String(roleCode || '').trim();
  if (!normalizedRoleCode) return false;
  return (subjectContextRoles || []).some(item => String(item?.role?.roleCode || item?.roleCode || '').trim() === normalizedRoleCode);
}

export function hasUsableRoleAssignment(roleView, subjectType, subjectName, authBackend) {
  return findMatchingAssignments(roleView, subjectType, subjectName, authBackend).directAssignments.length > 0;
}

export function roleDisplayName(roleView) {
  const role = roleView?.role || {};
  return role.roleName || role.roleCode || '-';
}

function recordDatabaseName(record) {
  return record?.databaseName || record?.database || '';
}

function recordTableName(record) {
  return record?.tableName || record?.table || '';
}

function recordResourceType(record) {
  return String(record?.resourceType || (recordTableName(record) ? 'TABLE' : 'DATABASE')).toUpperCase();
}

function recordPermission(record) {
  return normalizePermissionName(record?.permission);
}

function recordAuthBackend(record, fallbackAuthBackend) {
  return String(record?.authBackend || fallbackAuthBackend || '').toUpperCase();
}

export function isGroupInheritedGrant(record) {
  const source = String(record?.source || '').toUpperCase();
  if (source === 'GROUP_ROLE') return true;
  const subjectType = String(record?.subjectType || '').toUpperCase();
  return subjectType === 'GROUP' && source.includes('GROUP');
}

export function reconciliationStatusLabel(status) {
  const map = {
    EXACT_MATCH: '完全一致',
    SUBSET_OF_ROLE: '历史权限少于角色',
    SUPERSET_OF_ROLE: '历史权限超出角色',
    PARTIAL_OVERLAP: '部分重叠',
    GROUP_INHERITED_ONLY: '仅组继承匹配',
    NO_OVERLAP: '无可接管交集',
    ALREADY_ADOPTED: '已接管'
  };
  return map[String(status || '').toUpperCase()] || status || '-';
}

export function reconciliationStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'EXACT_MATCH' || normalized === 'ALREADY_ADOPTED') return 'green';
  if (normalized === 'SUBSET_OF_ROLE' || normalized === 'SUPERSET_OF_ROLE' || normalized === 'PARTIAL_OVERLAP') return 'orange';
  if (normalized === 'GROUP_INHERITED_ONLY') return 'purple';
  if (normalized === 'NO_OVERLAP') return 'red';
  return 'default';
}

export function adoptionStatusLabel(status) {
  const map = {
    DIRECT_MATCH: '可直接接管',
    GROUP_INHERITED_MATCH: 'LDAP 组继承',
    ROLE_MISSING_LIVE: '角色权限未在后端出现',
    LIVE_EXTRA_DIRECT: '角色外历史权限',
    LIVE_EXTRA_GROUP_INHERITED: '角色外组继承',
    ALREADY_RECORDED: '已有 DGA 记录',
    RECORDED_ONLY: '仅 DGA 记录'
  };
  return map[String(status || '').toUpperCase()] || status || '-';
}

export function adoptionStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'DIRECT_MATCH') return 'green';
  if (normalized === 'GROUP_INHERITED_MATCH') return 'purple';
  if (normalized === 'ALREADY_RECORDED') return 'blue';
  if (normalized === 'ROLE_MISSING_LIVE' || normalized === 'LIVE_EXTRA_DIRECT' || normalized === 'LIVE_EXTRA_GROUP_INHERITED') return 'orange';
  if (normalized === 'RECORDED_ONLY') return 'cyan';
  return 'default';
}

export function defaultHistoricalAdoptionKeys(preview) {
  return (preview?.items || [])
    .filter(item => item?.adoptable && String(item?.adoptionStatus || '').toUpperCase() === 'DIRECT_MATCH')
    .map(item => item.key)
    .filter(Boolean);
}

export function selectedHistoricalAdoptionItems(preview, selectedKeys) {
  const selected = new Set(selectedKeys || []);
  return (preview?.items || []).filter(item => selected.has(item?.key));
}

export function historicalAdoptionNeedsGroupAcknowledgement(preview, selectedKeys) {
  return selectedHistoricalAdoptionItems(preview, selectedKeys)
    .some(item => String(item?.source || '').toUpperCase() === 'GROUP_ROLE'
      || String(item?.adoptionStatus || '').toUpperCase() === 'GROUP_INHERITED_MATCH');
}

export function historicalAdoptionNeedsRoleMissingAcknowledgement(preview) {
  return Number(preview?.summary?.roleMissingLiveCount || 0) > 0;
}

export function historicalAdoptionNeedsExtraAcknowledgement(preview) {
  return Number(preview?.summary?.liveExtraDirectCount || 0) > 0;
}

export function historicalAdoptionNeedsPartialAcknowledgement(preview) {
  return String(preview?.reconciliationStatus || '').toUpperCase() === 'PARTIAL_OVERLAP';
}

export function permissionRecordKey(record, fallbackAuthBackend) {
  return [
    recordResourceType(record),
    String(recordDatabaseName(record) || '').toLowerCase(),
    String(recordTableName(record) || '*').toLowerCase(),
    recordPermission(record),
    recordAuthBackend(record, fallbackAuthBackend)
  ].join('|');
}

export function verificationDiffRows(snapshot) {
  const live = Array.isArray(snapshot?.grants) ? snapshot.grants : [];
  const recorded = Array.isArray(snapshot?.recordedGrants) ? snapshot.recordedGrants : [];
  const fallbackAuthBackend = snapshot?.authBackend || '';
  const rows = [];
  const liveByKey = new Map(uniqueBy(live, item => permissionRecordKey(item, fallbackAuthBackend)).map(item => [permissionRecordKey(item, fallbackAuthBackend), item]));
  const recordedByKey = new Map(uniqueBy(recorded, item => permissionRecordKey(item, fallbackAuthBackend)).map(item => [permissionRecordKey(item, fallbackAuthBackend), item]));
  const keys = Array.from(new Set([...liveByKey.keys(), ...recordedByKey.keys()]));
  keys.forEach(key => {
    const liveItem = liveByKey.get(key);
    const recordedItem = recordedByKey.get(key);
    rows.push({
      key,
      status: liveItem && recordedItem ? 'MATCHED' : liveItem ? 'LIVE_ONLY' : 'RECORDED_ONLY',
      resourceType: recordResourceType(liveItem || recordedItem),
      databaseName: recordDatabaseName(liveItem || recordedItem),
      tableName: recordTableName(liveItem || recordedItem),
      permission: recordPermission(liveItem || recordedItem),
      authBackend: recordAuthBackend(liveItem || recordedItem, fallbackAuthBackend),
      source: liveItem?.source || recordedItem?.source || '',
      sourceRole: liveItem?.sourceRole || recordedItem?.sourceRole || '',
      sourceGroup: liveItem?.sourceGroup || recordedItem?.sourceGroup || '',
      grantMode: liveItem?.grantMode || recordedItem?.grantMode || '',
      roleCode: liveItem?.roleCode || recordedItem?.roleCode || '',
      subjectType: liveItem?.subjectType || recordedItem?.subjectType || '',
      subjectName: liveItem?.subjectName || recordedItem?.subjectName || '',
      groupInherited: isGroupInheritedGrant(liveItem || recordedItem),
      live: liveItem || null,
      recorded: recordedItem || null,
      grantText: liveItem?.grantText || recordedItem?.grantText || ''
    });
  });
  return rows.sort((left, right) => left.key.localeCompare(right.key));
}

export function verificationStatusColor(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'MATCHED') return 'green';
  if (normalized === 'LIVE_ONLY') return 'orange';
  if (normalized === 'RECORDED_ONLY') return 'blue';
  return 'default';
}

export function verificationSummary(snapshot) {
  const rows = verificationDiffRows(snapshot);
  return rows.reduce((summary, row) => {
    summary.total += 1;
    if (row.status === 'MATCHED') summary.matched += 1;
    if (row.status === 'LIVE_ONLY') summary.liveOnly += 1;
    if (row.status === 'RECORDED_ONLY') summary.recordedOnly += 1;
    return summary;
  }, { total: 0, matched: 0, liveOnly: 0, recordedOnly: 0 });
}

export function verificationUserRequired(subjectType) {
  return String(subjectType || '').toUpperCase() === 'GROUP';
}

export function principalOptionName(principal) {
  return String(principal?.name || principal?.value || '').trim();
}

export function principalSourceLabel(principal) {
  const normalized = String(principal?.source || '').toUpperCase();
  if (normalized === 'RANGER_USER') return 'Ranger 现有用户';
  if (normalized === 'RANGER_GROUP') return 'Ranger 现有组';
  if (normalized === 'AUTH_BACKEND') return '授权后端现有用户';
  if (normalized === 'DGA_USER') return 'DGA 平台用户';
  if (normalized === 'LDAP_GROUP') return 'LDAP 组';
  return normalized || '';
}

export function isHistoricalPrincipal(principal) {
  if (!principal) return false;
  if (typeof principal.historical === 'boolean') return principal.historical;
  if (typeof principal.directExceptionAllowed === 'boolean') return principal.directExceptionAllowed;
  return false;
}

export function principalRequiresRoleBinding(principal) {
  if (!principal) return false;
  if (typeof principal.requiresRoleBinding === 'boolean') return principal.requiresRoleBinding;
  if (typeof principal.historical === 'boolean') return !principal.historical;
  return false;
}

export function canDirectExceptionPrincipal(principal) {
  if (!principal) return true;
  if (String(principal?.subjectType || 'USER').toUpperCase() !== 'USER') return false;
  if (typeof principal.directExceptionAllowed === 'boolean') return principal.directExceptionAllowed;
  return !principalRequiresRoleBinding(principal);
}

export function principalWarnings(principal) {
  return Array.isArray(principal?.warnings) ? principal.warnings.filter(Boolean) : [];
}

export function isDatabasePermission(permission) {
  return String(permission?.resourceType || '').toUpperCase() === 'DATABASE';
}

export function buildExpandedTableSelection(parentPermission, tableName) {
  return {
    resourceType: 'TABLE',
    databaseName: parentPermission?.databaseName || '',
    tableName: tableName || null,
    permission: normalizePermissionName(parentPermission?.permission),
    authBackend: parentPermission?.authBackend || '',
    expandedFromDatabasePermission: true
  };
}

export function isExpandedTableSelection(selection) {
  return Boolean(selection?.expandedFromDatabasePermission)
    && String(selection?.resourceType || '').toUpperCase() === 'TABLE';
}

export function expandDatabasePermissionToTables(permission, tables) {
  return (tables || []).map(table => buildExpandedTableSelection(permission, table));
}

export function isReverseBindable(item) {
  return Boolean(item?.reverseBindable);
}

export function defaultReverseBindKeys(preview) {
  return (preview?.items || [])
    .filter(item => item?.reverseBindable === true)
    .map(item => item.key)
    .filter(Boolean);
}
