package com.dga.access.service.authorization;

import com.dga.cluster.entity.ClusterEndpoint;

import java.util.List;
import java.util.regex.Pattern;

public final class AuthorizationSupport {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    private AuthorizationSupport() {
    }

    public static void validateName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name");
        }
    }

    public static void validatePermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Invalid permission");
        }
        String upper = permission.toUpperCase();
        if (!"ALL".equals(upper) && !"SELECT".equals(upper) && !"INSERT".equals(upper)
                && !"CREATE".equals(upper) && !"ALTER".equals(upper) && !"DROP".equals(upper)) {
            throw new IllegalArgumentException("Invalid permission");
        }
    }

    public static ClusterEndpoint firstEndpoint(AuthorizationContext context, String endpointType) {
        if (context == null || context.getEndpoints() == null) {
            return null;
        }
        List<ClusterEndpoint> endpoints = context.getEndpoints();
        for (ClusterEndpoint endpoint : endpoints) {
            if (endpointType.equalsIgnoreCase(endpoint.getEndpointType())) {
                return endpoint;
            }
        }
        return null;
    }

    public static boolean hasEndpoint(AuthorizationContext context, String endpointType) {
        return firstEndpoint(context, endpointType) != null;
    }

    public static boolean hasAuthBackend(AuthorizationContext context, String authBackend) {
        if (context == null || context.getEndpoints() == null) {
            return false;
        }
        for (ClusterEndpoint endpoint : context.getEndpoints()) {
            if (endpoint.getAuthBackend() != null && authBackend.equalsIgnoreCase(endpoint.getAuthBackend())) {
                return true;
            }
        }
        return false;
    }
}
