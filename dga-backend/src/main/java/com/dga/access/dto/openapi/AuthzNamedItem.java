package com.dga.access.dto.openapi;

public class AuthzNamedItem {

    private String name;

    public AuthzNamedItem() {
    }

    public AuthzNamedItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
