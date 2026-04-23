package com.kioschool.kioschoolapi.domain.dashboard.controller

import com.kioschool.kioschoolapi.domain.dashboard.dto.DashboardDto
import com.kioschool.kioschoolapi.domain.dashboard.facade.DashboardFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Dashboard")
@RestController
@RequestMapping("/admin")
class AdminDashboardController(
    private val dashboardFacade: DashboardFacade
) {
    @Operation(summary = "대시보드 조회", description = "워크스페이스 대시보드를 조회합니다.")
    @GetMapping("/dashboard")
    fun getDashboard(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): DashboardDto {
        return dashboardFacade.getDashboard(username, workspaceId)
    }
}
