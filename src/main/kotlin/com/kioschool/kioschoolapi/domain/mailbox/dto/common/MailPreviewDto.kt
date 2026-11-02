package com.kioschool.kioschoolapi.domain.mailbox.dto.common

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked

data class MailPreviewDto(
    @field:LogMasked
    val html: String,
)
