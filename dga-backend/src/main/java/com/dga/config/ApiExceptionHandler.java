package com.dga.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleThrowable(Throwable error, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("path", request == null ? null : request.getRequestURI());
        body.put("message", readableErrorMessage(error));
        body.put("exception", error == null ? "Unknown" : error.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String readableErrorMessage(Throwable error) {
        if (error == null) {
            return "未知错误";
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String primary = trimToNull(error.getMessage());
        String rootMessage = trimToNull(root.getMessage());
        String rootType = root.getClass().getSimpleName();
        if (primary == null && rootMessage == null) {
            return rootType;
        }
        if (primary == null) {
            return rootType + ": " + rootMessage;
        }
        if (root == error || rootMessage == null || primary.equals(rootMessage)) {
            return primary;
        }
        return primary + "；根因: " + rootType + ": " + rootMessage;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
