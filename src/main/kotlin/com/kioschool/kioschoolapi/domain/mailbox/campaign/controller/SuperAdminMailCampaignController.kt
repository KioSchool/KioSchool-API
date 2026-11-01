package com.kioschool.kioschoolapi.domain.mailbox.campaign.controller

import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudiencePreview
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignDetailDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignSummaryDto
import com.kioschool.kioschoolapi.domain.mailbox.campaign.facade.MailCampaignFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Mail Campaign Controller")
@RestController
@RequestMapping("/super-admin/mail/campaigns")
class SuperAdminMailCampaignController(
    private val mailCampaignFacade: MailCampaignFacade,
) {
    @Operation(summary = "받을 주점 미리보기", description = "조건에 맞는 주점과 제외 사유별 수를 조회합니다.")
    @PostMapping("/audience")
    fun previewAudience(
        @RequestBody @Valid filter: MailAudienceFilter,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): MailAudiencePreview {
        return mailCampaignFacade.preview(filter, page, size)
    }

    @Operation(summary = "나에게 테스트 발송", description = "첫 주점의 값으로 본인 계정 이메일에 보냅니다.")
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun test(
        @AdminUsername username: String,
        @RequestBody @Valid body: MailCampaignRequestBody,
    ) {
        mailCampaignFacade.test(username, body)
    }

    @Operation(summary = "발송 시작", description = "대상을 고정하고 하루 한도만큼 나눠 보냅니다.")
    @PostMapping
    fun start(
        @AdminUsername username: String,
        @RequestBody @Valid body: MailCampaignRequestBody,
    ): MailCampaignSummaryDto {
        return mailCampaignFacade.start(username, body)
    }

    @Operation(summary = "지난 발송 목록")
    @GetMapping
    fun getCampaigns(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<MailCampaignSummaryDto> {
        return mailCampaignFacade.getCampaigns(page, size)
    }

    @Operation(summary = "발송 현황")
    @GetMapping("/{campaignId}")
    fun getCampaign(
        @PathVariable campaignId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): MailCampaignDetailDto {
        return mailCampaignFacade.getCampaign(campaignId, page, size)
    }

    @Operation(summary = "발송 멈추기")
    @PostMapping("/{campaignId}/stop")
    fun stop(
        @AdminUsername username: String,
        @PathVariable campaignId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): MailCampaignDetailDto {
        return mailCampaignFacade.stop(username, campaignId, page, size)
    }

    @Operation(summary = "실패한 곳 다시 보내기")
    @PostMapping("/{campaignId}/retry-failed")
    fun retryFailed(
        @PathVariable campaignId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): MailCampaignDetailDto {
        return mailCampaignFacade.retryFailed(campaignId, page, size)
    }
}
