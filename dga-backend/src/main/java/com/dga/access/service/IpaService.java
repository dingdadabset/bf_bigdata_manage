package com.dga.access.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class IpaService {

    @Value("${ipa.ssh.enabled:false}")
    private boolean enabled;

    @Value("${ipa.ssh.host:}")
    private String defaultHost;

    @Value("${ipa.ssh.port:22}")
    private int port;

    @Value("${ipa.ssh.user:}")
    private String user;

    @Value("${ipa.ssh.password:}")
    private String password;

    public String createUser(String hostOverride, String username, String firstName, String lastName, String userPassword) {
        if (!enabled) {
            throw new IllegalStateException("IPA SSH is disabled");
        }
        String host = hostOverride != null && hostOverride.trim().length() > 0 ? hostOverride.trim() : defaultHost;
        if (host == null || host.isEmpty() || user == null || user.isEmpty()) {
            throw new IllegalStateException("IPA SSH host/user is not configured");
        }
        String cmd = buildIpaCommand(username, firstName, lastName, userPassword);
        return exec(host, cmd);
    }

    public String deleteUser(String username) {
        if (!enabled) {
            throw new IllegalStateException("IPA SSH is disabled");
        }
        String host = defaultHost; // Use default host for delete
        if (host == null || host.isEmpty() || user == null || user.isEmpty()) {
            throw new IllegalStateException("IPA SSH host/user is not configured");
        }
        String cmd = "ipa user-del " + username;
        return exec(host, "bash -lc \"" + cmd + "\"");
    }

    private String buildIpaCommand(String username, String firstName, String lastName, String userPassword) {
        String escaped = userPassword.replace("\\", "\\\\").replace("'", "'\"'\"'");
        String here = "$'" + escaped.replace("\n", "\\n") + "\\n" + escaped.replace("\n", "\\n") + "\\n'";
        String base = "ipa user-add " + username + " --first=" + firstName + " --last=" + lastName + " --password";
        return "bash -lc \"" + base + " <<< " + here + "\"";
    }

    private String exec(String host, String command) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int read;
            while ((read = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
            StringBuilder se = new StringBuilder();
            while ((read = err.read(buf)) > 0) {
                se.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
            int code = channel.getExitStatus();
            channel.disconnect();
            session.disconnect();
            if (code != 0) {
                String errorMsg = se.toString();
                if (errorMsg != null && errorMsg.toLowerCase().contains("already exists")) {
                    System.out.println("IPA SSH: User already exists. Treating as success.");
                    return sb.toString();
                }
                throw new RuntimeException("IPA Error: " + errorMsg);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("IPA SSH Failed: " + e.getMessage());
        }
    }
}

