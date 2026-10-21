package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.response.HealthResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Controller exposing lightweight smoke and health endpoints for the application.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    /**
     * Public endpoint to verify that the application layer is alive and receiving traffic.
     *
     * @return 200 OK with operational status, UTC timestamp, and active environment profile.
     */
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        String[] activeProfiles = environment.getActiveProfiles();
        String currentEnv = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(",", activeProfiles)
                : (environment.getDefaultProfiles().length > 0 ? environment.getDefaultProfiles()[0] : "dev");

        HealthResponse response = new HealthResponse(
                "UP",
                Instant.now(),
                currentEnv
        );
        return ResponseEntity.ok(response);
    }
}
