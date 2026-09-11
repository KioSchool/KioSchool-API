package com.kioschool.kioschoolapi.domain.inquiry.dto.common

import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import java.time.LocalDateTime

data class InquirySummaryDto(
    val id: Long,
    val title: String,
    val replyEmail: String,
    val status: InquiryStatus,
    val imageCount: Int,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(inquiry: Inquiry) = InquirySummaryDto(
            id = inquiry.id,
            title = inquiry.title,
            replyEmail = inquiry.replyEmail,
            status = inquiry.status,
            imageCount = inquiry.images.size,
            createdAt = inquiry.createdAt,
        )
    }
}
