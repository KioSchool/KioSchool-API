package com.kioschool.kioschoolapi.domain.inquiry.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "inquiry_reply")
class InquiryReply(
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", unique = true)
    val inquiry: Inquiry,
    val subject: String,
    @Column(columnDefinition = "TEXT")
    val content: String,
    // 발송 당시 수신 주소 스냅샷
    val recipientEmail: String,
    @ManyToOne(fetch = FetchType.LAZY)
    val respondedBy: User,
    val sentAt: LocalDateTime,
) : BaseEntity()
