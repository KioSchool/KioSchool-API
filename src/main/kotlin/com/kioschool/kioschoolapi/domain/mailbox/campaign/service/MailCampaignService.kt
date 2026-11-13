package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceRow
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignRecipientDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignSummaryDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MailCampaignService(
    private val mailCampaignRepository: MailCampaignRepository,
    private val mailCampaignRecipientRepository: MailCampaignRecipientRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val objectMapper: ObjectMapper,
    private val mailHtmlSanitizer: MailHtmlSanitizer,
    @Value("\${mail.retention-days}")
    private val retentionDays: Long,
) {
    @Transactional
    fun create(
        body: MailCampaignRequestBody,
        createdBy: User,
        rows: List<MailAudienceRow>,
        signature: MailSignatureBlock? = null,
    ): MailCampaign {
        val campaign = mailCampaignRepository.save(
            MailCampaign(
                subject = body.normalizedSubject(),
                bodyTemplate = mailHtmlSanitizer.clean(body.content),
                filterJson = objectMapper.writeValueAsString(body.filter),
                createdBy = createdBy,
                purgeAt = LocalDateTime.now().plusDays(retentionDays),
                requestId = body.requestId,
                signatureJson = signature?.let { objectMapper.writeValueAsString(it) },
            )
        )
        mailCampaignRecipientRepository.saveAll(
            rows.map {
                MailCampaignRecipient(
                    campaign = campaign,
                    workspace = workspaceRepository.getReferenceById(it.workspaceId),
                    workspaceName = it.workspaceName,
                    email = it.ownerEmail,
                    name = it.ownerName,
                    statsJson = objectMapper.writeValueAsString(it.toStats()),
                )
            }
        )
        return campaign
    }

    @Transactional
    fun stop(campaignId: Long): MailCampaign {
        val campaign = getCampaign(campaignId)
        if (campaign.status != MailCampaignStatus.SENDING) return campaign

        campaign.status = MailCampaignStatus.STOPPED
        campaign.purgeAt = LocalDateTime.now().plusDays(retentionDays)
        return mailCampaignRepository.save(campaign)
    }

    /** 멈춘 발송을 다시 돌리면 남은 수신자까지 나가므로 그대로 둔다. 돌렸으면 true를 돌려준다. */
    @Transactional
    fun retryFailed(campaignId: Long): Boolean {
        val campaign = getCampaign(campaignId)
        if (campaign.status == MailCampaignStatus.STOPPED) return false

        val failed = mailCampaignRecipientRepository.findAllByCampaignAndStatus(campaign, MailRecipientStatus.FAILED)
        if (failed.isEmpty()) return false

        failed.forEach {
            it.status = MailRecipientStatus.PENDING
            it.attempt += 1
            it.failureReason = null
        }
        mailCampaignRecipientRepository.saveAll(failed)

        campaign.status = MailCampaignStatus.SENDING
        mailCampaignRepository.save(campaign)
        return true
    }

    @Transactional(readOnly = true)
    fun getSummaries(page: Int, size: Int): Page<MailCampaignSummaryDto> =
        mailCampaignRepository.findAllByOrderByIdDesc(PageRequest.of(page, size)).map { toSummary(it) }

    @Transactional(readOnly = true)
    fun findSummaryByRequestId(requestId: String): MailCampaignSummaryDto? =
        mailCampaignRepository.findByRequestId(requestId)?.let { toSummary(it) }

    @Transactional(readOnly = true)
    fun getSummary(campaignId: Long): MailCampaignSummaryDto = toSummary(getCampaign(campaignId))

    @Transactional(readOnly = true)
    fun getRecipients(campaignId: Long, page: Int, size: Int): Page<MailCampaignRecipientDto> =
        mailCampaignRecipientRepository.findAllByCampaignOrderByIdAsc(getCampaign(campaignId), PageRequest.of(page, size))
            .map { MailCampaignRecipientDto.of(it) }

    private fun toSummary(campaign: MailCampaign): MailCampaignSummaryDto {
        val counts = mailCampaignRecipientRepository.countByCampaignGroupByStatus(campaign)
            .associate { (status, count) -> status as MailRecipientStatus to count as Long }
        return MailCampaignSummaryDto.of(campaign, counts)
    }

    private fun getCampaign(campaignId: Long): MailCampaign =
        mailCampaignRepository.findByIdOrNull(campaignId) ?: throw CustomException(ErrorCode.MAIL_CAMPAIGN_NOT_FOUND)
}
