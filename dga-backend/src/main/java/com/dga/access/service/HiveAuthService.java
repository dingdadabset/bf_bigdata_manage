package com.dga.access.service;

import com.dga.access.service.authorization.AuthorizationService;
import com.dga.access.service.authorization.GrantCommand;
import com.dga.access.service.authorization.RevokeCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HiveAuthService {

    @Autowired
    private AuthorizationService authorizationService;

    public List<String> listDatabases(String clusterName) {
        return authorizationService.listDatabases(clusterName);
    }

    public List<String> listTables(String clusterName, String database) {
        return authorizationService.listTables(clusterName, database);
    }

    public List<Map<String, Object>> getUserPermissions(String username, String clusterName) {
        return authorizationService.getUserPermissions(username, clusterName);
    }

    public void grantPermission(String username, String database, String permission, String clusterName) {
        GrantCommand command = new GrantCommand();
        command.setUsername(username);
        command.setDatabase(database);
        command.setPermission(permission);
        command.setCluster(clusterName);
        authorizationService.grant(command);
    }

    public void grantTablePermission(String username, String database, String table, String permission, String clusterName) {
        GrantCommand command = new GrantCommand();
        command.setUsername(username);
        command.setDatabase(database);
        command.setTable(table);
        command.setPermission(permission);
        command.setCluster(clusterName);
        authorizationService.grant(command);
    }

    public void revokePermission(String username, String database, String permission, String clusterName) {
        RevokeCommand command = new RevokeCommand();
        command.setUsername(username);
        command.setDatabase(database);
        command.setPermission(permission);
        command.setCluster(clusterName);
        authorizationService.revoke(command);
    }

    public void revokeTablePermission(String username, String database, String table, String permission, String clusterName) {
        RevokeCommand command = new RevokeCommand();
        command.setUsername(username);
        command.setDatabase(database);
        command.setTable(table);
        command.setPermission(permission);
        command.setCluster(clusterName);
        authorizationService.revoke(command);
    }

    public void revokeAll(String username, String clusterName) {
        authorizationService.revokeAll(username, clusterName);
    }
}
