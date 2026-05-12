package com.kioschool.kioschoolapi.domain.insight.controller

import com.kioschool.kioschoolapi.domain.insight.dto.InsightCardResponse
import com.kioschool.kioschoolapi.domain.insight.facade.InsightCardFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Insight Card Controller", description = "어제의 자랑 카드")
@RestController
@RequestMapping("/admin")
class AdminInsightCardController(
    private val insightCardFacade: InsightCardFacade
) {
    @Operation(summary = "어제의 자랑 카드 조회", description = "어제 영업일의 인사이트 카드를 반환합니다. 카드가 없으면 null.")
    @GetMapping("/insight-card")
    fun get(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): InsightCardResponse? = insightCardFacade.get(username, workspaceId)
}
