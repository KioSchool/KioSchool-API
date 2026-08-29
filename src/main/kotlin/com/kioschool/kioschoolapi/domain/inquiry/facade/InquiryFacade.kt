package com.kioschool.kioschoolapi.domain.inquiry.facade

import com.kioschool.kioschoolapi.domain.inquiry.dto.common.CreateInquiryResponse
import com.kioschool.kioschoolapi.domain.inquiry.dto.request.CreateInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.event.InquiryCreatedEvent
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryEmailGuard
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryService
import com.kioschool.kioschoolapi.domain.inquiry.util.InquiryImageValidator
import com.kioschool.kioschoolapi.global.template.TemplateService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class InquiryFacade(
    private val inquiryService: InquiryService,
    private val inquiryEmailGuard: InquiryEmailGuard,
    private val templateService: TemplateService,
    private val eventPublisher: ApplicationEventPublisher,
) {
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
}
