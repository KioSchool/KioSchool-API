package com.kioschool.kioschoolapi.domain.email.service

import com.kioschool.kioschoolapi.domain.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.domain.email.repository.EmailDomainRepository
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender

class EmailServiceSendTest : DescribeSpec({
    val javaMailSender = mockk<JavaMailSender>()
    val emailCodeRepository = mockk<EmailCodeRepository>()
    val emailDomainRepository = mockk<EmailDomainRepository>()

    val sut = EmailService(
        "noreply@kio-school.com",
        javaMailSender,
        emailCodeRepository,
        emailDomainRepository
    )

    beforeTest {
        every { javaMailSender.createMimeMessage() } returns MimeMessage(null as Session?)
    }

    afterTest { clearAllMocks() }

    describe("sendEmailSync") {
        it("발송에 성공하면 예외를 던지지 않는다") {
            every { javaMailSender.send(any<MimeMessage>()) } just Runs

            shouldNotThrowAny { sut.sendEmailSync("a@b.com", "제목", "<p>본문</p>") }

            verify(exactly = 1) { javaMailSender.send(any<MimeMessage>()) }
        }

        it("발송에 실패하면 EMAIL_SEND_FAILURE를 던진다") {
            every { javaMailSender.send(any<MimeMessage>()) } throws MailSendException("smtp down")

            val ex = shouldThrow<CustomException> { sut.sendEmailSync("a@b.com", "제목", "본문") }

            ex.errorCode shouldBe ErrorCode.EMAIL_SEND_FAILURE
        }
    }

    describe("sendEmail") {
        it("동기 경로에 위임한다") {
            every { javaMailSender.send(any<MimeMessage>()) } just Runs

            sut.sendEmail("a@b.com", "제목", "본문")

            verify(exactly = 1) { javaMailSender.send(any<MimeMessage>()) }
        }
    }
})
