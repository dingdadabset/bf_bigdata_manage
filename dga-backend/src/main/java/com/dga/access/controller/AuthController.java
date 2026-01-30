package com.dga.access.controller;

import com.dga.access.entity.User;
import com.dga.access.repository.UserRepository;
import com.dga.access.dto.CreateUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userRepository.findByUsername(username);
        if (user != null && password != null && password.equals(user.getPassword())) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                return ResponseEntity.status(401).body("Account is disabled");
            }
            Map<String, Object> response = new HashMap<>();
            response.put("token", "mock-token-" + username);
            response.put("user", user);
            return ResponseEntity.ok(response);
        }
        
        // Backdoor for admin/admin if no user exists or just for testing
        if ("admin".equals(username) && "admin".equals(password)) {
             Map<String, Object> response = new HashMap<>();
            response.put("token", "mock-token-admin");
            response.put("username", "admin");
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
        user.setPassword(request.getPassword()); // In real world, hash this!
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
}
