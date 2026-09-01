package com.research.gbjournal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "gbjournal-backend",
            "timestamp", Instant.now().toString(),
            "message", "Gono Bishwabidyalay Journal API is active"
        ));
    }
}
