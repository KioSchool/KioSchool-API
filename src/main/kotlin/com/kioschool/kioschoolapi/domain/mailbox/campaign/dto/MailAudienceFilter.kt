package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

// 고른 주점에게 보낸다. 기간은 메일에 넣는 주문 수 같은 값을 채우는 데 쓴다.
data class MailAudienceFilter(
    val from: LocalDate,
    val to: LocalDate,
    @field:NotEmpty(message = "받을 주점을 골라 주세요.")
    val workspaceIds: Set<Long>,
)
