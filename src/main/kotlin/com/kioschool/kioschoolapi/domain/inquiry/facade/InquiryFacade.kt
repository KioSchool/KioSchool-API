package com.kioschool.kioschoolapi.domain.inquiry.facade

import com.kioschool.kioschoolapi.domain.inquiry.dto.common.CreateInquiryResponse
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquiryDetailDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquiryImageDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquirySummaryDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.request.CreateInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.event.InquiryCreatedEvent
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryEmailGuard
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryService
import com.kioschool.kioschoolapi.domain.inquiry.util.InquiryImageValidator
import com.kioschool.kioschoolapi.global.template.TemplateService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Component
class InquiryFacade(
    private val inquiryService: InquiryService,
    private val inquiryEmailGuard: InquiryEmailGuard,
    private val templateService: TemplateService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun createInquiry(
        body: CreateInquiryRequestBody,
        imageFiles: List<MultipartFile>?,
    ): CreateInquiryResponse {
        val replyEmail = body.normalizedReplyEmail()
        val images = InquiryImageValidator.validate(imageFiles ?: emptyList())

        inquiryEmailGuard.check(replyEmail)

        val inquiry = inquiryService.createInquiry(
            title = body.normalizedTitle(),
            content = body.normalizedContent(),
            replyEmail = replyEmail,
            images = images,
        )

        eventPublisher.publishEvent(InquiryCreatedEvent(inquiry.id))

        return CreateInquiryResponse.of(inquiry)
    }

    fun getInquiries(status: InquiryStatus?, page: Int, size: Int): Page<InquirySummaryDto> =
        inquiryService.getInquiries(status, page, size).map { InquirySummaryDto.of(it) }

    fun getInquiry(inquiryId: Long): InquiryDetailDto {
        val inquiry = inquiryService.getInquiry(inquiryId)
        val images = inquiry.images.map {
            InquiryImageDto.of(it, inquiryService.getImageAccessUrl(it.storageKey))
        }

        return InquiryDetailDto.of(inquiry, images)
    }
}
