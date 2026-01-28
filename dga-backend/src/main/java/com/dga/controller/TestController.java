package com.dga.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin
public class TestController {
    
    @GetMapping("/api/health")
    public String healthCheck() {
        return "DGA Backend is running successfully!";
    }
}
