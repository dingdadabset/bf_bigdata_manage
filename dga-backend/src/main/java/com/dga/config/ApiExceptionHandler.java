package com.dga.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException error, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        HttpStatus status = error.getStatus();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("path", request == null ? null : request.getRequestURI());
        body.put("message", readableErrorMessage(error));
        body.put("exception", error.getClass().getSimpleName());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleThrowable(Throwable error, HttpServletRequest request) {
        ResponseStatusException statusException = findResponseStatusException(error);
        if (statusException != null) {
            return handleResponseStatus(statusException, request);
        }
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
        if (error instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) error;
            String reason = trimToNull(statusException.getReason());
            if (reason != null) {
                Throwable root = rootCause(error);
                String rootMessage = trimToNull(root.getMessage());
                if (root != error && rootMessage != null && !reason.equals(rootMessage)) {
                    return reason + "；根因: " + root.getClass().getSimpleName() + ": " + rootMessage;
                }
                return reason;
            }
        }
        Throwable root = rootCause(error);
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

    private ResponseStatusException findResponseStatusException(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != current) {
            if (current instanceof ResponseStatusException) {
                return (ResponseStatusException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private Throwable rootCause(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
