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

    @field:LogMasked
    @field:NotBlank(message = "내용을 입력해 주세요.")
    @field:Size(max = 10000, message = "내용은 10000자를 넘을 수 없습니다.")
    val content: String,

    @field:Size(max = 40, message = "버튼 문구는 40자를 넘을 수 없습니다.")
    val buttonLabel: String? = null,

    @field:LogMasked
    @field:Size(max = 2000, message = "버튼 링크는 2000자를 넘을 수 없습니다.")
    val buttonUrl: String? = null,

    // 화면에서 확인한 곳 수. 그사이 대상이 바뀌었으면 보내지 않는다
    val expectedCount: Int? = null,

    // 응답을 못 받아 다시 눌러도 같은 발송을 또 만들지 않도록 화면이 만든 요청 번호
    @field:Pattern(regexp = "^[A-Za-z0-9-]{1,64}$", message = "요청 번호가 올바르지 않습니다.")
    val requestId: String? = null,
) {
    fun normalizedSubject(): String = subject.trim()
    fun normalizedContent(): String = content.trim()
    fun normalizedButtonLabel(): String? = buttonLabel?.trim()?.ifEmpty { null }
    fun normalizedButtonUrl(): String? = buttonUrl?.trim()?.ifEmpty { null }
}
