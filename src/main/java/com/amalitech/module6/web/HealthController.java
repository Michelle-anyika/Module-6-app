package com.amalitech.module6.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lightweight liveness endpoint for ALB target group and container health checks. */
@RestController
public class HealthController {

    private final AppInfo appInfo;

    public HealthController(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", appInfo.getVersion());
    }
}
