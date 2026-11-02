package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class MailCampaignSummaryDto(
    val id: Long,
    @field:LogMasked
    val subject: String,
    val status: MailCampaignStatus,
    val totalCount: Long,
    val sentCount: Long,
    val pendingCount: Long,
    val failedCount: Long,
    val excludedCount: Long,
    @field:LogMasked
    val createdByName: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(campaign: MailCampaign, counts: Map<MailRecipientStatus, Long>) = MailCampaignSummaryDto(
            id = campaign.id,
            subject = campaign.subject,
            status = campaign.status,
            totalCount = counts.values.sum(),
            sentCount = counts[MailRecipientStatus.SENT] ?: 0,
            pendingCount = (counts[MailRecipientStatus.PENDING] ?: 0) + (counts[MailRecipientStatus.SENDING] ?: 0),
            failedCount = counts[MailRecipientStatus.FAILED] ?: 0,
            excludedCount = counts[MailRecipientStatus.EXCLUDED] ?: 0,
            createdByName = campaign.createdBy?.name,
            createdAt = campaign.createdAt,
        )
    }
}

data class MailCampaignRecipientDto(
    val id: Long,
    val workspaceId: Long?,
    val workspaceName: String,
    @field:LogMasked
    val name: String,
    val email: String,
    val status: MailRecipientStatus,
    val failureReason: String?,
    val sentAt: LocalDateTime?,
) {
    companion object {
        fun of(recipient: MailCampaignRecipient) = MailCampaignRecipientDto(
            id = recipient.id,
            workspaceId = recipient.workspace?.id,
            workspaceName = recipient.workspaceName,
            name = recipient.name,
            email = recipient.email,
            status = recipient.status,
            failureReason = recipient.failureReason,
            sentAt = recipient.sentAt,
        )
    }
}

data class MailCampaignDetailDto(
    val summary: MailCampaignSummaryDto,
    val dailyLimit: Int,
    val recipients: Page<MailCampaignRecipientDto>,
)
