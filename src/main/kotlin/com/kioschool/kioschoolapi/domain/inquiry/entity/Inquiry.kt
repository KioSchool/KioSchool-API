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
    // mappedBy 쪽 @OneToOne이라 bytecode enhancement 없이는 진짜 지연 로딩 프록시를 만들 수 없다.
    // Inquiry를 읽을 때마다 inquiry_reply 존재 여부를 확인하는 추가 쿼리가 나가지만,
    // hibernate.default_batch_fetch_size=100 덕에 페이지(최대 100건)당 한 번으로 묶인다.
    // 슈퍼어드민 전용 저트래픽 엔드포인트라 감수한다.
    @OneToOne(mappedBy = "inquiry", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var reply: InquiryReply? = null,
) : BaseEntity()
