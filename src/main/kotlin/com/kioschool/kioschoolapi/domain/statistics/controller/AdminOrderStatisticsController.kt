package com.kioschool.kioschoolapi.domain.statistics.controller

import com.kioschool.kioschoolapi.domain.statistics.dto.DailyOrderStatisticResponse
import com.kioschool.kioschoolapi.domain.statistics.facade.OrderStatisticsFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Tag(name = "Admin Order Statistics Controller", description = "어드민 전용 주문 통계 API")
@RestController
@RequestMapping("/admin")
class AdminOrderStatisticsController(
    private val orderStatisticsFacade: OrderStatisticsFacade
) {

    @Operation(summary = "일별 통계 조회", description = "특정 영업일(09:00 ~ 익일 08:59)의 매출, 건수, 회전율 등 통계를 조회합니다. date 미입력 시 당일 통계를 조회합니다.")
    @GetMapping("/statistics")
    fun getDailyStatistics(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): DailyOrderStatisticResponse {
        return orderStatisticsFacade.getStatistics(username, workspaceId, date)
    }
}
