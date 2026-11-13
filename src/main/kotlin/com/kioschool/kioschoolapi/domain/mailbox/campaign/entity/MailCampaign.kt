package com.kioschool.kioschoolapi.domain.mailbox.campaign.entity

import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "mail_campaign")
class MailCampaign(
    @Column(columnDefinition = "TEXT")
    var subject: String,
    @Column(columnDefinition = "TEXT")
    var bodyTemplate: String,
    @Column(columnDefinition = "TEXT")
    var filterJson: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    var createdBy: User?,
    var purgeAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: MailCampaignStatus = MailCampaignStatus.SENDING,
    var requestId: String? = null,
    /** 만들 때의 팀 공용 서명(HTML·글). 이 기능 전에 만든 발송은 null이라 서명 없이 나간다. */
    @Column(columnDefinition = "TEXT")
    var signatureJson: String? = null,
) : BaseEntity()
