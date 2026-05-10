package com.kioschool.kioschoolapi.domain.insight.controller

import com.kioschool.kioschoolapi.domain.insight.service.DailyInsightCardGenerationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Super Admin Insight Card Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminInsightCardController(
    private val generationService: DailyInsightCardGenerationService
) {
    @Operation(
        summary = "인사이트 카드 수동 재생성",
        description = "지정 날짜의 모든 워크스페이스 카드를 재생성합니다 (cron 실패 복구용)."
    )
    @PostMapping("/insight-card/regenerate")
    fun regenerate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): Map<String, Any> {
        generationService.generateForDate(date)
        return mapOf("status" to "ok", "date" to date.toString())
    }
}
