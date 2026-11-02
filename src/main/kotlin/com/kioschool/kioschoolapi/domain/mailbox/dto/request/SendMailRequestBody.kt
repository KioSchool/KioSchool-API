package com.kioschool.kioschoolapi.domain.mailbox.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SendMailRequestBody(
    val workspaceId: Long,

    @field:LogMasked
    @field:NotBlank(message = "제목을 입력해 주세요.")
    @field:Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    val subject: String,

    @field:LogMasked
    @field:NotBlank(message = "내용을 입력해 주세요.")
    @field:Size(max = 50000, message = "내용이 너무 깁니다.")
    val content: String,

    // 응답을 못 받아 다시 눌러도 같은 메일이 두 번 나가지 않도록 화면이 만든 요청 번호
    @field:Pattern(regexp = "^[A-Za-z0-9-]{1,64}$", message = "요청 번호가 올바르지 않습니다.")
    val requestId: String? = null,
) {
    fun normalizedSubject(): String = subject.trim()
}
