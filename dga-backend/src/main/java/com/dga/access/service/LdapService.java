package com.dga.access.service;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.util.HashMap;
import java.util.Map;

@Service
public class LdapService {

    @Autowired
    private LdapTemplate defaultLdapTemplate;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Value("${spring.ldap.user-base:cn=users,cn=accounts}")
    private String defaultUserBaseDn;

    public void createUser(String username, String password, String email) {
        createUser((String) null, username, password, email);
    }

    public void createUser(String clusterIdentifier, String username, String password, String email) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        LdapTemplate ldapTemplate = getLdapTemplate(cluster);
        Name dn = buildUserDn(cluster, username);

        if (checkUserExists(ldapTemplate, dn)) {
             System.out.println("User " + username + " already exists in LDAP.");
             return;
        }

        BasicAttributes attrs = new BasicAttributes();
        BasicAttribute ocattr = new BasicAttribute("objectclass");
        ocattr.add("top");
        ocattr.add("person");
        ocattr.add("organizationalPerson");
        ocattr.add("inetOrgPerson");
        attrs.put(ocattr);
        attrs.put("uid", username);
        attrs.put("cn", username);
        attrs.put("sn", username);
        attrs.put("userPassword", password);
        if (email != null && !email.isEmpty()) {
             attrs.put("mail", email);
        }

        try {
            ldapTemplate.bind(dn, null, attrs);
            System.out.println("LDAP user created: " + username);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Error: " + e.getMessage());
        }
    }

    public void deleteUser(String username) {
        deleteUser(null, username);
    }

    public void deleteUser(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        Name dn = buildUserDn(cluster, username);
        try {
            getLdapTemplate(cluster).unbind(dn);
            System.out.println("LDAP user deleted: " + username);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Delete Error: " + e.getMessage());
        }
    }

    public boolean checkUserExists(Name dn) {
        return checkUserExists(defaultLdapTemplate, dn);
    }

    public boolean checkUserExists(LdapTemplate ldapTemplate, Name dn) {
        try {
            ldapTemplate.lookup(dn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean userExists(String username) {
        return userExists(null, username);
    }

    public boolean userExists(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        return checkUserExists(getLdapTemplate(cluster), buildUserDn(cluster, username));
    }

    public Name buildUserDn(String username) {
        return buildUserDn(null, username);
    }

    public Name buildUserDn(Cluster cluster, String username) {
        String userBaseDn = resolveUserBaseDn(cluster);
        try {
            javax.naming.ldap.LdapName builder = new javax.naming.ldap.LdapName(userBaseDn);
            builder.add(new javax.naming.ldap.Rdn("uid", username));
            return builder;
        } catch (Exception e) {
            return LdapNameBuilder.newInstance()
                    .add("cn", "accounts")
                    .add("cn", "users")
                    .add("uid", username)
                    .build();
        }
    }

    public String getUserDnString(String username) {
        return buildUserDn(username).toString();
    }

    public String getUserDnString(String clusterIdentifier, String username) {
        return buildUserDn(resolveCluster(clusterIdentifier), username).toString();
    }

    public Map<String, Object> getUserInfo(String username) {
        return getUserInfo(null, username);
    }

    public Map<String, Object> getUserInfo(String clusterIdentifier, String username) {
        Cluster cluster = resolveCluster(clusterIdentifier);
        Name dn = buildUserDn(cluster, username);
        try {
            Attributes attrs = getLdapTemplate(cluster).lookup(dn, (AttributesMapper<Attributes>) a -> a);
            Map<String, Object> map = new HashMap<>();
            if (attrs.get("uid") != null) map.put("uid", attrs.get("uid").get());
            if (attrs.get("cn") != null) map.put("cn", attrs.get("cn").get());
            if (attrs.get("sn") != null) map.put("sn", attrs.get("sn").get());
            if (attrs.get("mail") != null) map.put("mail", attrs.get("mail").get());
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private LdapTemplate getLdapTemplate(Cluster cluster) {
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        if (endpoint == null || endpoint.getUrl() == null || endpoint.getUrl().isEmpty()) {
            return defaultLdapTemplate;
        }
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(endpoint.getUrl());
        contextSource.setBase(endpoint.getBaseDn());
        contextSource.setUserDn(endpoint.getUsername());
        contextSource.setPassword(endpoint.getPassword());
        contextSource.afterPropertiesSet();
        return new LdapTemplate(contextSource);
    }

    private Cluster resolveCluster(String clusterIdentifier) {
        if (clusterIdentifier == null || clusterIdentifier.isEmpty()) {
            return null;
        }
        Cluster cluster = clusterRepository.findByClusterCode(clusterIdentifier);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(clusterIdentifier);
        }
        return cluster;
    }

    private ClusterEndpoint getLdapEndpoint(Cluster cluster) {
        if (cluster == null || cluster.getClusterCode() == null) {
            return null;
        }
        java.util.List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                cluster.getClusterCode(), ClusterEndpoint.TYPE_LDAP, "ACTIVE");
        return endpoints.isEmpty() ? null : endpoints.get(0);
    }

    private String resolveUserBaseDn(Cluster cluster) {
        ClusterEndpoint endpoint = getLdapEndpoint(cluster);
        if (endpoint != null && endpoint.getUserBaseDn() != null && !endpoint.getUserBaseDn().isEmpty()) {
            return endpoint.getUserBaseDn();
        }
        return defaultUserBaseDn;
    }
}
