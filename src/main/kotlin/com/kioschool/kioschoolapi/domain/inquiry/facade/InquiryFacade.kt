package com.kioschool.kioschoolapi.domain.inquiry.facade

import com.kioschool.kioschoolapi.domain.inquiry.dto.common.CreateInquiryResponse
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquiryDetailDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquiryImageDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquirySummaryDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.request.CreateInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.dto.request.ReplyInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
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

    fun getInquiry(inquiryId: Long): InquiryDetailDto = toDetailDto(inquiryService.getInquiry(inquiryId))

    fun replyToInquiry(
        username: String,
        inquiryId: Long,
        body: ReplyInquiryRequestBody,
    ): InquiryDetailDto {
        val content = body.normalizedContent()
        // 이스케이프는 템플릿의 th:text가 처리한다. 여기서 직접 escape하거나 <br>을 만들지 않는다.
        val emailBody = templateService.getInquiryReplyEmailTemplate(content)

        val inquiry = inquiryService.replyToInquiry(
            username = username,
            inquiryId = inquiryId,
            subject = body.normalizedSubject(),
            content = content,
            emailBody = emailBody,
        )

        return toDetailDto(inquiry)
    }

    private fun toDetailDto(inquiry: Inquiry): InquiryDetailDto {
        val images = inquiry.images.map {
            InquiryImageDto.of(it, inquiryService.getImageAccessUrl(it.storageKey))
        }

        return InquiryDetailDto.of(inquiry, images)
    }
}
