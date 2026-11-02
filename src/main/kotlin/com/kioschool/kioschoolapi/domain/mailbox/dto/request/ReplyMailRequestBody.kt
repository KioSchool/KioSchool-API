package com.kioschool.kioschoolapi.domain.mailbox.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReplyMailRequestBody(
    val lastMessageId: Long,

    @field:LogMasked
    @field:NotBlank(message = "내용을 입력해 주세요.")
    @field:Size(max = 50000, message = "내용이 너무 깁니다.")
    val content: String,
)
