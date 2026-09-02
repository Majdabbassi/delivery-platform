package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get dashboard overview", description = "Retrieves consolidated counts and metrics for the admin dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        log.info("Fetching dashboard overview");
        return ResponseEntity.ok(dashboardService.getDashboardOverview());
    }
}