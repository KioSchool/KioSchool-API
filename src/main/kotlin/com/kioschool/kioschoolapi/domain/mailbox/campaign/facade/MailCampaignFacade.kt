package com.kioschool.kioschoolapi.domain.mailbox.campaign.facade

import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudiencePreview
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignDetailDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignSummaryDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailAudienceService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignComposer
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignContent
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignSender
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailTemplateRenderer
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailTemplateValues
import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeService
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.resend.service.MailRecipientGuard
import com.kioschool.kioschoolapi.global.resend.service.ResendDailyLimitException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MailCampaignFacade(
    private val mailCampaignService: MailCampaignService,
    private val mailAudienceService: MailAudienceService,
    private val mailTemplateRenderer: MailTemplateRenderer,
    private val mailCampaignComposer: MailCampaignComposer,
    private val mailCampaignSender: MailCampaignSender,
    private val userService: UserService,
    private val mailUnsubscribeService: MailUnsubscribeService,
    private val resendInboxClient: ResendInboxClient,
    private val mailRecipientGuard: MailRecipientGuard,
    @Value("\${mail.campaign.daily-limit}")
    private val dailyLimit: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun preview(filter: MailAudienceFilter, page: Int, size: Int): MailAudiencePreview =
        mailAudienceService.preview(filter, page, size)

    fun test(username: String, body: MailCampaignRequestBody) {
        validate(body)

        val user = userService.getUser(username)
        val email = user.email
        val first = mailAudienceService.resolve(body.filter).firstOrNull()
        if (email.isNullOrBlank() || first == null || !mailRecipientGuard.isAllowed(email)) {
            throw CustomException(ErrorCode.MAIL_RECIPIENT_UNAVAILABLE)
        }

        val mail = mailCampaignComposer.compose(
            contentOf(body),
            MailTemplateValues(first.workspaceName, first.ownerName, first.workspaceId),
            mailUnsubscribeService.tokenFor(user),
        )

        try {
            resendInboxClient.sendNew(
                email,
                "$TEST_PREFIX${mail.subject}",
                mail.html,
                mail.text,
                mail.headers,
                "mail-campaign-test-${UUID.randomUUID()}",
            )
        } catch (e: ResendDailyLimitException) {
            throw CustomException(ErrorCode.MAIL_DAILY_LIMIT_EXCEEDED, cause = e)
        } catch (e: ResendSendException) {
            log.error("Mail campaign test failed: {}", e.message)
            throw CustomException(ErrorCode.MAIL_SEND_FAILURE, cause = e)
        }
    }

    fun start(username: String, body: MailCampaignRequestBody): MailCampaignSummaryDto {
        validate(body)

        body.requestId?.let { requestId ->
            mailCampaignService.findSummaryByRequestId(requestId)?.let { return it }
        }

        val rows = mailAudienceService.resolve(body.filter)
        if (rows.isEmpty()) throw CustomException(ErrorCode.MAIL_RECIPIENT_UNAVAILABLE)
        if (body.expectedCount != rows.size) throw CustomException(ErrorCode.MAIL_AUDIENCE_CHANGED)

        val creator = userService.getUser(username)
        val campaign = try {
            mailCampaignService.create(body, creator, rows)
        } catch (e: DataIntegrityViolationException) {
            return body.requestId?.let { mailCampaignService.findSummaryByRequestId(it) } ?: throw e
        }
        log.info("[AUDIT] action=START_MAIL_CAMPAIGN campaignId={} by={} targetCount={}", campaign.id, username, rows.size)
        mailCampaignSender.sendDueAsync()
        return mailCampaignService.getSummary(campaign.id)
    }

    fun getCampaigns(page: Int, size: Int): Page<MailCampaignSummaryDto> = mailCampaignService.getSummaries(page, size)

    fun getCampaign(campaignId: Long, page: Int, size: Int): MailCampaignDetailDto =
        MailCampaignDetailDto(
            summary = mailCampaignService.getSummary(campaignId),
            dailyLimit = dailyLimit,
            recipients = mailCampaignService.getRecipients(campaignId, page, size),
        )

    fun stop(username: String, campaignId: Long, page: Int, size: Int): MailCampaignDetailDto {
        mailCampaignService.stop(campaignId)
        log.info("[AUDIT] action=STOP_MAIL_CAMPAIGN campaignId={} by={}", campaignId, username)
        return getCampaign(campaignId, page, size)
    }

    fun retryFailed(campaignId: Long, page: Int, size: Int): MailCampaignDetailDto {
        if (mailCampaignService.retryFailed(campaignId)) mailCampaignSender.sendDueAsync()
        return getCampaign(campaignId, page, size)
    }

    private fun validate(body: MailCampaignRequestBody) {
        mailTemplateRenderer.validate(
            body.normalizedSubject(),
            body.normalizedContent(),
            body.normalizedButtonLabel(),
            body.normalizedButtonUrl(),
        )
    }

    private fun contentOf(body: MailCampaignRequestBody) = MailCampaignContent(
        subject = body.normalizedSubject(),
        bodyTemplate = body.normalizedContent(),
        buttonLabel = body.normalizedButtonLabel(),
        buttonUrlTemplate = body.normalizedButtonUrl(),
    )

    companion object {
        private const val TEST_PREFIX = "[테스트] "
    }
}
