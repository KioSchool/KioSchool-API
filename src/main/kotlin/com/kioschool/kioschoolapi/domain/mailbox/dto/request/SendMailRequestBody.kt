package com.kioschool.kioschoolapi.domain.mailbox.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendMailRequestBody(
    val workspaceId: Long,

    @field:LogMasked
    @field:NotBlank(message = "제목을 입력해 주세요.")
    @field:Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    val subject: String,

    @field:LogMasked
    @field:NotBlank(message = "내용을 입력해 주세요.")
    @field:Size(max = 10000, message = "내용은 10000자를 넘을 수 없습니다.")
    val content: String,
) {
    fun normalizedSubject(): String = subject.trim()
    fun normalizedContent(): String = content.trim()
}
