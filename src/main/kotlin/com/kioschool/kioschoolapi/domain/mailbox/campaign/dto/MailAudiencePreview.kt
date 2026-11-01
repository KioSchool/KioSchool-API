package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import org.springframework.data.domain.Page

data class MailAudienceRow(
    val workspaceId: Long,
    val workspaceName: String,
    val schoolName: String,
    val ownerName: String,
    val ownerEmail: String,
)

data class MailAudiencePreview(
    val targetCount: Int,
    val noEmailCount: Int,
    val unsubscribedCount: Int,
    val withdrawnCount: Int,
    val dailyLimit: Int,
    val content: Page<MailAudienceRow>,
)
