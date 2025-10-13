package com.smartcane.point.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "헬스체크 API")
@RestController
public class HealthController {

    @Operation(summary = "애플리케이션 헬스 체크")
    @GetMapping("/api/healthz")
    public String health() {
        return "ok";
    }
}
