package com.dga.access.service;

import com.dga.access.security.AuthenticatedUser;
import com.dga.access.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Component
public class AdminGuard {

    private static final String DELETE_FORBIDDEN_MESSAGE = "仅 admin 或超级用户可执行删除操作";

    public void requireDeletePrivilege(HttpServletRequest request) {
        requirePlatformAdmin(request, DELETE_FORBIDDEN_MESSAGE);
    }

    public void requirePlatformAdmin(HttpServletRequest request, String forbiddenMessage) {
        AuthenticatedUser user = CurrentUser.get();
        if (user != null && user.isAdmin()) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    public void requireRootAdmin(HttpServletRequest request) {
        AuthenticatedUser user = CurrentUser.get();
        if (user != null && user.isRootAdmin()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅 admin 用户可设置超级管理员");
    }

}
