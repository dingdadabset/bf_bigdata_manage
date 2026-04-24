package com.dga.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * SPA (Single Page Application) Controller
 * 
 * Smartly handles errors:
 * 1. For 404s on frontend routes (non-API, non-static), forwards to index.html (History Mode support).
 * 2. For API errors or real 404s (e.g. missing JS file), returns JSON error response.
 */
@Controller
public class SpaController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (status != null) ? Integer.valueOf(status.toString()) : HttpStatus.INTERNAL_SERVER_ERROR.value();
        
        // Get the original path that caused the error
        String path = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        
        // Logic for 404 Not Found
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            // Heuristic: If it's NOT an API call AND NOT a static resource, it's likely a frontend route
            boolean isApi = path != null && path.startsWith("/api");
            boolean isAsset = path != null && (path.startsWith("/assets") || path.contains(".") || path.startsWith("/favicon.ico"));
            
            if (!isApi && !isAsset) {
                return "forward:/index.html";
            }
        }
        
        // Special case: If path is /login and it's NOT found (meaning Spring Security didn't intercept it as a form login page),
        // we should forward to index.html so Vue can handle the login page.
        // However, Spring Security often intercepts /login by default. 
        // If we see a 404 for /login, it means we likely disabled default login page or it's not matching.
        // If we see a 200 OK from Spring Security's default page, this controller won't be hit.

        // For API errors or static file 404s, return JSON
        Map<String, Object> body = new HashMap<>();
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        body.put("status", statusCode);
        body.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        body.put("path", path);
        body.put("message", message == null || message.trim().isEmpty()
                ? "An unexpected error occurred"
                : message);
        
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
