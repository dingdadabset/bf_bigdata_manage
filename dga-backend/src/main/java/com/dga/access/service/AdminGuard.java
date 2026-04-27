package com.dga.access.service;

import com.dga.access.entity.User;
import com.dga.access.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Component
public class AdminGuard {

    private static final String DELETE_FORBIDDEN_MESSAGE = "仅 admin 或超级用户可执行删除操作";

    @Autowired
    private UserRepository userRepository;

    public void requireDeletePrivilege(HttpServletRequest request) {
        String username = request == null ? null : request.getHeader("X-DGA-Username");
        if (username == null || username.trim().isEmpty()) {
            throw forbidden();
        }

        String normalized = username.trim();
        if ("admin".equals(normalized)) {
            return;
        }

        User user = userRepository.findByUsername(normalized);
        if (user != null && Integer.valueOf(1).equals(user.getIsAdmin())) {
            return;
        }

        throw forbidden();
    }

    public void requireRootAdmin(HttpServletRequest request) {
        String username = request == null ? null : request.getHeader("X-DGA-Username");
        if ("admin".equals(username == null ? null : username.trim())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅 admin 用户可设置超级管理员");
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, DELETE_FORBIDDEN_MESSAGE);
    }
}
