package com.kioschool.kioschoolapi.domain.inquiry.dto.request

import jakarta.validation.constraints.Size

data class CloseInquiryRequestBody(
    // 내부 메모. 고객에게 노출되는 경로는 없다.
    @field:Size(max = 200, message = "종결 사유는 200자를 넘을 수 없습니다.")
    val closedReason: String? = null,
) {
    fun normalizedClosedReason(): String? = closedReason?.trim()?.ifBlank { null }
}
