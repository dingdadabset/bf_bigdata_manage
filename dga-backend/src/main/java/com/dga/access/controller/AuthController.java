package com.dga.access.controller;

import com.dga.access.entity.User;
import com.dga.access.repository.UserRepository;
import com.dga.access.dto.CreateUserRequest;
import com.dga.access.security.JwtService;
import com.dga.access.service.AdminGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${dga.auth.allow-admin-bootstrap:false}")
    private boolean allowAdminBootstrap;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userRepository.findByUsername(username);
        if (user != null && passwordMatches(password, user.getPassword())) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                return ResponseEntity.status(401).body("Account is disabled");
            }
            if (!isEncodedPassword(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(password));
            }
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwtService.createToken(user));
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationSeconds());
            response.put("user", platformUserResponse(user));
            return ResponseEntity.ok(response);
        }
        
        if (allowAdminBootstrap && "admin".equals(username) && "admin".equals(password)) {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> adminUser = new HashMap<>();
            adminUser.put("username", "admin");
            adminUser.put("isAdmin", 1);
            adminUser.put("role", "Admin");

            response.put("token", jwtService.createToken("admin", true));
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationSeconds());
            response.put("user", adminUser);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        
        // Map nickname from FirstName/LastName or Username
        String nickname = request.getUsername();
        if (request.getFirstName() != null) {
            nickname = request.getFirstName();
            if (request.getLastName() != null) {
                nickname += " " + request.getLastName();
            }
        }
        user.setNickname(nickname);
        
        // Set defaults
        user.setAuthType("local");
        user.setStatus(1); // Active
        user.setIsAdmin(0);
        
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = normalize(request.get("username"));
        String email = normalize(request.get("email"));
        String newPassword = request.get("newPassword");

        User user = requireLocalPasswordUser(username);
        if (email == null || user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或邮箱不匹配");
        }

        updatePassword(user, newPassword);
        return ResponseEntity.ok("密码已重置，请使用新密码登录");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String username = normalize(request.get("username"));
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        User user = requireLocalPasswordUser(username);
        if (!passwordMatches(oldPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码不正确");
        }

        updatePassword(user, newPassword);
        return ResponseEntity.ok("密码已修改，请重新登录");
    }

    @GetMapping("/platform-users")
    public List<Map<String, Object>> listPlatformUsers(HttpServletRequest request) {
        adminGuard.requireRootAdmin(request);
        return userRepository.findAll().stream()
                .map(this::platformUserResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/platform-users/{username}/super-admin")
    public Map<String, Object> setSuperAdmin(@PathVariable String username,
                                             @RequestParam(defaultValue = "true") boolean enabled,
                                             HttpServletRequest request) {
        adminGuard.requireRootAdmin(request);
        if ("admin".equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "admin 用户默认拥有超级管理员权限，无需修改");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "登录账号不存在: " + username);
        }
        user.setIsAdmin(enabled ? 1 : 0);
        userRepository.save(user);

        return platformUserResponse(user);
    }

    private Map<String, Object> platformUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("email", user.getEmail());
        response.put("authType", user.getAuthType());
        response.put("status", user.getStatus());
        response.put("isAdmin", user.getIsAdmin());
        response.put("createTime", user.getCreateTime());
        response.put("lastLoginTime", user.getLastLoginTime());
        return response;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isEncodedPassword(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isEncodedPassword(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private User requireLocalPasswordUser(String username) {
        if (username == null || username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入用户名");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "登录账号不存在");
        }
        String authType = user.getAuthType();
        if (authType != null && !authType.trim().isEmpty() && !"local".equalsIgnoreCase(authType.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前账号不是本地密码账号，请使用对应认证方式登录");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已禁用，无法修改密码");
        }
        return user;
    }

    private void updatePassword(User user, String newPassword) {
        String password = normalize(newPassword);
        if (password == null || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码至少需要 6 位");
        }
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
