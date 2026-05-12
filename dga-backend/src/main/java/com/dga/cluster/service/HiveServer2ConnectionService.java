package com.dga.cluster.service;

import com.dga.cluster.config.HiveJdbcDriverProperties;
import com.dga.cluster.dto.HiveJdbcDriverOption;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HiveServer2ConnectionService {

    private static final String MODERN_DRIVER_CLASS = "org.apache.hive.jdbc.HiveDriver";
    private static final Map<String, ExternalDriverHandle> EXTERNAL_DRIVER_CACHE = new ConcurrentHashMap<>();

    @Value("${dga.hive.legacy-driver.jar-path:}")
    private String legacyDriverJarPath;

    @Value("${dga.hive.legacy-driver.class-name:org.apache.hive.jdbc.HiveDriver}")
    private String legacyDriverClassName;

    @Autowired
    private HiveJdbcDriverProperties driverProperties;

    public Connection openConnection(ClusterEndpoint endpoint) throws Exception {
        String profile = normalizeDriverProfile(endpoint);
        if (ClusterEndpoint.DRIVER_PROFILE_LEGACY_CDH5.equals(profile)) {
            return openSelectedExternalConnection(endpoint, true);
        }
        if (ClusterEndpoint.DRIVER_PROFILE_AUTO.equals(profile)) {
            try {
                return openModernConnection(endpoint);
            } catch (Exception e) {
                if (!isLegacyCompatibilityError(e)) {
                    throw e;
                }
                return openSelectedExternalConnection(endpoint, false);
            }
        }
        return openModernConnection(endpoint);
    }

    public void testConnection(ClusterEndpoint endpoint) throws Exception {
        DriverManager.setLoginTimeout(5);
        try (Connection ignored = openConnection(endpoint)) {
            // Opening and closing the connection is enough for endpoint reachability.
        }
    }

    public JdbcTemplate jdbcTemplate(ClusterEndpoint endpoint) {
        return new JdbcTemplate(dataSource(endpoint));
    }

    public List<HiveJdbcDriverOption> listDriverOptions() {
        List<RegisteredDriver> drivers = new ArrayList<>(resolveRegisteredDrivers().values());
        drivers.sort(Comparator.comparing(RegisteredDriver::isBuiltIn).reversed().thenComparing(RegisteredDriver::getName));
        List<HiveJdbcDriverOption> options = new ArrayList<>();
        for (RegisteredDriver driver : drivers) {
            options.add(new HiveJdbcDriverOption(
                    driver.getKey(),
                    driver.getName(),
                    driver.getDescription(),
                    driver.isBuiltIn(),
                    ClusterEndpoint.DRIVER_KEY_MODERN.equals(driver.getKey()),
                    driver.isDefaultForAuto()));
        }
        return options;
    }

    public String normalizeDriverProfile(ClusterEndpoint endpoint) {
        if (endpoint == null || endpoint.getEndpointType() == null
                || !ClusterEndpoint.TYPE_HIVE_SERVER2.equalsIgnoreCase(endpoint.getEndpointType())) {
            return ClusterEndpoint.DRIVER_PROFILE_MODERN;
        }
        String profile = endpoint.getDriverProfile();
        if (profile == null || profile.trim().isEmpty()) {
            return ClusterEndpoint.DRIVER_PROFILE_MODERN;
        }
        String normalized = profile.trim().toUpperCase();
        if (ClusterEndpoint.DRIVER_PROFILE_AUTO.equals(normalized)
                || ClusterEndpoint.DRIVER_PROFILE_LEGACY_CDH5.equals(normalized)
                || ClusterEndpoint.DRIVER_PROFILE_MODERN.equals(normalized)) {
            return normalized;
        }
        return ClusterEndpoint.DRIVER_PROFILE_MODERN;
    }

    public String normalizeDriverKey(ClusterEndpoint endpoint) {
        if (endpoint == null || endpoint.getEndpointType() == null
                || !ClusterEndpoint.TYPE_HIVE_SERVER2.equalsIgnoreCase(endpoint.getEndpointType())) {
            return null;
        }
        String profile = normalizeDriverProfile(endpoint);
        if (ClusterEndpoint.DRIVER_PROFILE_MODERN.equals(profile)) {
            return ClusterEndpoint.DRIVER_KEY_MODERN;
        }
        String requestedKey = trimToNull(endpoint.getDriverKey());
        Map<String, RegisteredDriver> drivers = resolveRegisteredDrivers();
        if (requestedKey != null && drivers.containsKey(requestedKey)) {
            return requestedKey;
        }
        if (requestedKey != null) {
            return requestedKey;
        }
        RegisteredDriver fallback = defaultExternalDriver(drivers);
        if (fallback != null) {
            return fallback.getKey();
        }
        return ClusterEndpoint.DRIVER_KEY_LEGACY_CDH5;
    }

    private DataSource dataSource(ClusterEndpoint endpoint) {
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return open(endpoint);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return open(overrideCredentials(endpoint, username, password));
            }

            private Connection open(ClusterEndpoint target) throws SQLException {
                try {
                    return openConnection(target);
                } catch (SQLException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SQLException(e.getMessage(), e);
                }
            }
        };
    }

    private ClusterEndpoint overrideCredentials(ClusterEndpoint endpoint, String username, String password) {
        ClusterEndpoint target = new ClusterEndpoint();
        target.setEndpointType(endpoint.getEndpointType());
        target.setDriverProfile(endpoint.getDriverProfile());
        target.setDriverKey(endpoint.getDriverKey());
        target.setUrl(endpoint.getUrl());
        target.setUsername(username);
        target.setPassword(password);
        return target;
    }

    private Connection openModernConnection(ClusterEndpoint endpoint) throws Exception {
        Class.forName(MODERN_DRIVER_CLASS);
        return DriverManager.getConnection(
                endpoint.getUrl(),
                nullToEmpty(endpoint.getUsername()),
                nullToEmpty(endpoint.getPassword()));
    }

    private Connection openSelectedExternalConnection(ClusterEndpoint endpoint, boolean explicitLegacySelection) throws Exception {
        RegisteredDriver driver = resolveSelectedExternalDriver(endpoint);
        if (driver == null) {
            if (explicitLegacySelection) {
                throw new IllegalStateException("当前端点已选择旧版 Hive 驱动，但系统未配置可用旧驱动。请先在配置中注册驱动文件。");
            }
            throw new IllegalStateException("检测到老版本 HiveServer2 协议，但系统未配置可回退的旧版 Hive 驱动。请补充 driverKey 或 legacy 驱动配置。");
        }
        return openExternalConnection(endpoint, driver);
    }

    private Connection openExternalConnection(ClusterEndpoint endpoint, RegisteredDriver registeredDriver) throws Exception {
        ExternalDriverHandle handle = loadExternalDriver(registeredDriver);
        Properties properties = new Properties();
        if (!isBlank(endpoint.getUsername())) {
            properties.setProperty("user", endpoint.getUsername().trim());
        }
        if (!isBlank(endpoint.getPassword())) {
            properties.setProperty("password", endpoint.getPassword());
        }
        Connection connection = handle.driver.connect(endpoint.getUrl(), properties);
        if (connection == null) {
            throw new SQLException("Hive JDBC 驱动 [" + registeredDriver.getName() + "] 未接受该连接串: " + endpoint.getUrl());
        }
        return connection;
    }

    private ExternalDriverHandle loadExternalDriver(RegisteredDriver registeredDriver) throws Exception {
        File jarFile = validateJarFile(registeredDriver.getJarPath(), registeredDriver.getKey());
        String cacheKey = jarFile.getAbsolutePath() + "::" + registeredDriver.getDriverClassName();
        ExternalDriverHandle cached = EXTERNAL_DRIVER_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        synchronized (EXTERNAL_DRIVER_CACHE) {
            cached = EXTERNAL_DRIVER_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, jdbcApiParentClassLoader());
            Class<?> driverClass = Class.forName(registeredDriver.getDriverClassName(), true, loader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            ExternalDriverHandle handle = new ExternalDriverHandle(driver, loader);
            EXTERNAL_DRIVER_CACHE.put(cacheKey, handle);
            return handle;
        }
    }

    private File validateJarFile(String jarPath, String driverKey) {
        String normalizedPath = trimToNull(jarPath);
        if (normalizedPath == null) {
            throw new IllegalStateException("Hive JDBC 驱动 [" + driverKey + "] 未配置 jarPath");
        }
        File jarFile = new File(normalizedPath);
        if (!jarFile.isFile()) {
            throw new IllegalStateException("Hive JDBC 驱动 [" + driverKey + "] 文件不存在: " + normalizedPath);
        }
        return jarFile;
    }

    private ClassLoader jdbcApiParentClassLoader() {
        ClassLoader parent = Driver.class.getClassLoader();
        if (parent != null) {
            return parent;
        }
        return HiveServer2ConnectionService.class.getClassLoader();
    }

    private RegisteredDriver resolveSelectedExternalDriver(ClusterEndpoint endpoint) {
        Map<String, RegisteredDriver> drivers = resolveRegisteredDrivers();
        String key = normalizeDriverKey(endpoint);
        RegisteredDriver selected = drivers.get(key);
        if (selected != null && !selected.isBuiltIn()) {
            return selected;
        }
        return defaultExternalDriver(drivers);
    }

    private RegisteredDriver defaultExternalDriver(Map<String, RegisteredDriver> drivers) {
        return drivers.values().stream()
                .filter(driver -> !driver.isBuiltIn())
                .filter(RegisteredDriver::isDefaultForAuto)
                .findFirst()
                .orElseGet(() -> drivers.values().stream()
                        .filter(driver -> !driver.isBuiltIn())
                        .findFirst()
                        .orElse(null));
    }

    private Map<String, RegisteredDriver> resolveRegisteredDrivers() {
        Map<String, RegisteredDriver> drivers = new LinkedHashMap<>();
        drivers.put(ClusterEndpoint.DRIVER_KEY_MODERN, RegisteredDriver.builtInModern());

        RegisteredDriver compatibilityLegacy = fromCompatibilityProperties();
        if (compatibilityLegacy != null) {
            drivers.put(compatibilityLegacy.getKey(), compatibilityLegacy);
        }

        for (HiveJdbcDriverProperties.ExternalDriver config : driverProperties.getDrivers()) {
            RegisteredDriver driver = fromConfiguredDriver(config);
            if (driver != null) {
                drivers.put(driver.getKey(), driver);
            }
        }
        return drivers;
    }

    private RegisteredDriver fromCompatibilityProperties() {
        String jarPath = trimToNull(legacyDriverJarPath);
        if (jarPath == null) {
            return null;
        }
        String description = "兼容配置的 CDH5 / Hive 1.x 旧版 JDBC 驱动";
        return new RegisteredDriver(
                ClusterEndpoint.DRIVER_KEY_LEGACY_CDH5,
                "CDH5 / Hive 1.x 旧版驱动",
                trimToNull(legacyDriverClassName) == null ? MODERN_DRIVER_CLASS : legacyDriverClassName.trim(),
                jarPath,
                description,
                false,
                true);
    }

    private RegisteredDriver fromConfiguredDriver(HiveJdbcDriverProperties.ExternalDriver config) {
        if (config == null || !config.isEnabled()) {
            return null;
        }
        String key = trimToNull(config.getKey());
        String driverClassName = trimToNull(config.getDriverClassName());
        String jarPath = trimToNull(config.getJarPath());
        if (key == null || driverClassName == null || jarPath == null) {
            return null;
        }
        String name = trimToNull(config.getName()) == null ? key : config.getName().trim();
        String description = trimToNull(config.getDescription());
        return new RegisteredDriver(
                key,
                name,
                driverClassName,
                jarPath,
                description == null ? "自定义 Hive JDBC 驱动" : description,
                false,
                config.isDefaultForAuto());
    }

    private boolean isLegacyCompatibilityError(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("client_protocol")
                        || lower.contains("topensessionreq")
                        || lower.contains("tapplicationexception")
                        || lower.contains("unknown hs2 problem when communicating with thrift server")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class ExternalDriverHandle {
        private final Driver driver;
        @SuppressWarnings("unused")
        private final URLClassLoader classLoader;

        private ExternalDriverHandle(Driver driver, URLClassLoader classLoader) {
            this.driver = driver;
            this.classLoader = classLoader;
        }
    }

    private static final class RegisteredDriver {
        private final String key;
        private final String name;
        private final String driverClassName;
        private final String jarPath;
        private final String description;
        private final boolean builtIn;
        private final boolean defaultForAuto;

        private RegisteredDriver(String key,
                                 String name,
                                 String driverClassName,
                                 String jarPath,
                                 String description,
                                 boolean builtIn,
                                 boolean defaultForAuto) {
            this.key = key;
            this.name = name;
            this.driverClassName = driverClassName;
            this.jarPath = jarPath;
            this.description = description;
            this.builtIn = builtIn;
            this.defaultForAuto = defaultForAuto;
        }

        public static RegisteredDriver builtInModern() {
            return new RegisteredDriver(
                    ClusterEndpoint.DRIVER_KEY_MODERN,
                    "默认现代驱动",
                    MODERN_DRIVER_CLASS,
                    null,
                    "内置 Hive JDBC 驱动，适用于较新的 HiveServer2 集群",
                    true,
                    false);
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public String getJarPath() {
            return jarPath;
        }

        public String getDescription() {
            return description;
        }

        public boolean isBuiltIn() {
            return builtIn;
        }

        public boolean isDefaultForAuto() {
            return defaultForAuto;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof RegisteredDriver)) {
                return false;
            }
            RegisteredDriver that = (RegisteredDriver) object;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
