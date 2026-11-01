package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import jakarta.validation.constraints.Min
import java.time.LocalDate

data class MailAudienceFilter(
    val from: LocalDate,
    val to: LocalDate,
    @field:Min(value = 1, message = "주문 수는 1건 이상으로 입력해 주세요.")
    val minOrders: Long,
    val schoolName: String? = null,
    val excludedWorkspaceIds: Set<Long> = emptySet(),
)
