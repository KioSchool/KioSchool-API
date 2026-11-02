package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked

data class MailCampaignPreviewDto(
    @field:LogMasked
    val subject: String,
    @field:LogMasked
    val html: String,
    val workspaceName: String?,
    val ownerEmail: String?,
)
