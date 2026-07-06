package com.fundmatrix.controller;

import com.fundmatrix.dto.AdminStatsDto;
import com.fundmatrix.dto.ComplianceSummaryDto;
import com.fundmatrix.dto.DistributorDashboardDto;
import com.fundmatrix.dto.InvestorDashboardDto;
import com.fundmatrix.service.CommissionService;
import com.fundmatrix.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboards", description = "Role-specific aggregated read models")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CommissionService commissionService;

    public DashboardController(DashboardService dashboardService, CommissionService commissionService) {
        this.dashboardService = dashboardService;
        this.commissionService = commissionService;
    }

    @GetMapping("/investor")
    public InvestorDashboardDto investor() {
        return dashboardService.investorDashboard();
    }

    @GetMapping("/distributor")
    public DistributorDashboardDto distributor() {
        return commissionService.currentDistributorDashboard();
    }

    @GetMapping("/admin")
    public AdminStatsDto admin() {
        return dashboardService.adminStats();
    }

    @GetMapping("/compliance")
    public ComplianceSummaryDto compliance() {
        return dashboardService.complianceSummary();
    }
}
