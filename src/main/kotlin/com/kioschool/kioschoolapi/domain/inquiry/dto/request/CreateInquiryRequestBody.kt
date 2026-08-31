package com.kioschool.kioschoolapi.domain.inquiry.dto.request

import com.kioschool.kioschoolapi.global.logging.annotation.Masked
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInquiryRequestBody(
    @field:NotBlank(message = "문의 제목을 입력해 주세요.")
    @field:Size(max = 100, message = "문의 제목은 100자를 넘을 수 없습니다.")
    val title: String,

    @field:Masked
    @field:NotBlank(message = "문의 내용을 입력해 주세요.")
    @field:Size(max = 2000, message = "문의 내용은 2000자를 넘을 수 없습니다.")
    val content: String,

    @field:Masked
    @field:NotBlank(message = "답변받을 이메일을 입력해 주세요.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val replyEmail: String,

    @field:AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
    val privacyConsent: Boolean,
) {
    fun normalizedTitle(): String = title.trim()
    fun normalizedContent(): String = content.trim()
    fun normalizedReplyEmail(): String = replyEmail.trim().lowercase()
}
