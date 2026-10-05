package com.droneops.controller;

import com.droneops.dto.DashboardStats;
import com.droneops.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Panel", description = "Komuta merkezi özet verileri")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Özet sayıları getir", description = "Duruma göre görev sayıları ve filo özeti.")
    @GetMapping("/stats")
    public DashboardStats stats() {
        return dashboardService.stats();
    }
}
