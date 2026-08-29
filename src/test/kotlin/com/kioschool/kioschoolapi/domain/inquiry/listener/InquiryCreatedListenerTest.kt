package com.kioschool.kioschoolapi.domain.inquiry.listener

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.event.InquiryCreatedEvent
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.template.TemplateService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import java.util.*

class InquiryCreatedListenerTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val discordService = mockk<DiscordService>()
    val emailService = mockk<EmailService>()
    val templateService = mockk<TemplateService>()

    val sut = InquiryCreatedListener(inquiryRepository, discordService, emailService, templateService)

    afterTest { clearAllMocks() }

    describe("on") {
        it("Discord 알림과 접수 확인 메일을 보낸다") {
            val inquiry = SampleEntity.newInquiry(id = 5L, replyEmail = "a@b.com")
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { discordService.sendInquiryCreated(inquiry) } just Runs
            every { templateService.getInquiryReceivedEmailTemplate(5L, inquiry.title) } returns "html"
            every { emailService.sendEmailSync("a@b.com", any(), "html") } just Runs

            sut.on(InquiryCreatedEvent(5L))

            verify(exactly = 1) { discordService.sendInquiryCreated(inquiry) }
            verify(exactly = 1) { emailService.sendEmailSync("a@b.com", any(), "html") }
        }

        it("Discord가 실패해도 접수 확인 메일은 보낸다") {
            val inquiry = SampleEntity.newInquiry(id = 5L, replyEmail = "a@b.com")
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { discordService.sendInquiryCreated(any()) } throws RuntimeException("discord down")
            every { templateService.getInquiryReceivedEmailTemplate(any(), any()) } returns "html"
            every { emailService.sendEmailSync(any(), any(), any()) } just Runs

            shouldNotThrowAny { sut.on(InquiryCreatedEvent(5L)) }

            verify(exactly = 1) { emailService.sendEmailSync("a@b.com", any(), "html") }
        }

        it("메일 발송이 실패해도 예외를 밖으로 던지지 않는다") {
            val inquiry = SampleEntity.newInquiry(id = 5L, replyEmail = "a@b.com")
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { discordService.sendInquiryCreated(any()) } just Runs
            every { templateService.getInquiryReceivedEmailTemplate(any(), any()) } returns "html"
            every { emailService.sendEmailSync(any(), any(), any()) } throws RuntimeException("smtp down")

            shouldNotThrowAny { sut.on(InquiryCreatedEvent(5L)) }
        }

        it("문의가 이미 사라졌으면 아무것도 하지 않는다") {
            every { inquiryRepository.findById(5L) } returns Optional.empty()

            shouldNotThrowAny { sut.on(InquiryCreatedEvent(5L)) }

            verify(exactly = 0) { discordService.sendInquiryCreated(any()) }
        }
    }
})
