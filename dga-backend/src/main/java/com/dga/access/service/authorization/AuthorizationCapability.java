package com.dga.access.service.authorization;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationCapability {
    private String cluster;
    private String clusterCode;
    private String clusterName;
    private String engineType;
    private String authBackend;
    private String endpointType;
    private String endpointUrl;
    private List<String> principalTypes = new ArrayList<>();
    private List<String> resourceTypes = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private boolean requiresLdap;
    private String status;
    private List<String> warnings = new ArrayList<>();
    private IdentityCapabilities identity = new IdentityCapabilities();
    private RbacCapabilities rbac = new RbacCapabilities();
    private GrantCapabilities grant = new GrantCapabilities();
    private UiCapabilities ui = new UiCapabilities();

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(String authBackend) {
        this.authBackend = authBackend;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public List<String> getPrincipalTypes() {
        return principalTypes;
    }

    public void setPrincipalTypes(List<String> principalTypes) {
        this.principalTypes = principalTypes;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean isRequiresLdap() {
        return requiresLdap;
    }

    public void setRequiresLdap(boolean requiresLdap) {
        this.requiresLdap = requiresLdap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public IdentityCapabilities getIdentity() {
        return identity;
    }

    public void setIdentity(IdentityCapabilities identity) {
        this.identity = identity;
    }

    public RbacCapabilities getRbac() {
        return rbac;
    }

    public void setRbac(RbacCapabilities rbac) {
        this.rbac = rbac;
    }

    public GrantCapabilities getGrant() {
        return grant;
    }

    public void setGrant(GrantCapabilities grant) {
        this.grant = grant;
    }

    public UiCapabilities getUi() {
        return ui;
    }

    public void setUi(UiCapabilities ui) {
        this.ui = ui;
    }

    public static class IdentityCapabilities {
        private String mode;
        private String userSource;
        private boolean requiresLdap;
        private boolean canCreateUser;
        private boolean canImportUsers;
        private String importMode;
        private boolean supportsLdapGroupInput;
        private boolean supportsLdapGroupManagement;
        private String userLabel;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getUserSource() {
            return userSource;
        }

        public void setUserSource(String userSource) {
            this.userSource = userSource;
        }

        public boolean isRequiresLdap() {
            return requiresLdap;
        }

        public void setRequiresLdap(boolean requiresLdap) {
            this.requiresLdap = requiresLdap;
        }

        public boolean isCanCreateUser() {
            return canCreateUser;
        }

        public void setCanCreateUser(boolean canCreateUser) {
            this.canCreateUser = canCreateUser;
        }

        public boolean isCanImportUsers() {
            return canImportUsers;
        }

        public void setCanImportUsers(boolean canImportUsers) {
            this.canImportUsers = canImportUsers;
        }

        public String getImportMode() {
            return importMode;
        }

        public void setImportMode(String importMode) {
            this.importMode = importMode;
        }

        public boolean isSupportsLdapGroupInput() {
            return supportsLdapGroupInput;
        }

        public void setSupportsLdapGroupInput(boolean supportsLdapGroupInput) {
            this.supportsLdapGroupInput = supportsLdapGroupInput;
        }

        public boolean isSupportsLdapGroupManagement() {
            return supportsLdapGroupManagement;
        }

        public void setSupportsLdapGroupManagement(boolean supportsLdapGroupManagement) {
            this.supportsLdapGroupManagement = supportsLdapGroupManagement;
        }

        public String getUserLabel() {
            return userLabel;
        }

        public void setUserLabel(String userLabel) {
            this.userLabel = userLabel;
        }
    }

    public static class RbacCapabilities {
        private boolean supportsRoles;
        private boolean supportsRoleTemplates;
        private boolean supportsDefaultRole;
        private boolean supportsNativeUserRole;
        private boolean supportsNativeGroupRole;
        private boolean usesMaterializedPolicies;
        private boolean userAssignmentUsesMaterializedPolicies;
        private boolean groupAssignmentUsesMaterializedPolicies;
        private String defaultSubjectType;
        private List<String> allowedSubjectTypes = new ArrayList<>();

        public boolean isSupportsRoles() {
            return supportsRoles;
        }

        public void setSupportsRoles(boolean supportsRoles) {
            this.supportsRoles = supportsRoles;
        }

        public boolean isSupportsRoleTemplates() {
            return supportsRoleTemplates;
        }

        public void setSupportsRoleTemplates(boolean supportsRoleTemplates) {
            this.supportsRoleTemplates = supportsRoleTemplates;
        }

        public boolean isSupportsDefaultRole() {
            return supportsDefaultRole;
        }

        public void setSupportsDefaultRole(boolean supportsDefaultRole) {
            this.supportsDefaultRole = supportsDefaultRole;
        }

        public boolean isSupportsNativeUserRole() {
            return supportsNativeUserRole;
        }

        public void setSupportsNativeUserRole(boolean supportsNativeUserRole) {
            this.supportsNativeUserRole = supportsNativeUserRole;
        }

        public boolean isSupportsNativeGroupRole() {
            return supportsNativeGroupRole;
        }

        public void setSupportsNativeGroupRole(boolean supportsNativeGroupRole) {
            this.supportsNativeGroupRole = supportsNativeGroupRole;
        }

        public boolean isUsesMaterializedPolicies() {
            return usesMaterializedPolicies;
        }

        public void setUsesMaterializedPolicies(boolean usesMaterializedPolicies) {
            this.usesMaterializedPolicies = usesMaterializedPolicies;
        }

        public boolean isUserAssignmentUsesMaterializedPolicies() {
            return userAssignmentUsesMaterializedPolicies;
        }

        public void setUserAssignmentUsesMaterializedPolicies(boolean userAssignmentUsesMaterializedPolicies) {
            this.userAssignmentUsesMaterializedPolicies = userAssignmentUsesMaterializedPolicies;
        }

        public boolean isGroupAssignmentUsesMaterializedPolicies() {
            return groupAssignmentUsesMaterializedPolicies;
        }

        public void setGroupAssignmentUsesMaterializedPolicies(boolean groupAssignmentUsesMaterializedPolicies) {
            this.groupAssignmentUsesMaterializedPolicies = groupAssignmentUsesMaterializedPolicies;
        }

        public String getDefaultSubjectType() {
            return defaultSubjectType;
        }

        public void setDefaultSubjectType(String defaultSubjectType) {
            this.defaultSubjectType = defaultSubjectType;
        }

        public List<String> getAllowedSubjectTypes() {
            return allowedSubjectTypes;
        }

        public void setAllowedSubjectTypes(List<String> allowedSubjectTypes) {
            this.allowedSubjectTypes = allowedSubjectTypes;
        }
    }

    public static class GrantCapabilities {
        private boolean supportsDirectGrant;
        private boolean supportsRoleSubsetGrant;
        private boolean supportsUserRoleSubsetGrant;
        private boolean supportsGroupRoleSubsetGrant;
        private boolean supportsDatabasePermission;
        private boolean supportsTablePermission;
        private boolean supportsColumnPermission;
        private boolean supportsWildcardTable;

        public boolean isSupportsDirectGrant() {
            return supportsDirectGrant;
        }

        public void setSupportsDirectGrant(boolean supportsDirectGrant) {
            this.supportsDirectGrant = supportsDirectGrant;
        }

        public boolean isSupportsRoleSubsetGrant() {
            return supportsRoleSubsetGrant;
        }

        public void setSupportsRoleSubsetGrant(boolean supportsRoleSubsetGrant) {
            this.supportsRoleSubsetGrant = supportsRoleSubsetGrant;
        }

        public boolean isSupportsUserRoleSubsetGrant() {
            return supportsUserRoleSubsetGrant;
        }

        public void setSupportsUserRoleSubsetGrant(boolean supportsUserRoleSubsetGrant) {
            this.supportsUserRoleSubsetGrant = supportsUserRoleSubsetGrant;
        }

        public boolean isSupportsGroupRoleSubsetGrant() {
            return supportsGroupRoleSubsetGrant;
        }

        public void setSupportsGroupRoleSubsetGrant(boolean supportsGroupRoleSubsetGrant) {
            this.supportsGroupRoleSubsetGrant = supportsGroupRoleSubsetGrant;
        }

        public boolean isSupportsDatabasePermission() {
            return supportsDatabasePermission;
        }

        public void setSupportsDatabasePermission(boolean supportsDatabasePermission) {
            this.supportsDatabasePermission = supportsDatabasePermission;
        }

        public boolean isSupportsTablePermission() {
            return supportsTablePermission;
        }

        public void setSupportsTablePermission(boolean supportsTablePermission) {
            this.supportsTablePermission = supportsTablePermission;
        }

        public boolean isSupportsColumnPermission() {
            return supportsColumnPermission;
        }

        public void setSupportsColumnPermission(boolean supportsColumnPermission) {
            this.supportsColumnPermission = supportsColumnPermission;
        }

        public boolean isSupportsWildcardTable() {
            return supportsWildcardTable;
        }

        public void setSupportsWildcardTable(boolean supportsWildcardTable) {
            this.supportsWildcardTable = supportsWildcardTable;
        }
    }

    public static class UiCapabilities {
        private String userLabel;
        private String createUserTitle;
        private String importUsersLabel;
        private String principalHelpText;
        private String roleEffectiveHelpText;
        private boolean hideLdapPanels;
        private boolean showLdapGroupInput;
        private boolean showUserDirectBinding;
        private boolean showRoleTemplates;
        private boolean showDefaultRole;

        public String getUserLabel() {
            return userLabel;
        }

        public void setUserLabel(String userLabel) {
            this.userLabel = userLabel;
        }

        public String getCreateUserTitle() {
            return createUserTitle;
        }

        public void setCreateUserTitle(String createUserTitle) {
            this.createUserTitle = createUserTitle;
        }

        public String getImportUsersLabel() {
            return importUsersLabel;
        }

        public void setImportUsersLabel(String importUsersLabel) {
            this.importUsersLabel = importUsersLabel;
        }

        public String getPrincipalHelpText() {
            return principalHelpText;
        }

        public void setPrincipalHelpText(String principalHelpText) {
            this.principalHelpText = principalHelpText;
        }

        public String getRoleEffectiveHelpText() {
            return roleEffectiveHelpText;
        }

        public void setRoleEffectiveHelpText(String roleEffectiveHelpText) {
            this.roleEffectiveHelpText = roleEffectiveHelpText;
        }

        public boolean isHideLdapPanels() {
            return hideLdapPanels;
        }

        public void setHideLdapPanels(boolean hideLdapPanels) {
            this.hideLdapPanels = hideLdapPanels;
        }

        public boolean isShowLdapGroupInput() {
            return showLdapGroupInput;
        }

        public void setShowLdapGroupInput(boolean showLdapGroupInput) {
            this.showLdapGroupInput = showLdapGroupInput;
        }

        public boolean isShowUserDirectBinding() {
            return showUserDirectBinding;
        }

        public void setShowUserDirectBinding(boolean showUserDirectBinding) {
            this.showUserDirectBinding = showUserDirectBinding;
        }

        public boolean isShowRoleTemplates() {
            return showRoleTemplates;
        }

        public void setShowRoleTemplates(boolean showRoleTemplates) {
            this.showRoleTemplates = showRoleTemplates;
        }

        public boolean isShowDefaultRole() {
            return showDefaultRole;
        }

        public void setShowDefaultRole(boolean showDefaultRole) {
            this.showDefaultRole = showDefaultRole;
        }
    }
}
