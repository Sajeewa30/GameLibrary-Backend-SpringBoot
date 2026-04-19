package com.backend.gamelibrarybackend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "ok");
    }

    @GetMapping("/health/db")
    public ResponseEntity<Map<String, Object>> healthDb() {
        Map<String, Object> body = new HashMap<>();
        if (dataSource == null) {
            body.put("status", "down");
            body.put("reason", "no datasource");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            body.put("status", valid ? "ok" : "down");
            return valid
                    ? ResponseEntity.ok(body)
                    : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception e) {
            body.put("status", "down");
            body.put("reason", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}
