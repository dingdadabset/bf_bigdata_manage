package com.dga.access.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
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
    private LdapTemplate ldapTemplate;

    public void createUser(String username, String password, String email) {
        Name dn = buildUserDn(username);

        if (checkUserExists(dn)) {
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
        Name dn = buildUserDn(username);
        try {
            ldapTemplate.unbind(dn);
            System.out.println("LDAP user deleted: " + username);
        } catch (Exception e) {
            throw new RuntimeException("LDAP Delete Error: " + e.getMessage());
        }
    }

    public boolean checkUserExists(Name dn) {
        try {
            ldapTemplate.lookup(dn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean userExists(String username) {
        return checkUserExists(buildUserDn(username));
    }

    public Name buildUserDn(String username) {
        return LdapNameBuilder.newInstance()
                .add("cn", "accounts")
                .add("cn", "users")
                .add("uid", username)
                .build();
    }

    public String getUserDnString(String username) {
        return buildUserDn(username).toString();
    }

    public Map<String, Object> getUserInfo(String username) {
        Name dn = buildUserDn(username);
        try {
            Attributes attrs = ldapTemplate.lookup(dn, (AttributesMapper<Attributes>) a -> a);
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
}
