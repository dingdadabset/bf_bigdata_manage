package com.dga.access.security;

public class AuthenticatedUser {

    private final String username;
    private final boolean admin;
    private final boolean rootAdmin;

    public AuthenticatedUser(String username, boolean admin, boolean rootAdmin) {
        this.username = username;
        this.admin = admin;
        this.rootAdmin = rootAdmin;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isRootAdmin() {
        return rootAdmin;
    }
}
