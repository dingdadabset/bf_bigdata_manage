package com.dga.access.service.authorization;

import com.dga.access.service.RangerService;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class RangerAuthorizationProvider implements AuthorizationProvider {

    @Autowired
    private RangerService rangerService;

    @Value("${hdp.hive.server2.url}")
    private String hdpHiveUrl;

    @Value("${hdp.hive.server2.username}")
    private String hdpHiveUser;

    @Value("${hdp.hive.server2.password}")
    private String hdpHivePassword;

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasAuthBackend(context, ClusterEndpoint.AUTH_RANGER)
                || AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_RANGER)) {
            return true;
        }
        String identifier = context.getClusterIdentifier();
        if (identifier != null && identifier.toUpperCase().contains("HDP")) {
            return true;
        }
        return cluster != null && cluster.getType() != null && cluster.getType().toUpperCase().contains("HDP");
    }

    @Override
    public String engineType() {
        return "HIVE";
    }

    @Override
    public String authBackend() {
        return ClusterEndpoint.AUTH_RANGER;
    }

    @Override
    public List<String> listDatabases(AuthorizationContext context) {
        try {
            return hdpJdbcTemplate(context).queryForList("SHOW DATABASES", String.class);
        } catch (Exception e) {
            System.err.println("HDP HiveServer2 list databases failed, fallback to Ranger policies: " + e.getMessage());
            return rangerService.listPolicyDatabases(rangerEndpoint(context));
        }
    }

    @Override
    public List<String> listTables(AuthorizationContext context, String database) {
        AuthorizationSupport.validateName(database);
        try {
            return hdpJdbcTemplate(context).queryForList("SHOW TABLES IN " + database, String.class);
        } catch (Exception e) {
            System.err.println("HDP HiveServer2 list tables failed, fallback to Ranger policies: " + e.getMessage());
            return rangerService.listPolicyTables(rangerEndpoint(context), database);
        }
    }

    @Override
    public List<String> listPrincipals(AuthorizationContext context) {
        return rangerService.listPolicyUsers(rangerEndpoint(context));
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        return rangerService.getUserPermissions(username, rangerEndpoint(context));
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        rangerService.grantPermission(command.getUsername(), command.getDatabase(), command.getTable(),
                command.getPermission(), rangerEndpoint(context));
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        rangerService.revokePermission(command.getUsername(), command.getDatabase(), command.getTable(),
                command.getPermission(), rangerEndpoint(context));
    }

    @Override
    public void revokeAll(AuthorizationContext context, String username) {
        System.out.println("Ranger revokeAll is not implemented because Ranger policies are resource scoped.");
    }

    private org.springframework.jdbc.core.JdbcTemplate hdpJdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(endpoint != null && endpoint.getUrl() != null ? endpoint.getUrl() : hdpHiveUrl);
        dataSource.setUsername(endpoint != null && endpoint.getUsername() != null ? endpoint.getUsername() : hdpHiveUser);
        dataSource.setPassword(endpoint != null && endpoint.getPassword() != null ? endpoint.getPassword() : hdpHivePassword);
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }

    private ClusterEndpoint rangerEndpoint(AuthorizationContext context) {
        return AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_RANGER);
    }
}
