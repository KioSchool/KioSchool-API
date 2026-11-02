package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class MailCampaignRequestBody(
    @field:Valid
    val filter: MailAudienceFilter,

    @field:LogMasked
    @field:NotBlank(message = "제목을 입력해 주세요.")
    @field:Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    val subject: String,

    // 편집기가 만든 HTML. 서버에서 다시 정리한다
    @field:LogMasked
    @field:NotBlank(message = "내용을 입력해 주세요.")
    @field:Size(max = 50000, message = "내용이 너무 깁니다.")
    val content: String,

    // 화면에서 확인한 곳 수. 그사이 대상이 바뀌었으면 보내지 않는다
    val expectedCount: Int? = null,

    // 응답을 못 받아 다시 눌러도 같은 발송을 또 만들지 않도록 화면이 만든 요청 번호
    @field:Pattern(regexp = "^[A-Za-z0-9-]{1,64}$", message = "요청 번호가 올바르지 않습니다.")
    val requestId: String? = null,
) {
    fun normalizedSubject(): String = subject.trim()
}

// 입력 중에도 부르므로 비어 있어도 된다
data class MailCampaignPreviewRequestBody(
    @field:Valid
    val filter: MailAudienceFilter,

    @field:LogMasked
    @field:Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    val subject: String = "",

    @field:LogMasked
    @field:Size(max = 50000, message = "내용이 너무 깁니다.")
    val content: String = "",
)
