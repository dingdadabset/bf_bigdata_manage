package com.dga.cluster.dto;

public class HiveJdbcDriverOption {

    private String key;
    private String name;
    private String description;
    private boolean builtIn;
    private boolean defaultOption;
    private boolean defaultLegacy;

    public HiveJdbcDriverOption() {
    }

    public HiveJdbcDriverOption(String key,
                                String name,
                                String description,
                                boolean builtIn,
                                boolean defaultOption,
                                boolean defaultLegacy) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.builtIn = builtIn;
        this.defaultOption = defaultOption;
        this.defaultLegacy = defaultLegacy;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public boolean isDefaultOption() {
        return defaultOption;
    }

    public void setDefaultOption(boolean defaultOption) {
        this.defaultOption = defaultOption;
    }

    public boolean isDefaultLegacy() {
        return defaultLegacy;
    }

    public void setDefaultLegacy(boolean defaultLegacy) {
        this.defaultLegacy = defaultLegacy;
    }
}
