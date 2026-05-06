package com.dga.cluster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "dga.hive.jdbc")
public class HiveJdbcDriverProperties {

    private List<ExternalDriver> drivers = new ArrayList<>();

    public List<ExternalDriver> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<ExternalDriver> drivers) {
        this.drivers = drivers == null ? new ArrayList<>() : drivers;
    }

    public static class ExternalDriver {
        private String key;
        private String name;
        private String driverClassName;
        private String jarPath;
        private String description;
        private boolean enabled = true;
        private boolean defaultForAuto = false;

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

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getJarPath() {
            return jarPath;
        }

        public void setJarPath(String jarPath) {
            this.jarPath = jarPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDefaultForAuto() {
            return defaultForAuto;
        }

        public void setDefaultForAuto(boolean defaultForAuto) {
            this.defaultForAuto = defaultForAuto;
        }
    }
}
