package com.dga.access.service;

import com.dga.access.entity.LdapGroupEmptyState;
import com.dga.access.repository.LdapGroupEmptyStateRepository;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class LdapService {

    private static final List<String> BASE_OBJECT_CLASSES = Arrays.asList(
            "top",
            "person",
            "organizationalPerson",
            "inetOrgPerson"
    );

    public static final int STALE_EMPTY_GROUP_DAYS = 30;

    private static final String POSIX_ACCOUNT = "posixAccount";
    private static final String SHADOW_ACCOUNT = "shadowAccount";
    private static final String LOCKED_TIME_VALUE = "000001010000Z";
    private static final String DISABLED_LOGIN_SHELL = "/sbin/nologin";

    @Autowired
    private LdapTemplate defaultLdapTemplate;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private LdapGroupEmptyStateRepository ldapGroupEmptyStateRepository;

    @Value("${spring.ldap.user-base:cn=users,cn=accounts}")
    private String defaultUserBaseDn;

    @Value("${dga.ldap.posix.uid-number-start:20000}")
    private long posixUidNumberStart;

    @Value("${dga.ldap.posix.default-gid-number:20000}")
    private long posixDefaultGidNumber;

    @Value("${dga.ldap.posix.home-directory-base:/home}")
    private String posixHomeDirectoryBase;

    @Value("${dga.ldap.posix.login-shell:/bin/bash}")
    private String posixLoginShell;

    @Value("${dga.ldap.posix.shadow-account-enabled:true}")
    private boolean shadowAccountEnabled;

    public void createUser(String username, String password, String email) {
        createUser((String) null, username, password, email, null, null);
    }

    public void createUser(String clusterIdentifier, String username, String password, String email) {
        createUser(clusterIdentifier, username, password, email, null, null);
    }

    public void createUser(String clusterIdentifier, String username, String password, String email,
                           Long gidNumber, String groupName) {
        createUser(clusterIdentifier, username, password, email, gidNumber, groupName, true);
    }

    public void createUser(String clusterIdentifier, String username, String password, String email,
                           Long gidNumber, String groupName, boolean posixAccount) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);

        if (checkUserExists(ldapTemplate, dn)) {
             if (posixAccount) {
                 repairPosixAccount(clusterIdentifier, username);
             }
             System.out.println("User " + username + " already exists in LDAP. POSIX attributes checked.");
             return;
        }

        GroupSelection groupSelection = posixAccount
                ? resolveGroupSelection(ldapTemplate, cluster, gidNumber, groupName)
                : null;
        Long selectedGidNumber = groupSelection == null ? null : groupSelection.gidNumber;
        String selectedGroupName = groupSelection == null ? null : groupSelection.groupName;
        if (posixAccount && selectedGidNumber == null) {
            throw new IllegalStateException("无法解析 OpenLDAP 用户默认组");
        }
        long targetGidNumber = selectedGidNumber == null ? 0L : selectedGidNumber;
        BasicAttributes attrs = posixAccount
                ? buildUserAttributes(ldapTemplate, cluster, username, password, email, targetGidNumber)
                : buildLdapIdentityAttributes(username, password, email);

        try {
            ldapTemplate.bind(dn, null, attrs);
            if (posixAccount) {
                addUserToGroupMemberUidIfPossible(ldapTemplate, cluster, username, selectedGroupName, targetGidNumber);
            }
            System.out.println("LDAP user created: " + username);
        } catch (Exception e) {
            if (posixAccount && isManagedEntryAddRejected(e)) {
                try {
                    ldapTemplate.bind(dn, null, buildLdapIdentityAttributes(username, password, email));
                    applyPosixAttributes(ldapTemplate, cluster, dn, username, targetGidNumber);
                    addUserToGroupMemberUidIfPossible(ldapTemplate, cluster, username, selectedGroupName, targetGidNumber);
                    System.out.println("LDAP user created with POSIX fallback: " + username);
                    return;
                } catch (Exception fallbackError) {
                    throw new RuntimeException("LDAP Error: " + fallbackError.getMessage(), fallbackError);
                }
            }
            throw new RuntimeException("LDAP Error: " + e.getMessage());
        }
    }

    public Map<String, Object> repairPosixAccount(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        Attributes attrs;
        try {
            attrs = ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Repair Error: 用户不存在或无法读取条目 - " + e.getMessage(), e);
        }

        long gidNumber = firstLongAttributeValue(attrs, "gidNumber")
                .orElseGet(() -> resolveDefaultGidNumber(ldapTemplate, cluster));
        OptionalLong uidNumber = firstLongAttributeValue(attrs, "uidNumber");
        String homeDirectory = firstAttributeValue(attrs, "homeDirectory");
        String loginShell = firstAttributeValue(attrs, "loginShell");
        boolean hasPosixAccount = attributeContainsIgnoreCase(attrs.get("objectClass"), POSIX_ACCOUNT);
        boolean hasShadowAccount = attributeContainsIgnoreCase(attrs.get("objectClass"), SHADOW_ACCOUNT);

        List<ModificationItem> modifications = new ArrayList<>();
        List<String> updatedObjectClasses = mergeObjectClasses(attrs.get("objectClass"));
        if (!hasPosixAccount) {
            updatedObjectClasses.add(POSIX_ACCOUNT);
        }
        if (shadowAccountEnabled && !hasShadowAccount) {
            updatedObjectClasses.add(SHADOW_ACCOUNT);
        }
        if (!sameIgnoreCaseValues(attrs.get("objectClass"), updatedObjectClasses)) {
            modifications.add(replaceAttribute("objectClass", updatedObjectClasses));
        }

        if (!uidNumber.isPresent()) {
            modifications.add(replaceAttribute("uidNumber",
                    String.valueOf(allocateNextUidNumber(ldapTemplate, cluster))));
        }
        if (!firstLongAttributeValue(attrs, "gidNumber").isPresent()) {
            modifications.add(replaceAttribute("gidNumber", String.valueOf(gidNumber)));
        }
        if (trimToNull(homeDirectory) == null) {
            modifications.add(replaceAttribute("homeDirectory", defaultHomeDirectory(username)));
        }
        if (trimToNull(loginShell) == null) {
            modifications.add(replaceAttribute("loginShell", posixLoginShell));
        }

        if (!modifications.isEmpty()) {
            try {
                ldapTemplate.modifyAttributes(dn, modifications.toArray(new ModificationItem[0]));
            } catch (Exception e) {
                throw new RuntimeException("LDAP Repair Error: " + e.getMessage(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("cluster", cluster != null ? cluster.getClusterName() : null);
        result.put("repaired", !modifications.isEmpty());
        result.put("uidNumber", uidNumber.isPresent() ? uidNumber.getAsLong() : lookupLongAttribute(ldapTemplate, dn, "uidNumber"));
        result.put("gidNumber", firstLongAttributeValue(attrs, "gidNumber").orElse(gidNumber));
        result.put("homeDirectory", trimToNull(homeDirectory) == null ? defaultHomeDirectory(username) : homeDirectory);
        result.put("loginShell", trimToNull(loginShell) == null ? posixLoginShell : loginShell);
        result.put("message", modifications.isEmpty()
                ? "LDAP 条目已是系统可识别的 POSIX 账号"
                : "已补齐 POSIX 账号属性");
        return result;
    }

    public List<Map<String, Object>> listPosixGroups(String clusterIdentifier) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        List<Map<String, Object>> groups = ldapTemplate.search("", "(objectClass=posixGroup)",
                (ContextMapper<Map<String, Object>>) this::mapGroupContext);
        groups.removeIf(group -> group.get("name") == null || group.get("gidNumber") == null);
        enrichPosixGroups(clusterIdentifier, cluster, ldapTemplate, groups);
        groups.sort((left, right) -> {
            long leftGid = ((Number) left.get("gidNumber")).longValue();
            long rightGid = ((Number) right.get("gidNumber")).longValue();
            int gidCompare = Long.compare(leftGid, rightGid);
            if (gidCompare != 0) return gidCompare;
            return String.valueOf(left.get("name")).compareToIgnoreCase(String.valueOf(right.get("name")));
        });
        return groups;
    }

    public Map<String, Object> getPosixGroup(String clusterIdentifier, String groupName) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        String normalized = requireGroupName(groupName);
        List<Map<String, Object>> groups = ldapTemplate.search("",
                "(&(objectClass=posixGroup)(cn=" + Rdn.escapeValue(normalized) + "))",
                (ContextMapper<Map<String, Object>>) this::mapGroupContext);
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("LDAP 用户组不存在: " + normalized);
        }
        enrichPosixGroups(clusterIdentifier, cluster, ldapTemplate, groups);
        return groups.get(0);
    }

    public Map<String, Object> createPosixGroup(String clusterIdentifier, String groupName, Long gidNumber, String description) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        String normalized = requireGroupName(groupName);
        if (findGroupByName(ldapTemplate, normalized) != null) {
            throw new IllegalArgumentException("LDAP 用户组已存在: " + normalized);
        }
        long targetGid = gidNumber != null && gidNumber > 0
                ? gidNumber : allocateNextGroupGidNumber(ldapTemplate);
        if (findGroupByGid(ldapTemplate, targetGid) != null) {
            throw new IllegalArgumentException("gidNumber 已被其他 LDAP 用户组占用: " + targetGid);
        }

        BasicAttributes attrs = new BasicAttributes();
        BasicAttribute objectClasses = new BasicAttribute("objectclass");
        objectClasses.add("top");
        objectClasses.add("posixGroup");
        attrs.put(objectClasses);
        attrs.put("cn", normalized);
        attrs.put("gidNumber", String.valueOf(targetGid));
        if (trimToNull(description) != null) {
            attrs.put("description", description.trim());
        }

        Name dn = buildGroupDn(ldapTemplate, cluster, normalized);
        ldapTemplate.bind(dn, null, attrs);
        return getPosixGroup(clusterIdentifier, normalized);
    }

    public Map<String, Object> updatePosixGroup(String clusterIdentifier, String groupName,
                                                Long gidNumber, String description, List<String> memberUids) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        String normalized = requireGroupName(groupName);
        Map<String, Object> existing = getPosixGroup(clusterIdentifier, normalized);
        Name groupDn = findGroupDn(ldapTemplate, cluster, normalized, null);
        if (groupDn == null) {
            throw new IllegalArgumentException("LDAP 用户组不存在: " + normalized);
        }

        List<ModificationItem> modifications = new ArrayList<>();
        Long currentGid = existing.get("gidNumber") instanceof Number
                ? ((Number) existing.get("gidNumber")).longValue() : null;
        if (gidNumber != null && gidNumber > 0 && !gidNumber.equals(currentGid)) {
            Map<String, Object> gidOwner = findGroupByGid(ldapTemplate, gidNumber);
            if (gidOwner != null && !normalized.equalsIgnoreCase(String.valueOf(gidOwner.get("name")))) {
                throw new IllegalArgumentException("gidNumber 已被其他 LDAP 用户组占用: " + gidNumber);
            }
            modifications.add(replaceAttribute("gidNumber", String.valueOf(gidNumber)));
        }
        if (description != null) {
            String desc = trimToNull(description);
            modifications.add(desc == null
                    ? new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("description"))
                    : replaceAttribute("description", desc));
        }
        if (memberUids != null) {
            BasicAttribute memberUid = new BasicAttribute("memberUid");
            for (String member : normalizeUidList(memberUids)) {
                memberUid.add(member);
            }
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, memberUid));
        }
        try {
            ldapTemplate.modifyAttributes(groupDn, modifications.toArray(new ModificationItem[0]));
        } catch (Exception e) {
            throw new RuntimeException("LDAP Group Update Error: " + e.getMessage(), e);
        }
        return getPosixGroup(clusterIdentifier, normalized);
    }

    public Map<String, Object> updatePosixGroupMembers(String clusterIdentifier, String groupName, List<String> memberUids) {
        return updatePosixGroup(clusterIdentifier, groupName, null, null, memberUids);
    }

    public Map<String, Object> deletePosixGroup(String clusterIdentifier, String groupName, boolean force) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        String normalized = requireGroupName(groupName);
        Map<String, Object> group = getPosixGroup(clusterIdentifier, normalized);
        long gid = ((Number) group.get("gidNumber")).longValue();
        List<String> primaryUsers = findPrimaryUsersByGid(ldapTemplate, cluster, gid);
        if (!primaryUsers.isEmpty()) {
            throw new IllegalArgumentException("该组仍是用户主组，不能删除。请先迁移用户: " + String.join(",", primaryUsers));
        }
        List<String> members = toStringList(group.get("members"));
        if (!members.isEmpty() && !force) {
            throw new IllegalArgumentException("该组仍包含 memberUid，请先清空成员或启用强制删除");
        }
        Name groupDn = findGroupDn(ldapTemplate, cluster, normalized, gid);
        if (groupDn == null) {
            throw new IllegalArgumentException("LDAP 用户组不存在: " + normalized);
        }
        Attributes groupAttrs = readEntryAttributes(ldapTemplate, groupDn, "LDAP Group Lookup Error");
        boolean managedEntry = isManagedGroupEntry(groupAttrs);
        if (managedEntry && !force) {
            throw new IllegalArgumentException("该组是 LDAP 托管条目，请先解除关联用户或启用强制删除");
        }
        if (managedEntry) {
            unlinkManagedGroup(ldapTemplate, groupDn, groupAttrs);
        }
        try {
            ldapTemplate.unbind(groupDn);
        } catch (Exception e) {
            if (isManagedEntryDeleteError(e)) {
                throw new RuntimeException("LDAP Group Delete Error: 该组仍是托管条目，请先解除关联用户后再删除", e);
            }
            throw new RuntimeException("LDAP Group Delete Error: " + e.getMessage(), e);
        }
        String clusterName = clusterNameValue(cluster, clusterIdentifier);
        if (clusterName != null) {
            ldapGroupEmptyStateRepository.deleteByClusterNameAndGroupNameIgnoreCase(clusterName, normalized);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("name", normalized);
        result.put("deleted", true);
        result.put("managedEntry", managedEntry);
        result.put("message", managedEntry ? "LDAP 用户组已解除托管关联并删除" : "LDAP 用户组已删除");
        return result;
    }

    private Attributes readEntryAttributes(LdapTemplate ldapTemplate, Name dn, String errorPrefix) {
        try {
            return ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        } catch (Exception e) {
            throw new RuntimeException(errorPrefix + ": " + e.getMessage(), e);
        }
    }

    private boolean isManagedGroupEntry(Attributes attrs) {
        return attributeContainsIgnoreCase(attrs.get("objectClass"), "mepManagedEntry")
                || !attributeValues(attrs.get("mepManagedBy")).isEmpty();
    }

    private void unlinkManagedGroup(LdapTemplate ldapTemplate, Name groupDn, Attributes groupAttrs) {
        List<String> managerDns = attributeValues(groupAttrs.get("mepManagedBy"));
        for (String managerDnValue : managerDns) {
            Name managerDn = parseLdapName(managerDnValue);
            if (managerDn == null) {
                continue;
            }
            removeAttributeReference(ldapTemplate, managerDn, "mepManagedEntry", groupDn.toString());
        }
        removeAttributeReference(ldapTemplate, groupDn, "mepManagedBy", null);
    }

    private void removeAttributeReference(LdapTemplate ldapTemplate, Name dn, String attributeName, String expectedValue) {
        Attributes attrs = readEntryAttributes(ldapTemplate, dn, "LDAP Managed Entry Lookup Error");
        Attribute attribute = attrs.get(attributeName);
        if (attribute == null || attribute.size() == 0) {
            return;
        }
        try {
            ModificationItem modification;
            List<String> values = attributeValues(attribute);
            if (expectedValue == null || values.size() <= 1) {
                modification = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attributeName));
            } else {
                modification = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attributeName, expectedValue));
            }
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{modification});
        } catch (Exception e) {
            throw new RuntimeException("LDAP Managed Entry Unlink Error: " + e.getMessage(), e);
        }
    }

    private Name parseLdapName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new LdapName(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isManagedEntryDeleteError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("managed entry") && lower.contains("unlinked first")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isAttributeNotAllowedError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("error code 65")
                        || (lower.contains("attribute") && lower.contains("not allowed"))
                        || lower.contains("object class violation")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public Map<String, Object> getUserPrimaryGroup(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        Attributes attrs;
        try {
            attrs = ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Group Lookup Error: 用户不存在或无法读取条目 - " + e.getMessage(), e);
        }
        long gidNumber = firstLongAttributeValue(attrs, "gidNumber").orElse(-1L);
        Map<String, Object> group = findGroupByGid(ldapTemplate, gidNumber);
        if (group == null) {
            group = new HashMap<>();
            group.put("gidNumber", gidNumber > 0 ? gidNumber : null);
            group.put("name", null);
        }
        group.put("username", username);
        return group;
    }

    public Map<String, Object> updateUserPrimaryGroup(String clusterIdentifier, String username, String groupName) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name userDn = buildUserDn(cluster, username);
        Map<String, Object> targetGroup = findGroupByName(ldapTemplate, groupName);
        if (targetGroup == null) {
            throw new IllegalArgumentException("所选 LDAP 用户组不存在: " + groupName);
        }
        long targetGid = ((Number) targetGroup.get("gidNumber")).longValue();

        try {
            ldapTemplate.modifyAttributes(userDn, new ModificationItem[]{
                    replaceAttribute("gidNumber", String.valueOf(targetGid))
            });
        } catch (Exception e) {
            throw new RuntimeException("LDAP Group Update Error: 更新 gidNumber 失败 - " + e.getMessage(), e);
        }

        removeUserFromAllPosixGroups(ldapTemplate, username);
        addUserToGroupMemberUid(ldapTemplate, cluster, username, String.valueOf(targetGroup.get("name")), targetGid, true);

        Map<String, Object> result = new HashMap<>(targetGroup);
        result.put("username", username);
        result.put("message", "已更新用户所属组");
        return result;
    }

    public Map<String, Object> getUserLdapProfile(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        Attributes attrs;
        try {
            attrs = ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Profile Lookup Error: 用户不存在或无法读取条目 - " + e.getMessage(), e);
        }
        return buildUserLdapProfile(ldapTemplate, username, attrs, dn);
    }

    public Map<String, Object> resetUserPassword(String clusterIdentifier, String username, String password) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        try {
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                    replaceAttribute("userPassword", password)
            });
        } catch (Exception e) {
            throw new RuntimeException("LDAP Password Reset Error: " + e.getMessage(), e);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("message", "LDAP 密码已重置");
        return result;
    }

    public Map<String, Object> updateUserLockStatus(String clusterIdentifier, String username, boolean locked) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        try {
            if (locked) {
                ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                        replaceAttribute("pwdAccountLockedTime", LOCKED_TIME_VALUE)
                });
            } else {
                ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                        new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("pwdAccountLockedTime"))
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("LDAP Lock Update Error: " + e.getMessage(), e);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("locked", locked);
        result.put("message", locked ? "LDAP 用户已锁定" : "LDAP 用户已解锁");
        return result;
    }

    public Map<String, Object> disableUserForOffboarding(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);
        try {
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                    replaceAttribute("pwdAccountLockedTime", LOCKED_TIME_VALUE)
            });
            Map<String, Object> result = new HashMap<>();
            result.put("username", username);
            result.put("locked", true);
            result.put("method", "pwdAccountLockedTime");
            result.put("message", "LDAP 账户已通过 pwdAccountLockedTime 锁定");
            return result;
        } catch (Exception lockError) {
            if (!isAttributeNotAllowedError(lockError)) {
                throw new RuntimeException("LDAP Lock Update Error: " + lockError.getMessage(), lockError);
            }
            try {
                ldapTemplate.modifyAttributes(dn, new ModificationItem[]{
                        replaceAttribute("loginShell", DISABLED_LOGIN_SHELL)
                });
                Map<String, Object> result = new HashMap<>();
                result.put("username", username);
                result.put("locked", true);
                result.put("method", "loginShell");
                result.put("message", "LDAP schema 不允许 pwdAccountLockedTime，已改用 loginShell=" + DISABLED_LOGIN_SHELL + " 禁止登录");
                return result;
            } catch (Exception shellError) {
                throw new RuntimeException("LDAP Offboarding Disable Error: pwdAccountLockedTime 不可用，loginShell 兜底也失败 - "
                        + shellError.getMessage(), shellError);
            }
        }
    }

    public List<Map<String, Object>> getUserSupplementaryGroups(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Attributes attrs = readUserAttributes(ldapTemplate, buildUserDn(cluster, username));
        long primaryGid = firstLongAttributeValue(attrs, "gidNumber").orElse(-1L);
        return findUserMemberGroups(ldapTemplate, username, primaryGid);
    }

    public Map<String, Object> updateUserSupplementaryGroups(String clusterIdentifier, String username, List<String> groupNames) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Attributes attrs = readUserAttributes(ldapTemplate, buildUserDn(cluster, username));
        long primaryGid = firstLongAttributeValue(attrs, "gidNumber").orElse(-1L);

        removeUserFromAllSupplementaryGroups(ldapTemplate, cluster, username, primaryGid);
        List<Map<String, Object>> selectedGroups = new ArrayList<>();
        if (groupNames != null) {
            for (String groupName : groupNames) {
                if (trimToNull(groupName) == null) {
                    continue;
                }
                Map<String, Object> group = findGroupByName(ldapTemplate, groupName.trim());
                if (group == null) {
                    throw new IllegalArgumentException("所选附加组不存在: " + groupName);
                }
                Long gid = group.get("gidNumber") instanceof Number
                        ? ((Number) group.get("gidNumber")).longValue() : null;
                if (gid != null && gid == primaryGid) {
                    continue;
                }
                addUserToGroupMemberUid(ldapTemplate, cluster, username, String.valueOf(group.get("name")), gid, true);
                selectedGroups.add(group);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("supplementaryGroups", selectedGroups);
        result.put("message", "已更新用户附加组");
        return result;
    }

    public void deleteUser(String username) {
        deleteUser(null, username);
    }

    public void deleteUser(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        Name dn = buildUserDn(cluster, username);
        try {
            getLdapTemplate(cluster).unbind(dn);
            System.out.println("LDAP user deleted: " + username);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Delete Error: " + e.getMessage());
        }
    }

    public boolean checkUserExists(Name dn) {
        return checkUserExists(defaultLdapTemplate, dn);
    }

    public boolean checkUserExists(LdapTemplate ldapTemplate, Name dn) {
        try {
            ldapTemplate.lookup(dn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean userExists(String username) {
        return userExists(null, username);
    }

    public boolean userExists(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        return checkUserExists(getLdapTemplate(cluster), buildUserDn(cluster, username));
    }

    public Name buildUserDn(String username) {
        return buildUserDn(null, username);
    }

    public Name buildUserDn(Cluster cluster, String username) {
        String userBaseDn = resolveUserBaseDn(cluster);
        try {
            javax.naming.ldap.LdapName builder = new javax.naming.ldap.LdapName(userBaseDn);
            builder.add(new javax.naming.ldap.Rdn("uid", username));
            return builder;
        } catch (Exception e) {
            return LdapNameBuilder.newInstance()
                    .add("cn", "accounts")
                    .add("cn", "users")
                    .add("uid", username)
                    .build();
        }
    }

    public String getUserDnString(String username) {
        return buildUserDn(username).toString();
    }

    public String getUserDnString(String clusterIdentifier, String username) {
        return buildUserDn(resolveCluster(clusterIdentifier), username).toString();
    }

    public Map<String, Object> getUserInfo(String username) {
        return getUserInfo(null, username);
    }

    public Map<String, Object> getUserInfo(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        Name dn = buildUserDn(cluster, username);
        try {
            Attributes attrs = getLdapTemplate(cluster).lookup(dn, (AttributesMapper<Attributes>) a -> a);
            Map<String, Object> map = new HashMap<>();
            if (attrs.get("uid") != null) map.put("uid", attrs.get("uid").get());
            if (attrs.get("cn") != null) map.put("cn", attrs.get("cn").get());
            if (attrs.get("sn") != null) map.put("sn", attrs.get("sn").get());
            if (attrs.get("mail") != null) map.put("mail", attrs.get("mail").get());
            if (attrs.get("modifyTimestamp") != null) map.put("modifyTimestamp", attrs.get("modifyTimestamp").get());
            if (attrs.get("createTimestamp") != null) map.put("createTimestamp", attrs.get("createTimestamp").get());
            if (attrs.get("pwdChangedTime") != null) map.put("pwdChangedTime", attrs.get("pwdChangedTime").get());
            if (attrs.get("krbLastSuccessfulAuth") != null) map.put("krbLastSuccessfulAuth", attrs.get("krbLastSuccessfulAuth").get());
            if (attrs.get("authTimestamp") != null) map.put("authTimestamp", attrs.get("authTimestamp").get());
            if (attrs.get("lastLoginTime") != null) map.put("lastLoginTime", attrs.get("lastLoginTime").get());
            if (attrs.get("lastLogin") != null) map.put("lastLogin", attrs.get("lastLogin").get());
            if (attrs.get("lastLogonTimestamp") != null) map.put("lastLogonTimestamp", attrs.get("lastLogonTimestamp").get());
            if (attrs.get("lastLogon") != null) map.put("lastLogon", attrs.get("lastLogon").get());
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> listUsers(String clusterIdentifier) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        if (cluster == null) {
            throw new IllegalArgumentException("请选择有效集群后再导入 LDAP 用户");
        }
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        if (endpoint == null || endpoint.getUrl() == null || endpoint.getUrl().isEmpty()) {
            throw new IllegalStateException("集群 " + cluster.getClusterName() + " 未配置 ACTIVE LDAP 端点");
        }

        String userBaseDn = resolveUserBaseDn(cluster);
        return getLdapTemplate(cluster).search(userBaseDn, "(uid=*)", (AttributesMapper<Map<String, Object>>) attrs -> {
            Map<String, Object> user = new HashMap<>();
            user.put("uid", firstAttributeValue(attrs, "uid"));
            user.put("cn", firstAttributeValue(attrs, "cn"));
            user.put("sn", firstAttributeValue(attrs, "sn"));
            user.put("mail", firstAttributeValue(attrs, "mail"));
            user.put("givenName", firstAttributeValue(attrs, "givenName"));
            user.put("modifyTimestamp", firstAttributeValue(attrs, "modifyTimestamp"));
            user.put("createTimestamp", firstAttributeValue(attrs, "createTimestamp"));
            user.put("pwdChangedTime", firstAttributeValue(attrs, "pwdChangedTime"));
            user.put("krbLastSuccessfulAuth", firstAttributeValue(attrs, "krbLastSuccessfulAuth"));
            user.put("authTimestamp", firstAttributeValue(attrs, "authTimestamp"));
            user.put("lastLoginTime", firstAttributeValue(attrs, "lastLoginTime"));
            user.put("lastLogin", firstAttributeValue(attrs, "lastLogin"));
            user.put("lastLogonTimestamp", firstAttributeValue(attrs, "lastLogonTimestamp"));
            user.put("lastLogon", firstAttributeValue(attrs, "lastLogon"));
            return user;
        });
    }

    public Map<String, Object> describeUserSearch(String clusterIdentifier) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        Map<String, Object> info = new HashMap<>();
        info.put("filter", "(uid=*)");
        if (cluster == null) {
            return info;
        }
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        String baseDn = endpoint == null ? null : trimToNull(endpoint.getBaseDn());
        String searchBase = resolveUserBaseDn(cluster);
        info.put("baseDn", baseDn);
        info.put("searchBase", searchBase);
        info.put("searchBaseDn", absoluteSearchBase(searchBase, baseDn));
        return info;
    }

    private LdapTemplate getLdapTemplate(Cluster cluster) {
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        if (endpoint == null || endpoint.getUrl() == null || endpoint.getUrl().isEmpty()) {
            return defaultLdapTemplate;
        }
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(endpoint.getUrl());
        contextSource.setBase(endpoint.getBaseDn());
        Exception lastError = null;
        List<String> candidates = ldapBindDnCandidates(endpoint);
        for (String userDn : candidates) {
            try {
                contextSource = new LdapContextSource();
                contextSource.setUrl(endpoint.getUrl());
                if (endpoint.getBaseDn() != null && !endpoint.getBaseDn().trim().isEmpty()) {
                    contextSource.setBase(endpoint.getBaseDn().trim());
                }
                if (userDn != null && !userDn.isEmpty()) {
                    contextSource.setUserDn(userDn);
                }
                contextSource.setPassword(endpoint.getPassword());
                contextSource.afterPropertiesSet();
                LdapTemplate template = new LdapTemplate(contextSource);
                template.lookup("");
                return template;
            } catch (Exception e) {
                lastError = e;
            }
        }
        String message = lastError == null ? "LDAP 认证失败" : lastError.getMessage();
        throw new IllegalStateException("LDAP 认证失败，请检查账号是否为完整 Bind DN 或密码是否正确。尝试账号: "
                + String.join(" / ", candidates) + "；错误: " + message, lastError);
    }

    private Cluster resolveCluster(String clusterIdentifier) {
        if (clusterIdentifier == null || clusterIdentifier.isEmpty()) {
            return null;
        }
        Cluster cluster = clusterRepository.findByClusterCode(clusterIdentifier);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(clusterIdentifier);
        }
        return cluster;
    }

    private ClusterEndpoint getLdapEndpoint(Cluster cluster) {
        if (cluster == null || cluster.getClusterCode() == null) {
            return null;
        }
        java.util.List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                cluster.getClusterCode(), ClusterEndpoint.TYPE_LDAP, "ACTIVE");
        return endpoints.isEmpty() ? null : endpoints.get(0);
    }

    private BasicAttributes buildUserAttributes(LdapTemplate ldapTemplate, Cluster cluster, String username,
                                                String password, String email, long gidNumber) {
        BasicAttributes attrs = buildLdapIdentityAttributes(username, password, email);
        BasicAttribute objectClasses = (BasicAttribute) attrs.get("objectclass");
        objectClasses.add(POSIX_ACCOUNT);
        if (shadowAccountEnabled) {
            objectClasses.add(SHADOW_ACCOUNT);
        }
        attrs.put("uidNumber", String.valueOf(allocateNextUidNumber(ldapTemplate, cluster)));
        attrs.put("gidNumber", String.valueOf(gidNumber));
        attrs.put("homeDirectory", defaultHomeDirectory(username));
        attrs.put("loginShell", posixLoginShell);
        return attrs;
    }

    private BasicAttributes buildLdapIdentityAttributes(String username, String password, String email) {
        BasicAttributes attrs = new BasicAttributes();
        BasicAttribute objectClasses = new BasicAttribute("objectclass");
        for (String objectClass : BASE_OBJECT_CLASSES) {
            objectClasses.add(objectClass);
        }
        attrs.put(objectClasses);
        attrs.put("uid", username);
        attrs.put("cn", username);
        attrs.put("sn", username);
        attrs.put("userPassword", password);
        if (email != null && !email.isEmpty()) {
            attrs.put("mail", email);
        }
        return attrs;
    }

    private void applyPosixAttributes(LdapTemplate ldapTemplate, Cluster cluster, Name dn, String username, long gidNumber) {
        Attributes existing = ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        List<ModificationItem> modifications = new ArrayList<>();
        List<String> objectClasses = mergeObjectClasses(existing.get("objectClass"));
        if (!attributeContainsIgnoreCase(existing.get("objectClass"), POSIX_ACCOUNT)) {
            objectClasses.add(POSIX_ACCOUNT);
        }
        if (shadowAccountEnabled && !attributeContainsIgnoreCase(existing.get("objectClass"), SHADOW_ACCOUNT)) {
            objectClasses.add(SHADOW_ACCOUNT);
        }
        if (!sameIgnoreCaseValues(existing.get("objectClass"), objectClasses)) {
            modifications.add(replaceAttribute("objectClass", objectClasses));
        }
        if (!firstLongAttributeValue(existing, "uidNumber").isPresent()) {
            modifications.add(replaceAttribute("uidNumber", String.valueOf(allocateNextUidNumber(ldapTemplate, cluster))));
        }
        modifications.add(replaceAttribute("gidNumber", String.valueOf(gidNumber)));
        if (trimToNull(firstAttributeValue(existing, "homeDirectory")) == null) {
            modifications.add(replaceAttribute("homeDirectory", defaultHomeDirectory(username)));
        }
        if (trimToNull(firstAttributeValue(existing, "loginShell")) == null) {
            modifications.add(replaceAttribute("loginShell", posixLoginShell));
        }
        if (!modifications.isEmpty()) {
            ldapTemplate.modifyAttributes(dn, modifications.toArray(new ModificationItem[0]));
        }
    }

    private boolean isManagedEntryAddRejected(Exception e) {
        String message = e == null || e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("managed entry plugin rejected add operation")
                || message.contains("operationnotsupportedexception")
                || message.contains("error code 53");
    }

    private synchronized long allocateNextUidNumber(LdapTemplate ldapTemplate, Cluster cluster) {
        List<Long> existing = ldapTemplate.search(resolveUserBaseDn(cluster), "(uidNumber=*)",
                (AttributesMapper<Long>) attrs -> firstLongAttributeValue(attrs, "uidNumber").orElse(-1L));
        long max = posixUidNumberStart - 1;
        for (Long value : existing) {
            if (value != null && value > max) {
                max = value;
            }
        }
        return max + 1;
    }

    private synchronized long allocateNextGroupGidNumber(LdapTemplate ldapTemplate) {
        List<Long> existing = ldapTemplate.search("", "(gidNumber=*)",
                (AttributesMapper<Long>) attrs -> firstLongAttributeValue(attrs, "gidNumber").orElse(-1L));
        long max = posixDefaultGidNumber - 1;
        for (Long value : existing) {
            if (value != null && value > max) {
                max = value;
            }
        }
        return max + 1;
    }

    private long resolveDefaultGidNumber(LdapTemplate ldapTemplate, Cluster cluster) {
        List<Long> existing = ldapTemplate.search(resolveUserBaseDn(cluster), "(gidNumber=*)",
                (AttributesMapper<Long>) attrs -> firstLongAttributeValue(attrs, "gidNumber").orElse(-1L));
        for (Long value : existing) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return posixDefaultGidNumber;
    }

    private GroupSelection resolveGroupSelection(LdapTemplate ldapTemplate, Cluster cluster, Long gidNumber, String groupName) {
        String normalizedGroupName = trimToNull(groupName);
        if (normalizedGroupName != null) {
            List<Map<String, Object>> groups = listPosixGroups(cluster != null ? cluster.getClusterCode() : null);
            for (Map<String, Object> group : groups) {
                if (normalizedGroupName.equalsIgnoreCase(String.valueOf(group.get("name")))) {
                    return new GroupSelection(((Number) group.get("gidNumber")).longValue(), String.valueOf(group.get("name")));
                }
            }
            throw new IllegalArgumentException("所选 LDAP 用户组不存在: " + normalizedGroupName);
        }
        if (gidNumber != null && gidNumber > 0) {
            return new GroupSelection(gidNumber, null);
        }
        return new GroupSelection(resolveDefaultGidNumber(ldapTemplate, cluster), null);
    }

    private void addUserToGroupMemberUidIfPossible(LdapTemplate ldapTemplate, Cluster cluster, String username,
                                                   String groupName, Long gidNumber) {
        addUserToGroupMemberUid(ldapTemplate, cluster, username, groupName, gidNumber, false);
    }

    private void addUserToGroupMemberUid(LdapTemplate ldapTemplate, Cluster cluster, String username,
                                         String groupName, Long gidNumber, boolean strict) {
        List<Name> groupDns = findGroupDns(ldapTemplate, cluster, groupName, gidNumber);
        if (groupDns.isEmpty()) {
            if (strict) {
                String target = trimToNull(groupName) != null ? groupName.trim() : String.valueOf(gidNumber);
                throw new IllegalArgumentException("LDAP 用户组不存在: " + target);
            }
            return;
        }
        Exception lastError = null;
        for (Name groupDn : groupDns) {
            try {
                Attributes attributes = ldapTemplate.lookup(groupDn, (AttributesMapper<Attributes>) attrs -> attrs);
                Attribute memberUid = attributes.get("memberUid");
                if (!attributeContainsIgnoreCase(memberUid, username)) {
                    ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{
                            new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("memberUid", username))
                    });
                }
                return;
            } catch (Exception e) {
                lastError = e;
                System.err.println("Failed to append memberUid for group " + groupDn + ": " + e.getMessage());
            }
        }
        if (strict && lastError != null) {
            throw new RuntimeException("LDAP Group Update Error: 写入 memberUid 失败 - " + lastError.getMessage(), lastError);
        }
    }

    private void removeUserFromAllPosixGroups(LdapTemplate ldapTemplate, String username) {
        List<Name> groups = ldapTemplate.search("", "(&(objectClass=posixGroup)(memberUid=" + Rdn.escapeValue(username) + "))",
                (ContextMapper<Name>) ctx -> ((DirContextOperations) ctx).getDn());
        groups.sort(Comparator.comparingInt(this::groupDnPriority));
        for (Name groupDn : groups) {
            try {
                ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{
                        new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("memberUid", username))
                });
            } catch (Exception e) {
                System.err.println("Failed to remove memberUid from group " + groupDn + ": " + e.getMessage());
            }
        }
    }

    private void removeUserFromAllSupplementaryGroups(LdapTemplate ldapTemplate, Cluster cluster, String username, long primaryGid) {
        List<Map<String, Object>> groups = findUserMemberGroups(ldapTemplate, username, -1L);
        for (Map<String, Object> group : groups) {
            Long gid = group.get("gidNumber") instanceof Number ? ((Number) group.get("gidNumber")).longValue() : null;
            if (gid != null && gid == primaryGid) {
                continue;
            }
            for (Name groupDn : findGroupDns(ldapTemplate, cluster, (String) group.get("name"), gid)) {
                try {
                    ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{
                            new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("memberUid", username))
                    });
                    break;
                } catch (Exception e) {
                    System.err.println("Failed to remove supplementary group memberUid from " + groupDn + ": " + e.getMessage());
                }
            }
        }
    }

    private Name findGroupDn(LdapTemplate ldapTemplate, Cluster cluster, String groupName, Long gidNumber) {
        List<Name> matches = findGroupDns(ldapTemplate, cluster, groupName, gidNumber);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private List<Name> findGroupDns(LdapTemplate ldapTemplate, Cluster cluster, String groupName, Long gidNumber) {
        String filter;
        if (trimToNull(groupName) != null) {
            filter = "(&(objectClass=posixGroup)(cn=" + Rdn.escapeValue(groupName) + "))";
        } else if (gidNumber != null && gidNumber > 0) {
            filter = "(&(objectClass=posixGroup)(gidNumber=" + gidNumber + "))";
        } else {
            return Collections.emptyList();
        }
        List<Name> matches = ldapTemplate.search("", filter,
                (ContextMapper<Name>) ctx -> ((DirContextOperations) ctx).getDn());
        matches.sort(Comparator.comparingInt(this::groupDnPriority));
        return matches;
    }

    private int groupDnPriority(Name dn) {
        String value = dn == null ? "" : dn.toString().toLowerCase(Locale.ROOT);
        if (value.contains("cn=compat")) {
            return 10;
        }
        if (value.contains("cn=groups,cn=accounts")) {
            return 0;
        }
        return 5;
    }

    private Name buildGroupDn(LdapTemplate ldapTemplate, Cluster cluster, String groupName) {
        String parent = resolveGroupParentDn(ldapTemplate, cluster);
        return LdapNameBuilder.newInstance(parent).add("cn", groupName).build();
    }

    private String resolveGroupParentDn(LdapTemplate ldapTemplate, Cluster cluster) {
        for (String candidate : groupParentCandidates(cluster)) {
            if (ldapEntryExists(ldapTemplate, candidate)) {
                return candidate;
            }
        }
        List<Name> existingGroups = ldapTemplate.search("", "(objectClass=posixGroup)",
                (ContextMapper<Name>) ctx -> ((DirContextOperations) ctx).getDn());
        if (existingGroups.isEmpty()) {
            return "";
        }
        try {
            LdapName name = new LdapName(existingGroups.get(0).toString());
            if (name.size() > 0) {
                name.remove(name.size() - 1);
            }
            return name.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> groupParentCandidates(Cluster cluster) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String userBase = trimToNull(resolveUserBaseDn(cluster));
        if (userBase != null) {
            try {
                LdapName userBaseName = new LdapName(userBase);
                if (userBaseName.size() > 0) {
                    userBaseName.remove(userBaseName.size() - 1);
                    userBaseName.add(new Rdn("cn", "groups"));
                    candidates.add(userBaseName.toString());
                }
            } catch (Exception ignored) {
                String lower = userBase.toLowerCase(Locale.ROOT);
                if (lower.startsWith("cn=users,")) {
                    candidates.add("cn=groups," + userBase.substring("cn=users,".length()));
                }
            }
        }
        candidates.add("cn=groups,cn=accounts");
        candidates.add("cn=groups");
        return new ArrayList<>(candidates);
    }

    private boolean ldapEntryExists(LdapTemplate ldapTemplate, String dn) {
        String value = trimToNull(dn);
        if (value == null) {
            return false;
        }
        try {
            ldapTemplate.lookup(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Map<String, Object> findGroupByName(LdapTemplate ldapTemplate, String groupName) {
        if (trimToNull(groupName) == null) {
            return null;
        }
        List<Map<String, Object>> groups = ldapTemplate.search("",
                "(&(objectClass=posixGroup)(cn=" + Rdn.escapeValue(groupName) + "))",
                (AttributesMapper<Map<String, Object>>) attrs -> {
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", firstAttributeValue(attrs, "cn"));
                    group.put("gidNumber", firstLongAttributeValue(attrs, "gidNumber").isPresent()
                            ? firstLongAttributeValue(attrs, "gidNumber").getAsLong() : null);
                    return group;
                });
        return groups.isEmpty() ? null : groups.get(0);
    }

    private Map<String, Object> findGroupByGid(LdapTemplate ldapTemplate, long gidNumber) {
        if (gidNumber <= 0) {
            return null;
        }
        List<Map<String, Object>> groups = ldapTemplate.search("",
                "(&(objectClass=posixGroup)(gidNumber=" + gidNumber + "))",
                (AttributesMapper<Map<String, Object>>) attrs -> {
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", firstAttributeValue(attrs, "cn"));
                    group.put("gidNumber", firstLongAttributeValue(attrs, "gidNumber").isPresent()
                            ? firstLongAttributeValue(attrs, "gidNumber").getAsLong() : null);
                    return group;
                });
        return groups.isEmpty() ? null : groups.get(0);
    }

    private List<Map<String, Object>> findUserMemberGroups(LdapTemplate ldapTemplate, String username, long excludedGid) {
        List<Map<String, Object>> groups = ldapTemplate.search("",
                "(&(objectClass=posixGroup)(memberUid=" + Rdn.escapeValue(username) + "))",
                (AttributesMapper<Map<String, Object>>) attrs -> {
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", firstAttributeValue(attrs, "cn"));
                    group.put("gidNumber", firstLongAttributeValue(attrs, "gidNumber").isPresent()
                            ? firstLongAttributeValue(attrs, "gidNumber").getAsLong() : null);
                    return group;
                });
        groups.removeIf(group -> group.get("name") == null || group.get("gidNumber") == null
                || (((Number) group.get("gidNumber")).longValue() == excludedGid));
        groups.sort((left, right) -> String.valueOf(left.get("name")).compareToIgnoreCase(String.valueOf(right.get("name"))));
        return groups;
    }

    private List<String> findPrimaryUsersByGid(LdapTemplate ldapTemplate, Cluster cluster, long gidNumber) {
        return ldapTemplate.search(resolveUserBaseDn(cluster), "(&(uid=*)(gidNumber=" + gidNumber + "))",
                (AttributesMapper<String>) attrs -> firstAttributeValue(attrs, "uid"))
                .stream()
                .filter(value -> trimToNull(value) != null)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapGroupContext(Object context) {
        DirContextOperations ctx = (DirContextOperations) context;
        Attributes attrs = ctx.getAttributes();
        Map<String, Object> group = new HashMap<>();
        group.put("dn", ctx.getDn().toString());
        group.put("name", firstAttributeValue(attrs, "cn"));
        group.put("gidNumber", firstLongAttributeValue(attrs, "gidNumber").isPresent()
                ? firstLongAttributeValue(attrs, "gidNumber").getAsLong()
                : null);
        group.put("description", firstAttributeValue(attrs, "description"));
        group.put("members", attributeValues(attrs.get("memberUid")));
        group.put("memberCount", attributeValues(attrs.get("memberUid")).size());
        return group;
    }

    private void enrichPosixGroups(String clusterIdentifier, Cluster cluster, LdapTemplate ldapTemplate,
                                   List<Map<String, Object>> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        String clusterName = clusterNameValue(cluster, clusterIdentifier);
        Map<String, LdapGroupEmptyState> stateMap = loadEmptyStateMap(clusterName);
        for (Map<String, Object> group : groups) {
            GroupBindingSnapshot snapshot = buildGroupBindingSnapshot(ldapTemplate, cluster, group);
            String stateKey = groupStateKey(stringValue(group.get("name")));
            LdapGroupEmptyState state = refreshGroupEmptyState(cluster, clusterIdentifier, group, snapshot,
                    stateMap.get(stateKey));
            if (state != null) {
                stateMap.put(stateKey, state);
            }
            applyGroupDerivedFields(group, snapshot, state);
        }
    }

    private Map<String, LdapGroupEmptyState> loadEmptyStateMap(String clusterName) {
        Map<String, LdapGroupEmptyState> stateMap = new HashMap<>();
        if (clusterName == null) {
            return stateMap;
        }
        for (LdapGroupEmptyState state : ldapGroupEmptyStateRepository.findByClusterName(clusterName)) {
            stateMap.put(groupStateKey(state.getGroupName()), state);
        }
        return stateMap;
    }

    private GroupBindingSnapshot buildGroupBindingSnapshot(LdapTemplate ldapTemplate, Cluster cluster,
                                                           Map<String, Object> group) {
        List<String> directMembers = normalizeUidList(toStringList(group.get("members")));
        Long gidNumber = longValue(group.get("gidNumber"));
        List<String> primaryUsers = gidNumber == null
                ? new ArrayList<>()
                : findPrimaryUsersByGid(ldapTemplate, cluster, gidNumber);
        LinkedHashSet<String> boundUsers = new LinkedHashSet<>();
        boundUsers.addAll(directMembers);
        boundUsers.addAll(primaryUsers);

        GroupBindingSnapshot snapshot = new GroupBindingSnapshot();
        snapshot.directMembers = directMembers;
        snapshot.primaryUsers = primaryUsers;
        snapshot.boundUsers = new ArrayList<>(boundUsers);
        snapshot.boundUsers.sort(String::compareToIgnoreCase);
        snapshot.boundUserCount = snapshot.boundUsers.size();
        return snapshot;
    }

    private LdapGroupEmptyState refreshGroupEmptyState(Cluster cluster, String clusterIdentifier,
                                                       Map<String, Object> group, GroupBindingSnapshot snapshot,
                                                       LdapGroupEmptyState existing) {
        String clusterName = clusterNameValue(cluster, clusterIdentifier);
        String groupName = stringValue(group.get("name"));
        if (clusterName == null || groupName == null) {
            return null;
        }
        LdapGroupEmptyState state = existing == null ? new LdapGroupEmptyState() : existing;
        state.setClusterCode(clusterCodeValue(cluster, clusterIdentifier));
        state.setClusterName(clusterName);
        state.setGroupName(groupName);
        state.setGidNumber(longValue(group.get("gidNumber")));
        LocalDateTime now = LocalDateTime.now();
        if (snapshot.boundUserCount > 0) {
            state.setEmptySince(null);
            state.setLastSeenEmptyAt(null);
            state.setLastSeenNonEmptyAt(now);
        } else {
            if (state.getEmptySince() == null) {
                state.setEmptySince(now);
            }
            state.setLastSeenEmptyAt(now);
        }
        return ldapGroupEmptyStateRepository.save(state);
    }

    private void applyGroupDerivedFields(Map<String, Object> group, GroupBindingSnapshot snapshot,
                                         LdapGroupEmptyState state) {
        LocalDateTime emptySince = state == null ? null : state.getEmptySince();
        long staleEmptyDays = emptySince == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(emptySince, LocalDateTime.now()));
        boolean empty = snapshot.boundUserCount <= 0;
        group.put("directMemberCount", snapshot.directMembers.size());
        group.put("primaryUsers", snapshot.primaryUsers);
        group.put("primaryUserCount", snapshot.primaryUsers.size());
        group.put("boundUsers", snapshot.boundUsers);
        group.put("boundUserCount", snapshot.boundUserCount);
        group.put("empty", empty);
        group.put("emptySince", empty ? emptySince : null);
        group.put("staleEmptyDays", empty ? staleEmptyDays : 0);
        group.put("staleEmpty", empty && emptySince != null && staleEmptyDays >= STALE_EMPTY_GROUP_DAYS);
    }

    private String clusterNameValue(Cluster cluster, String clusterIdentifier) {
        if (cluster != null && trimToNull(cluster.getClusterName()) != null) {
            return cluster.getClusterName().trim();
        }
        return trimToNull(clusterIdentifier);
    }

    private String clusterCodeValue(Cluster cluster, String clusterIdentifier) {
        if (cluster != null && trimToNull(cluster.getClusterCode()) != null) {
            return cluster.getClusterCode().trim();
        }
        return trimToNull(clusterIdentifier);
    }

    private String groupStateKey(String groupName) {
        return groupName == null ? "" : groupName.toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<String> items = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String text = stringValue(item);
            if (text != null) {
                items.add(text);
            }
        }
        return items;
    }

    private String requireGroupName(String groupName) {
        String normalized = trimToNull(groupName);
        if (normalized == null) {
            throw new IllegalArgumentException("LDAP 用户组名称不能为空");
        }
        if (!normalized.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalArgumentException("LDAP 用户组名称只能包含字母、数字、点、下划线和中划线");
        }
        return normalized;
    }

    private List<String> normalizeUidList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(value -> value != null && value.matches("[A-Za-z0-9._-]{1,128}"))
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    private List<String> attributeValues(Attribute attribute) {
        if (attribute == null) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        try {
            for (int i = 0; i < attribute.size(); i++) {
                Object value = attribute.get(i);
                if (value != null && trimToNull(String.valueOf(value)) != null) {
                    values.add(String.valueOf(value).trim());
                }
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        values.sort(String::compareToIgnoreCase);
        return values;
    }

    private Attributes readUserAttributes(LdapTemplate ldapTemplate, Name dn) {
        try {
            return ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) attributes -> attributes);
        } catch (Exception e) {
            throw new RuntimeException("LDAP User Lookup Error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildUserLdapProfile(LdapTemplate ldapTemplate, String username, Attributes attrs, Name dn) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", username);
        profile.put("dn", dn.toString());
        long primaryGid = firstLongAttributeValue(attrs, "gidNumber").orElse(-1L);
        profile.put("primaryGroup", findGroupByGid(ldapTemplate, primaryGid));
        profile.put("supplementaryGroups", findUserMemberGroups(ldapTemplate, username, primaryGid));
        profile.put("locked", isLocked(attrs));
        profile.put("attributes", flattenAttributes(attrs));
        return profile;
    }

    private boolean isLocked(Attributes attrs) {
        String pwdLocked = firstAttributeValue(attrs, "pwdAccountLockedTime");
        if (trimToNull(pwdLocked) != null) {
            return true;
        }
        String nsAccountLock = firstAttributeValue(attrs, "nsAccountLock");
        if ("true".equalsIgnoreCase(nsAccountLock)) {
            return true;
        }
        String accountStatus = firstAttributeValue(attrs, "accountStatus");
        return "inactive".equalsIgnoreCase(accountStatus) || "disabled".equalsIgnoreCase(accountStatus);
    }

    private Map<String, Object> flattenAttributes(Attributes attrs) {
        Map<String, Object> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            NamingEnumeration<? extends Attribute> all = attrs.getAll();
            while (all.hasMore()) {
                Attribute attribute = all.next();
                List<String> values = new ArrayList<>();
                for (int i = 0; i < attribute.size(); i++) {
                    Object value = attribute.get(i);
                    if (value != null) {
                        values.add(String.valueOf(value));
                    }
                }
                if (values.size() == 1) {
                    result.put(attribute.getID(), values.get(0));
                } else {
                    result.put(attribute.getID(), values);
                }
            }
        } catch (Exception e) {
            result.put("_error", e.getMessage());
        }
        return result;
    }

    private String defaultHomeDirectory(String username) {
        String base = trimToNull(posixHomeDirectoryBase);
        if (base == null) {
            base = "/home";
        }
        return base.endsWith("/") ? base + username : base + "/" + username;
    }

    private ModificationItem replaceAttribute(String name, String value) {
        return new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(name, value));
    }

    private ModificationItem replaceAttribute(String name, List<String> values) {
        BasicAttribute attribute = new BasicAttribute(name);
        for (String value : values) {
            attribute.add(value);
        }
        return new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute);
    }

    private List<String> mergeObjectClasses(Attribute attribute) {
        LinkedHashSet<String> values = new LinkedHashSet<>(BASE_OBJECT_CLASSES);
        if (attribute != null) {
            try {
                for (int i = 0; i < attribute.size(); i++) {
                    Object value = attribute.get(i);
                    if (value != null) {
                        values.add(String.valueOf(value));
                    }
                }
            } catch (Exception ignored) {
                // Fall back to the base classes below.
            }
        }
        return new ArrayList<>(values);
    }

    private boolean attributeContainsIgnoreCase(Attribute attribute, String expected) {
        if (attribute == null || expected == null) {
            return false;
        }
        try {
            for (int i = 0; i < attribute.size(); i++) {
                Object value = attribute.get(i);
                if (value != null && expected.equalsIgnoreCase(String.valueOf(value))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private boolean sameIgnoreCaseValues(Attribute attribute, List<String> expected) {
        List<String> current = mergeObjectClasses(attribute).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        List<String> normalizedExpected = expected.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        return current.equals(normalizedExpected);
    }

    private OptionalLong firstLongAttributeValue(Attributes attrs, String name) {
        try {
            Attribute attr = attrs.get(name);
            if (attr == null || attr.size() == 0 || attr.get(0) == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(Long.parseLong(String.valueOf(attr.get(0)).trim()));
        } catch (Exception e) {
            return OptionalLong.empty();
        }
    }

    private Long lookupLongAttribute(LdapTemplate ldapTemplate, Name dn, String name) {
        try {
            Attributes attrs = ldapTemplate.lookup(dn, new String[]{name}, (AttributesMapper<Attributes>) attributes -> attributes);
            return firstLongAttributeValue(attrs, name).isPresent() ? firstLongAttributeValue(attrs, name).getAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class GroupBindingSnapshot {
        private List<String> directMembers = new ArrayList<>();
        private List<String> primaryUsers = new ArrayList<>();
        private List<String> boundUsers = new ArrayList<>();
        private int boundUserCount;
    }

    private static final class GroupSelection {
        private final long gidNumber;
        private final String groupName;

        private GroupSelection(long gidNumber, String groupName) {
            this.gidNumber = gidNumber;
            this.groupName = groupName;
        }
    }

    private String resolveUserBaseDn(Cluster cluster) {
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        if (endpoint != null && endpoint.getUserBaseDn() != null && !endpoint.getUserBaseDn().isEmpty()) {
            String userBaseDn = endpoint.getUserBaseDn().trim();
            String baseDn = endpoint.getBaseDn();
            String normalizedBaseDn = trimToNull(baseDn);
            if (normalizedBaseDn != null) {
                String userBaseLower = userBaseDn.toLowerCase(Locale.ROOT);
                String baseLower = normalizedBaseDn.toLowerCase(Locale.ROOT);
                if (userBaseLower.equals(baseLower)) {
                    return "";
                }
                if (userBaseLower.endsWith("," + baseLower)) {
                    return userBaseDn.substring(0, userBaseDn.length() - normalizedBaseDn.length() - 1);
                }
            }
            return userBaseDn;
        }
        return defaultUserBaseDn;
    }

    private List<String> ldapBindDnCandidates(ClusterEndpoint endpoint) {
        String username = endpoint.getUsername() == null ? "" : endpoint.getUsername().trim();
        if (username.isEmpty()) {
            return Collections.singletonList("");
        }
        if (username.contains("=")) {
            return Collections.singletonList(username);
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String escaped = Rdn.escapeValue(username).toString();
        String baseDn = trimToNull(endpoint.getBaseDn());
        String userBaseDn = normalizeUserBaseDn(endpoint.getUserBaseDn(), baseDn);

        if (baseDn != null) {
            candidates.add("cn=" + escaped + "," + baseDn);
        }
        if (userBaseDn != null) {
            candidates.add("uid=" + escaped + "," + userBaseDn);
            candidates.add("cn=" + escaped + "," + userBaseDn);
        }
        candidates.add(username);
        return new ArrayList<>(candidates);
    }

    private String normalizeUserBaseDn(String userBaseDn, String baseDn) {
        String value = trimToNull(userBaseDn);
        if (value == null) {
            return baseDn;
        }
        if (baseDn == null
                || value.toLowerCase(Locale.ROOT).equals(baseDn.toLowerCase(Locale.ROOT))
                || value.toLowerCase(Locale.ROOT).endsWith("," + baseDn.toLowerCase(Locale.ROOT))) {
            return value;
        }
        return value + "," + baseDn;
    }

    private String absoluteSearchBase(String searchBase, String baseDn) {
        String base = trimToNull(baseDn);
        String search = trimToNull(searchBase);
        if (search == null) {
            return base;
        }
        if (base == null) {
            return search;
        }
        String searchLower = search.toLowerCase(Locale.ROOT);
        String baseLower = base.toLowerCase(Locale.ROOT);
        if (searchLower.equals(baseLower) || searchLower.endsWith("," + baseLower)) {
            return search;
        }
        return search + "," + base;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String firstAttributeValue(Attributes attrs, String name) {
        try {
            Attribute attr = attrs.get(name);
            if (attr == null || attr.size() == 0 || attr.get(0) == null) {
                return null;
            }
            return attr.get(0).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
