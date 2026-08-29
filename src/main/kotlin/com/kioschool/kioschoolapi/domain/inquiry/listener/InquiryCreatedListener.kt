package com.kioschool.kioschoolapi.domain.inquiry.listener

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.event.InquiryCreatedEvent
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.template.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 접수 커밋 이후의 부수 작업. 여기서 무엇이 실패해도 이미 반환된 201에는 영향이 없고,
 * 고객이 문의를 다시 접수할 필요도 없다.
 */
@Component
class InquiryCreatedListener(
    private val inquiryRepository: InquiryRepository,
    private val discordService: DiscordService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("taskExecutor")
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: InquiryCreatedEvent) {
        val inquiry = inquiryRepository.findByIdOrNull(event.inquiryId) ?: return

        runCatching { discordService.sendInquiryCreated(inquiry) }
            .onFailure { log.error("Discord notification failed for inquiryId={}", inquiry.id, it) }

        runCatching {
            val template = templateService.getInquiryReceivedEmailTemplate(inquiry.id, inquiry.title)
            emailService.sendEmail(inquiry.replyEmail, RECEIVED_SUBJECT, template)
        }.onFailure { log.error("Received-confirmation email failed for inquiryId={}", inquiry.id, it) }
    }

    companion object {
        private const val RECEIVED_SUBJECT = "[키오스쿨] 문의가 접수되었습니다"
    }
}
