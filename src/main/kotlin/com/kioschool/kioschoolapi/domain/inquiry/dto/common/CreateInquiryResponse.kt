package com.kioschool.kioschoolapi.domain.inquiry.dto.common

import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import java.time.LocalDateTime

data class CreateInquiryResponse(
    val id: Long,
    val status: InquiryStatus,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(inquiry: Inquiry) = CreateInquiryResponse(
            id = inquiry.id,
            status = inquiry.status,
            createdAt = inquiry.createdAt,
        )
    }
}
