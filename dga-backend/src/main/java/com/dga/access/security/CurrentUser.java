package com.dga.access.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            return null;
        }
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    public static String usernameOrUnknown() {
        AuthenticatedUser user = get();
        return user == null || user.getUsername() == null ? "unknown" : user.getUsername();
    }
}
