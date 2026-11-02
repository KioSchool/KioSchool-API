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
    // 값이 있으면 조건 대신 이 주점들에게만 보낸다. 기간은 값을 채우는 데만 쓴다.
    val workspaceIds: Set<Long>? = null,
)
