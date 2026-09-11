package com.kioschool.kioschoolapi.domain.inquiry.dto.common

import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryReply
import java.time.LocalDateTime

data class InquiryReplyDto(
    val subject: String,
    val content: String,
    val sentAt: LocalDateTime,
) {
    companion object {
        fun of(reply: InquiryReply) = InquiryReplyDto(
            subject = reply.subject,
            content = reply.content,
            sentAt = reply.sentAt,
        )
    }
}
