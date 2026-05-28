import { describe, expect, it } from 'vitest';
import {
  adoptionStatusColor,
  adoptionStatusLabel,
  allowedSubjectTypes,
  batchActionText,
  batchStatusColor,
  boundRoleMeta,
  canCreateProviderUser,
  defaultHistoricalAdoptionKeys,
  defaultSubjectType,
  findMatchingAssignments,
  hasUsableRoleAssignment,
  historicalAdoptionNeedsExtraAcknowledgement,
  historicalAdoptionNeedsGroupAcknowledgement,
  historicalAdoptionNeedsPartialAcknowledgement,
  historicalAdoptionNeedsRoleMissingAcknowledgement,
  identityMode,
  importMode,
  isBoundRole,
  parseBatchUsers,
  permissionKey,
  reconciliationStatusColor,
  reconciliationStatusLabel,
  rolePermissionSelection,
  rolePermissionShortText,
  rolePermissionText,
  shouldShowLdapGroupInput,
  shouldShowLdapPanels,
  supportsRoleSubsetGrant,
  uniqueRolePermissions,
  userSourceLabel
} from './authorizationCenterHelpers';

describe('authorizationCenterHelpers', () => {
  it('parses newline and comma separated batch users', () => {
    expect(parseBatchUsers(' alice\n bob,carol ,,  dave  ')).toEqual(['alice', 'bob', 'carol', 'dave']);
  });

  it('keeps duplicate batch users for backend dry-run classification', () => {
    expect(parseBatchUsers('alice,Alice\nalice')).toEqual(['alice', 'Alice', 'alice']);
  });

  it('maps batch statuses to tag colors', () => {
    expect(batchStatusColor('SUCCESS')).toBe('green');
    expect(batchStatusColor('FAILED')).toBe('red');
    expect(batchStatusColor('PENDING_GROUP_MAPPING')).toBe('orange');
    expect(batchStatusColor('SKIPPED')).toBe('blue');
    expect(batchStatusColor('UNKNOWN')).toBe('default');
  });

  it('maps batch actions to display text', () => {
    expect(batchActionText('WILL_BIND')).toBe('将绑定');
    expect(batchActionText('BOUND')).toBe('已处理');
    expect(batchActionText('ALREADY_BOUND')).toBe('已绑定');
    expect(batchActionText('DUPLICATE')).toBe('重复跳过');
    expect(batchActionText('INVALID')).toBe('无效');
    expect(batchActionText('SKIPPED')).toBe('已跳过');
    expect(batchActionText('CUSTOM')).toBe('CUSTOM');
    expect(batchActionText('')).toBe('-');
  });

  it('builds stable permission keys for database and table permissions', () => {
    expect(permissionKey({
      resourceType: 'database',
      databaseName: 'Finance',
      permission: 'select',
      authBackend: 'sentry'
    })).toBe('DATABASE|finance|*|SELECT|SENTRY');

    expect(permissionKey({
      resourceType: 'TABLE',
      databaseName: 'Finance',
      tableName: 'Orders',
      permission: 'insert',
      authBackend: 'STARROCKS_SQL'
    })).toBe('TABLE|finance|orders|INSERT|STARROCKS_SQL');
  });

  it('deduplicates role permissions by normalized permission key', () => {
    const roleView = {
      permissions: [
        { resourceType: 'DATABASE', databaseName: 'Finance', permission: 'SELECT', authBackend: 'SENTRY' },
        { resourceType: 'database', databaseName: 'finance', permission: 'select', authBackend: 'sentry' },
        { resourceType: 'TABLE', databaseName: 'Finance', tableName: 'Orders', permission: 'SELECT', authBackend: 'SENTRY' }
      ]
    };

    expect(uniqueRolePermissions(roleView)).toHaveLength(2);
  });

  it('formats role permission text consistently', () => {
    const databasePermission = { resourceType: 'DATABASE', databaseName: 'finance', permission: 'SELECT' };
    const tablePermission = { resourceType: 'TABLE', databaseName: 'finance', tableName: 'orders', permission: 'INSERT' };

    expect(rolePermissionText(databasePermission)).toBe('DATABASE finance.* SELECT');
    expect(rolePermissionShortText(databasePermission)).toBe('finance.* SELECT');
    expect(rolePermissionText(tablePermission)).toBe('TABLE finance.orders INSERT');
    expect(rolePermissionShortText(tablePermission)).toBe('finance.orders INSERT');
  });

  it('preserves revoke source metadata in role permission selections', () => {
    expect(rolePermissionSelection({
      resourceType: 'TABLE',
      databaseName: 'finance',
      tableName: 'orders',
      permission: 'select',
      authBackend: 'sentry',
      expandedFromDatabasePermission: true,
      sourceRole: 'role_finance_sub_abc123',
      sourceGroup: 'analytics'
    })).toEqual({
      resourceType: 'TABLE',
      databaseName: 'finance',
      tableName: 'orders',
      permission: 'SELECT',
      authBackend: 'sentry',
      expandedFromDatabasePermission: true,
      sourceRole: 'role_finance_sub_abc123',
      sourceGroup: 'analytics'
    });
  });

  it('finds direct and inherited role assignments for the current subject', () => {
    const roleView = {
      assignments: [
        { subjectType: 'USER', subjectName: 'alice', authBackend: 'SENTRY', backendSyncStatus: 'SUCCESS' },
        { subjectType: 'GROUP', subjectName: 'analytics', authBackend: 'SENTRY', backendSyncStatus: 'LOCAL_ONLY' },
        { subjectType: 'GROUP', subjectName: 'finance', authBackend: 'SENTRY', backendSyncStatus: 'FAILED' }
      ]
    };

    expect(findMatchingAssignments(roleView, 'USER', 'alice', 'SENTRY', ['analytics'])).toEqual({
      directAssignments: [roleView.assignments[0]],
      inheritedAssignments: [roleView.assignments[1]]
    });
    expect(hasUsableRoleAssignment(roleView, 'USER', 'alice', 'SENTRY')).toBe(true);
  });

  it('builds bound role metadata and bound-role flags from subject context', () => {
    const directRole = {
      role: { roleCode: 'role_direct', roleName: 'Direct Role' },
      assignments: [
        { subjectType: 'USER', subjectName: 'alice', authBackend: 'SENTRY', backendSyncStatus: 'SUCCESS' }
      ]
    };
    const inheritedRole = {
      role: { roleCode: 'role_group', roleName: 'Group Role' },
      assignments: [
        { subjectType: 'GROUP', subjectName: 'analytics', authBackend: 'SENTRY', backendSyncStatus: 'SUCCESS' }
      ]
    };

    expect(boundRoleMeta(directRole, 'USER', 'alice', 'SENTRY', ['analytics'])).toMatchObject({
      isBound: true,
      bindingMode: 'DIRECT',
      bindingLabel: '已绑定'
    });
    expect(boundRoleMeta(inheritedRole, 'USER', 'alice', 'SENTRY', ['analytics'])).toMatchObject({
      isBound: true,
      bindingMode: 'GROUP_INHERITED',
      matchedGroupName: 'analytics',
      bindingLabel: '来自组'
    });
    expect(isBoundRole('role_group', [{ ...inheritedRole, bindingMode: 'GROUP_INHERITED' }])).toBe(true);
    expect(isBoundRole('missing_role', [{ ...inheritedRole, bindingMode: 'GROUP_INHERITED' }])).toBe(false);
  });

  it('interprets LDAP/Sentry-style capabilities from explicit fields', () => {
    const capability = {
      requiresLdap: true,
      identity: {
        mode: 'LDAP',
        importMode: 'LDAP',
        supportsLdapGroupInput: true,
        userLabel: 'LDAP 用户'
      },
      rbac: {
        defaultSubjectType: 'GROUP',
        allowedSubjectTypes: ['USER', 'GROUP']
      },
      grant: {
        supportsRoleSubsetGrant: true,
        supportsUserRoleSubsetGrant: false,
        supportsGroupRoleSubsetGrant: true
      }
    };

    expect(identityMode(capability)).toBe('LDAP');
    expect(importMode(capability)).toBe('LDAP');
    expect(shouldShowLdapGroupInput(capability)).toBe(true);
    expect(shouldShowLdapPanels(capability)).toBe(true);
    expect(defaultSubjectType(capability)).toBe('GROUP');
    expect(supportsRoleSubsetGrant(capability, 'USER')).toBe(false);
    expect(supportsRoleSubsetGrant(capability, 'GROUP')).toBe(true);
    expect(userSourceLabel(capability)).toBe('LDAP 用户');
  });

  it('interprets SQL provider-managed capabilities without backend-name checks', () => {
    const capability = {
      engineType: 'SOME_SQL_ENGINE',
      identity: {
        mode: 'AUTH_BACKEND',
        importMode: 'AUTH_BACKEND',
        canCreateUser: true,
        userLabel: 'SQL 引擎用户'
      },
      ui: {
        userLabel: 'SQL 引擎用户',
        hideLdapPanels: true
      },
      rbac: {
        defaultSubjectType: 'USER',
        allowedSubjectTypes: ['USER']
      },
      grant: {
        supportsRoleSubsetGrant: true,
        supportsUserRoleSubsetGrant: true,
        supportsGroupRoleSubsetGrant: false
      }
    };

    expect(identityMode(capability)).toBe('AUTH_BACKEND');
    expect(canCreateProviderUser(capability)).toBe(true);
    expect(importMode(capability)).toBe('AUTH_BACKEND');
    expect(shouldShowLdapPanels(capability)).toBe(false);
    expect(allowedSubjectTypes(capability)).toEqual(['USER']);
    expect(defaultSubjectType(capability)).toBe('USER');
    expect(supportsRoleSubsetGrant(capability, 'USER')).toBe(true);
    expect(supportsRoleSubsetGrant(capability, 'GROUP')).toBe(false);
    expect(userSourceLabel(capability)).toBe('SQL 引擎用户');
  });

  it('keeps conservative fallback behavior for older capability payloads', () => {
    const ldapCapability = {
      requiresLdap: true,
      principalTypes: ['USER', 'GROUP'],
      resourceTypes: ['DATABASE', 'TABLE']
    };
    const sqlCapability = {
      requiresLdap: false,
      engineType: 'STARROCKS',
      principalTypes: ['USER'],
      resourceTypes: ['DATABASE', 'TABLE']
    };

    expect(identityMode(ldapCapability)).toBe('LDAP');
    expect(defaultSubjectType(ldapCapability)).toBe('USER');
    expect(supportsRoleSubsetGrant(ldapCapability, 'GROUP')).toBe(true);
    expect(identityMode(sqlCapability)).toBe('AUTH_BACKEND');
    expect(userSourceLabel(sqlCapability)).toBe('StarRocks 用户');
  });

  it('maps historical adoption statuses to display labels and colors', () => {
    expect(reconciliationStatusLabel('EXACT_MATCH')).toBe('完全一致');
    expect(reconciliationStatusColor('EXACT_MATCH')).toBe('green');
    expect(reconciliationStatusLabel('PARTIAL_OVERLAP')).toBe('部分重叠');
    expect(reconciliationStatusColor('PARTIAL_OVERLAP')).toBe('orange');
    expect(reconciliationStatusColor('GROUP_INHERITED_ONLY')).toBe('purple');
    expect(adoptionStatusLabel('DIRECT_MATCH')).toBe('可直接接管');
    expect(adoptionStatusColor('DIRECT_MATCH')).toBe('green');
    expect(adoptionStatusLabel('GROUP_INHERITED_MATCH')).toBe('LDAP 组继承');
    expect(adoptionStatusColor('GROUP_INHERITED_MATCH')).toBe('purple');
  });

  it('selects only direct adoptable historical permissions by default', () => {
    const preview = {
      items: [
        { key: 'direct', adoptable: true, adoptionStatus: 'DIRECT_MATCH' },
        { key: 'group', adoptable: true, adoptionStatus: 'GROUP_INHERITED_MATCH' },
        { key: 'missing', adoptable: false, adoptionStatus: 'ROLE_MISSING_LIVE' },
        { key: 'recorded', adoptable: true, adoptionStatus: 'ALREADY_RECORDED' }
      ]
    };

    expect(defaultHistoricalAdoptionKeys(preview)).toEqual(['direct']);
  });

  it('detects historical adoption acknowledgement requirements', () => {
    const preview = {
      reconciliationStatus: 'PARTIAL_OVERLAP',
      summary: {
        roleMissingLiveCount: 1,
        liveExtraDirectCount: 1
      },
      items: [
        { key: 'direct', source: 'USER', adoptionStatus: 'DIRECT_MATCH' },
        { key: 'group', source: 'GROUP_ROLE', adoptionStatus: 'GROUP_INHERITED_MATCH' }
      ]
    };

    expect(historicalAdoptionNeedsRoleMissingAcknowledgement(preview)).toBe(true);
    expect(historicalAdoptionNeedsExtraAcknowledgement(preview)).toBe(true);
    expect(historicalAdoptionNeedsPartialAcknowledgement(preview)).toBe(true);
    expect(historicalAdoptionNeedsGroupAcknowledgement(preview, ['direct'])).toBe(false);
    expect(historicalAdoptionNeedsGroupAcknowledgement(preview, ['group'])).toBe(true);
  });
});
