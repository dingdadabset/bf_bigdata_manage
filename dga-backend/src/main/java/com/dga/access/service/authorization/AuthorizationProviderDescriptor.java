package com.dga.access.service.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthorizationProviderDescriptor {
    private String engineType;
    private String authBackend;
    private String endpointType;
    private boolean endpointRequired = true;
    private String missingEndpointWarning;
    private List<String> principalTypes = new ArrayList<>();
    private List<String> resourceTypes = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private boolean requiresLdap;
    private AuthorizationCapability.IdentityCapabilities identity = new AuthorizationCapability.IdentityCapabilities();
    private AuthorizationCapability.RbacCapabilities rbac = new AuthorizationCapability.RbacCapabilities();
    private AuthorizationCapability.GrantCapabilities grant = new AuthorizationCapability.GrantCapabilities();
    private AuthorizationCapability.UiCapabilities ui = new AuthorizationCapability.UiCapabilities();

    public static AuthorizationProviderDescriptor create(String engineType, String authBackend, String endpointType) {
        AuthorizationProviderDescriptor descriptor = new AuthorizationProviderDescriptor();
        descriptor.setEngineType(engineType);
        descriptor.setAuthBackend(authBackend);
        descriptor.setEndpointType(endpointType);
        return descriptor;
    }

    public AuthorizationProviderDescriptor endpointRequired(boolean endpointRequired) {
        this.endpointRequired = endpointRequired;
        return this;
    }

    public AuthorizationProviderDescriptor missingEndpointWarning(String missingEndpointWarning) {
        this.missingEndpointWarning = missingEndpointWarning;
        return this;
    }

    public AuthorizationProviderDescriptor principalTypes(String... principalTypes) {
        this.principalTypes = list(principalTypes);
        return this;
    }

    public AuthorizationProviderDescriptor resourceTypes(String... resourceTypes) {
        this.resourceTypes = list(resourceTypes);
        return this;
    }

    public AuthorizationProviderDescriptor permissions(String... permissions) {
        this.permissions = list(permissions);
        return this;
    }

    public AuthorizationProviderDescriptor requiresLdap(boolean requiresLdap) {
        this.requiresLdap = requiresLdap;
        return this;
    }

    public AuthorizationProviderDescriptor identity(String mode, String userSource, boolean canCreateUser,
                                                    boolean canImportUsers, String importMode,
                                                    boolean supportsLdapGroupInput,
                                                    boolean supportsLdapGroupManagement,
                                                    String userLabel) {
        identity.setMode(mode);
        identity.setUserSource(userSource);
        identity.setRequiresLdap(requiresLdap);
        identity.setCanCreateUser(canCreateUser);
        identity.setCanImportUsers(canImportUsers);
        identity.setImportMode(importMode);
        identity.setSupportsLdapGroupInput(supportsLdapGroupInput);
        identity.setSupportsLdapGroupManagement(supportsLdapGroupManagement);
        identity.setUserLabel(userLabel);
        return this;
    }

    public AuthorizationProviderDescriptor rbac(boolean supportsRoles, boolean supportsRoleTemplates,
                                                boolean supportsDefaultRole, boolean supportsNativeUserRole,
                                                boolean supportsNativeGroupRole, boolean usesMaterializedPolicies,
                                                boolean userAssignmentUsesMaterializedPolicies,
                                                boolean groupAssignmentUsesMaterializedPolicies,
                                                String defaultSubjectType, String... allowedSubjectTypes) {
        rbac.setSupportsRoles(supportsRoles);
        rbac.setSupportsRoleTemplates(supportsRoleTemplates);
        rbac.setSupportsDefaultRole(supportsDefaultRole);
        rbac.setSupportsNativeUserRole(supportsNativeUserRole);
        rbac.setSupportsNativeGroupRole(supportsNativeGroupRole);
        rbac.setUsesMaterializedPolicies(usesMaterializedPolicies);
        rbac.setUserAssignmentUsesMaterializedPolicies(userAssignmentUsesMaterializedPolicies);
        rbac.setGroupAssignmentUsesMaterializedPolicies(groupAssignmentUsesMaterializedPolicies);
        rbac.setDefaultSubjectType(defaultSubjectType);
        rbac.setAllowedSubjectTypes(list(allowedSubjectTypes));
        return this;
    }

    public AuthorizationProviderDescriptor grant(boolean supportsDirectGrant, boolean supportsRoleSubsetGrant,
                                                 boolean supportsUserRoleSubsetGrant,
                                                 boolean supportsGroupRoleSubsetGrant,
                                                 boolean supportsDatabasePermission,
                                                 boolean supportsTablePermission,
                                                 boolean supportsColumnPermission,
                                                 boolean supportsWildcardTable) {
        grant.setSupportsDirectGrant(supportsDirectGrant);
        grant.setSupportsRoleSubsetGrant(supportsRoleSubsetGrant);
        grant.setSupportsUserRoleSubsetGrant(supportsUserRoleSubsetGrant);
        grant.setSupportsGroupRoleSubsetGrant(supportsGroupRoleSubsetGrant);
        grant.setSupportsDatabasePermission(supportsDatabasePermission);
        grant.setSupportsTablePermission(supportsTablePermission);
        grant.setSupportsColumnPermission(supportsColumnPermission);
        grant.setSupportsWildcardTable(supportsWildcardTable);
        return this;
    }

    public AuthorizationProviderDescriptor ui(String userLabel, String createUserTitle, String importUsersLabel,
                                              String principalHelpText, String roleEffectiveHelpText) {
        ui.setUserLabel(userLabel);
        ui.setCreateUserTitle(createUserTitle);
        ui.setImportUsersLabel(importUsersLabel);
        ui.setPrincipalHelpText(principalHelpText);
        ui.setRoleEffectiveHelpText(roleEffectiveHelpText);
        ui.setHideLdapPanels(!requiresLdap);
        ui.setShowLdapGroupInput(identity.isSupportsLdapGroupInput());
        ui.setShowUserDirectBinding(rbac.getAllowedSubjectTypes().contains("USER"));
        ui.setShowRoleTemplates(rbac.isSupportsRoleTemplates());
        ui.setShowDefaultRole(rbac.isSupportsDefaultRole());
        return this;
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

    public boolean isEndpointRequired() {
        return endpointRequired;
    }

    public void setEndpointRequired(boolean endpointRequired) {
        this.endpointRequired = endpointRequired;
    }

    public String getMissingEndpointWarning() {
        return missingEndpointWarning;
    }

    public void setMissingEndpointWarning(String missingEndpointWarning) {
        this.missingEndpointWarning = missingEndpointWarning;
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

    public AuthorizationCapability.IdentityCapabilities getIdentity() {
        return identity;
    }

    public void setIdentity(AuthorizationCapability.IdentityCapabilities identity) {
        this.identity = identity;
    }

    public AuthorizationCapability.RbacCapabilities getRbac() {
        return rbac;
    }

    public void setRbac(AuthorizationCapability.RbacCapabilities rbac) {
        this.rbac = rbac;
    }

    public AuthorizationCapability.GrantCapabilities getGrant() {
        return grant;
    }

    public void setGrant(AuthorizationCapability.GrantCapabilities grant) {
        this.grant = grant;
    }

    public AuthorizationCapability.UiCapabilities getUi() {
        return ui;
    }

    public void setUi(AuthorizationCapability.UiCapabilities ui) {
        this.ui = ui;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
