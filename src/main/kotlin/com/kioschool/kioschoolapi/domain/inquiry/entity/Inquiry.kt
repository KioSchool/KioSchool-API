package com.kioschool.kioschoolapi.domain.inquiry.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "inquiry")
class Inquiry(
    var title: String,
    @Column(columnDefinition = "TEXT")
    var content: String,
    var replyEmail: String,
    var privacyAgreedAt: LocalDateTime,
    var purgeAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: InquiryStatus = InquiryStatus.PENDING,
    var answeredAt: LocalDateTime? = null,
    var closedAt: LocalDateTime? = null,
    var closedReason: String? = null,
    @JsonIgnore
    @OneToMany(mappedBy = "inquiry", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<InquiryImage> = mutableListOf(),
    @JsonIgnore
    @OneToOne(mappedBy = "inquiry", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var reply: InquiryReply? = null,
) : BaseEntity()
