package com.kioschool.kioschoolapi.domain.mailbox.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.constraints.Size

data class MailPreviewRequestBody(
    @field:LogMasked
    @field:Size(max = 50000, message = "내용이 너무 깁니다.")
    val content: String = "",
)
