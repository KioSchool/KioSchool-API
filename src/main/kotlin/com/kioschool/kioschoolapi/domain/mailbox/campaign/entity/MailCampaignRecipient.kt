package com.kioschool.kioschoolapi.domain.mailbox.campaign.entity

import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "mail_campaign_recipient")
class MailCampaignRecipient(
    @ManyToOne(fetch = FetchType.LAZY)
    var campaign: MailCampaign,
    @ManyToOne(fetch = FetchType.LAZY)
    var workspace: Workspace?,
    var workspaceName: String,
    @Column(columnDefinition = "TEXT")
    var email: String,
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: MailRecipientStatus = MailRecipientStatus.PENDING,
    var failureReason: String? = null,
    var resendEmailId: String? = null,
    var attempt: Int = 0,
    var claimedAt: LocalDateTime? = null,
    var sentAt: LocalDateTime? = null,
) : BaseEntity()
