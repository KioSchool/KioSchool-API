package com.kioschool.kioschoolapi.domain.inquiry.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.Masked
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ReplyInquiryRequestBody(
    @field:NotBlank(message = "답변 제목을 입력해 주세요.")
    @field:Size(max = 150, message = "답변 제목은 150자를 넘을 수 없습니다.")
    // 메일 헤더 인젝션 방지. 제목에 CR/LF가 들어가면 헤더를 조작할 수 있다.
    @field:Pattern(regexp = "[^\\r\\n]*", message = "답변 제목에 줄바꿈을 넣을 수 없습니다.")
    val subject: String,

    @field:Masked
    @field:NotBlank(message = "답변 내용을 입력해 주세요.")
    @field:Size(max = 5000, message = "답변 내용은 5000자를 넘을 수 없습니다.")
    val content: String,
) {
    fun normalizedSubject(): String = subject.trim()
    fun normalizedContent(): String = content.trim()
}
