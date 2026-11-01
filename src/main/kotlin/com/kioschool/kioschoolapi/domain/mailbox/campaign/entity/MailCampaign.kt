package com.kioschool.kioschoolapi.domain.mailbox.campaign.entity

import com.kioschool.kioschoolapi.domain.mailbox.campaign.enum.MailCampaignStatus
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
    var buttonLabel: String?,
    @Column(columnDefinition = "TEXT")
    var buttonUrlTemplate: String?,
    @Column(columnDefinition = "TEXT")
    var filterJson: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    var createdBy: User?,
    var purgeAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: MailCampaignStatus = MailCampaignStatus.SENDING,
) : BaseEntity()
