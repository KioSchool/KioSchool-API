package com.kioschool.kioschoolapi.domain.dashboard.controller

import com.kioschool.kioschoolapi.domain.dashboard.dto.FestivalCalendarDto
import com.kioschool.kioschoolapi.domain.dashboard.dto.SuperAdminDashboardDto
import com.kioschool.kioschoolapi.domain.dashboard.facade.SuperAdminDashboardFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Super Admin Dashboard Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminDashboardController(
    private val superAdminDashboardFacade: SuperAdminDashboardFacade
) {
    @Operation(
        summary = "서비스 전체 현황 대시보드",
        description = "전체 유저 수/신규 추이, 워크스페이스 수/온보딩 완료율, 서비스 전체 매출 및 주문 통계를 조회합니다."
    )
    @GetMapping("/dashboard")
    fun getDashboard(): SuperAdminDashboardDto {
        return superAdminDashboardFacade.getDashboard()
    }

    @Operation(
        summary = "축제 달력",
        description = "주문 15건 이상인 주점을 기준으로 월별 축제 운영 현황을 달력 형태로 조회합니다. 대학교별 통계 및 주점별 상세 지표를 포함합니다."
    )
    @GetMapping("/workspaces/festival-calendar")
    fun getFestivalCalendar(
        @RequestParam year: Int,
        @RequestParam month: Int
    ): FestivalCalendarDto {
        return superAdminDashboardFacade.getFestivalCalendar(year, month)
    }
}
