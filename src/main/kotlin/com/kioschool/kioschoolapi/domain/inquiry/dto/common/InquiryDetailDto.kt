package com.kioschool.kioschoolapi.domain.inquiry.dto.common

import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import java.time.LocalDateTime

data class InquiryDetailDto(
    val id: Long,
    val title: String,
    val content: String,
    val replyEmail: String,
    val status: InquiryStatus,
    val imageCount: Int,
    val closedReason: String?,
    val createdAt: LocalDateTime?,
    val images: List<InquiryImageDto>,
    val reply: InquiryReplyDto?,
) {
    companion object {
        fun of(inquiry: Inquiry, images: List<InquiryImageDto>) = InquiryDetailDto(
            id = inquiry.id,
            title = inquiry.title,
            content = inquiry.content,
            replyEmail = inquiry.replyEmail,
            status = inquiry.status,
            imageCount = images.size,
            closedReason = inquiry.closedReason,
            createdAt = inquiry.createdAt,
            images = images,
            reply = inquiry.reply?.let { InquiryReplyDto.of(it) },
        )
    }
}
