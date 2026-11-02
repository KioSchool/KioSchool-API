package com.kioschool.kioschoolapi.domain.mailbox.campaign.dto

import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailAudienceStats
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailTemplateValues
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime

data class MailAudienceRow(
    val workspaceId: Long,
    val workspaceName: String,
    val schoolName: String,
    val ownerName: String,
    val ownerEmail: String,
    val tableCount: Int = 0,
    val orderCount: Long = 0,
    val revenue: Long = 0,
    val operatingDays: Long = 0,
    val firstOperatingDate: LocalDate? = null,
    val lastOperatingDate: LocalDate? = null,
    val topMenu: String? = null,
    val insightCardCount: Long = 0,
    val lastSentAt: LocalDateTime? = null,
    @field:LogMasked
    val lastSentSubject: String? = null,
    val lastReplied: Boolean = false,
) {
    fun toStats() = MailAudienceStats(
        schoolName = schoolName,
        tableCount = tableCount,
        orderCount = orderCount,
        revenue = revenue,
        operatingDays = operatingDays,
        firstOperatingDate = firstOperatingDate,
        lastOperatingDate = lastOperatingDate,
        topMenu = topMenu,
    )

    fun toTemplateValues() = MailTemplateValues(workspaceName, ownerName, workspaceId, toStats())
}

data class MailAudiencePreview(
    val targetCount: Int,
    val noEmailCount: Int,
    val unsubscribedCount: Int,
    val withdrawnCount: Int,
    val dailyLimit: Int,
    val content: Page<MailAudienceRow>,
)
