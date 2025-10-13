package com.smartcane.point.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/healthz")
    public String health() {
        return "ok";
    }
}
