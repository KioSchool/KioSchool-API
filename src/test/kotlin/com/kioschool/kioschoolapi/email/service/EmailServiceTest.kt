package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import com.kioschool.kioschoolapi.domain.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.domain.email.repository.EmailDomainRepository
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDateTime
import java.util.*

class EmailServiceTest : DescribeSpec({
    val javaMailSender = mockk<JavaMailSender>()
    val emailCodeRepository = mockk<EmailCodeRepository>()
    val emailDomainRepository = mockk<EmailDomainRepository>()
    val fromAddress = "test@test.com"

    val sut = EmailService(
        fromAddress,
        javaMailSender,
        emailCodeRepository,
        emailDomainRepository
    )

    beforeTest {
        mockkObject(javaMailSender)
        mockkObject(emailCodeRepository)
        mockkObject(emailDomainRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("createOrUpdateRegisterEmailCode") {
        it("should create register email code if not exists") {
            val emailAddress = "test@test.com"
            val code = "123456"

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.REGISTER
                )
            } returns null
            every { emailCodeRepository.save(any()) } answers { firstArg() }

            val result = sut.createOrUpdateRegisterEmailCode(emailAddress, code)

            assert(result.email == emailAddress)
            assert(result.code == code)
            assert(result.kind == EmailKind.REGISTER)
            assert(result != SampleEntity.emailCode)
        }

        it("should update register email code if exists") {
            val emailAddress = SampleEntity.emailCode.email
            val code = "123456"

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.REGISTER
                )
            } returns SampleEntity.emailCode
            every { emailCodeRepository.save(any()) } answers { firstArg() }

            val result = sut.createOrUpdateRegisterEmailCode(emailAddress, code)

            assert(result.email == emailAddress)
            assert(result.code == code)
            assert(result.kind == EmailKind.REGISTER)
            assert(result == SampleEntity.emailCode)
        }

        it("코드를 다시 발급하면 만료 시각과 시도 횟수를 새로 잡는다") {
            val emailAddress = "reissue@test.com"
            val existing = EmailCode(emailAddress, "111111", kind = EmailKind.REGISTER).apply {
                attemptCount = EmailService.MAX_REGISTER_CODE_ATTEMPTS
                expiresAt = LocalDateTime.now().minusHours(1)
            }

            every { emailCodeRepository.findByEmailAndKind(emailAddress, EmailKind.REGISTER) } returns existing
            every { emailCodeRepository.save(any()) } answers { firstArg() }

            val before = LocalDateTime.now()
            val result = sut.createOrUpdateRegisterEmailCode(emailAddress, "222222")

            assertEquals(0, result.attemptCount)
            val expiresAt = result.expiresAt!!
            assert(!expiresAt.isBefore(before.plus(EmailService.REGISTER_CODE_TTL)))
            assert(!expiresAt.isAfter(LocalDateTime.now().plus(EmailService.REGISTER_CODE_TTL)))
        }
    }

    describe("validateEmailDomain") {
        it("should throw CustomException if email domain is not verified") {
            val emailAddress = "test@test.com"

            every { emailDomainRepository.findByDomain(any()) } returns null

            val ex = assertThrows<CustomException> {
                sut.validateEmailDomainVerified(emailAddress)
            }
            assertEquals(ErrorCode.NOT_VERIFIED_EMAIL_DOMAIN, ex.errorCode)
        }

        it("should not throw CustomException if email domain is verified") {
            val emailAddress = "test@test.com"

            every { emailDomainRepository.findByDomain(any()) } returns SampleEntity.emailDomain

            sut.validateEmailDomainVerified(emailAddress)
        }
    }

    describe("isRegisterEmailVerified") {
        it("should return true if email is verified") {
            val emailAddress = "test@test.com"
            val emailCode = SampleEntity.emailCode.apply { isVerified = true }

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.REGISTER
                )
            } returns emailCode

            val result = sut.isRegisterEmailVerified(emailAddress)

            assert(result)
        }

        it("should return false if email is not verified") {
            val emailAddress = "test@test.com"
            val emailCode = SampleEntity.emailCode.apply { isVerified = false }

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.REGISTER
                )
            } returns emailCode

            val result = sut.isRegisterEmailVerified(emailAddress)

            assert(!result)
        }

        it("should return false if email is not exists") {
            val emailAddress = "test@test.com"

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.REGISTER
                )
            } returns null

            val result = sut.isRegisterEmailVerified(emailAddress)

            assert(!result)
        }
    }

    describe("deleteRegisterCode") {
        it("should call deleteByEmailAndKind") {
            val email = "test@test.com"

            every {
                emailCodeRepository.deleteByEmailAndKind(
                    email,
                    EmailKind.REGISTER
                )
            } returns Unit

            sut.deleteRegisterCode(email)

            verify { emailCodeRepository.deleteByEmailAndKind(email, EmailKind.REGISTER) }
        }
    }

    describe("generateRegisterCode") {
        it("should return 6 digit random number") {
            val result = sut.generateRegisterCode()

            assert(result.length == 6)
            assert(result.toIntOrNull() != null)
        }
    }

    describe("generateResetPasswordCode") {
        it("should return UUID") {
            val result = sut.generateResetPasswordCode()

            assert(result.length == 36)
        }
    }

    describe("verifyRegisterCode") {
        val email = "verify@test.com"
        val code = "123456"

        fun issuedCode(
            expiresAt: LocalDateTime? = LocalDateTime.now().plusMinutes(5),
            attemptCount: Int = 0
        ) = EmailCode(email, code, kind = EmailKind.REGISTER).apply {
            this.expiresAt = expiresAt
            this.attemptCount = attemptCount
        }

        fun givenCode(emailCode: EmailCode?) {
            every { emailCodeRepository.findLockedByEmailAndKind(email, EmailKind.REGISTER) } returns emailCode
            every { emailCodeRepository.save(any()) } answers { firstArg() }
        }

        it("맞는 코드면 인증하고 시도 1회로 센다") {
            val emailCode = issuedCode()
            givenCode(emailCode)

            val result = sut.verifyRegisterCode(email, code)

            assertEquals(true, result)
            assertEquals(true, emailCode.isVerified)
            assertEquals(1, emailCode.attemptCount)
            verify { emailCodeRepository.save(emailCode) }
        }

        it("틀린 코드면 false를 돌려주고 시도 횟수를 저장한다") {
            val emailCode = issuedCode(attemptCount = 2)
            givenCode(emailCode)

            val result = sut.verifyRegisterCode(email, "000000")

            assertEquals(false, result)
            assertEquals(false, emailCode.isVerified)
            assertEquals(3, emailCode.attemptCount)
            verify { emailCodeRepository.save(emailCode) }
        }

        it("시도 횟수를 다 쓰면 맞는 코드도 받지 않는다") {
            val emailCode = issuedCode(attemptCount = EmailService.MAX_REGISTER_CODE_ATTEMPTS)
            givenCode(emailCode)

            val ex = assertThrows<CustomException> { sut.verifyRegisterCode(email, code) }

            assertEquals(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED, ex.errorCode)
            assertEquals(false, emailCode.isVerified)
        }

        it("마지막 남은 시도에서는 맞는 코드를 받는다") {
            val emailCode = issuedCode(attemptCount = EmailService.MAX_REGISTER_CODE_ATTEMPTS - 1)
            givenCode(emailCode)

            assertEquals(true, sut.verifyRegisterCode(email, code))
        }

        it("만료된 코드는 맞아도 받지 않고 시도로 세지 않는다") {
            val emailCode = issuedCode(expiresAt = LocalDateTime.now().minusSeconds(1))
            givenCode(emailCode)

            val ex = assertThrows<CustomException> { sut.verifyRegisterCode(email, code) }

            assertEquals(ErrorCode.EMAIL_CODE_EXPIRED, ex.errorCode)
            assertEquals(false, emailCode.isVerified)
            assertEquals(0, emailCode.attemptCount)
        }

        it("만료 시각이 없는 코드(배포 전 발급분)는 만료로 본다") {
            givenCode(issuedCode(expiresAt = null))

            val ex = assertThrows<CustomException> { sut.verifyRegisterCode(email, code) }

            assertEquals(ErrorCode.EMAIL_CODE_EXPIRED, ex.errorCode)
        }

        it("발급한 코드가 없으면 false") {
            givenCode(null)

            assertEquals(false, sut.verifyRegisterCode(email, code))
            verify(exactly = 0) { emailCodeRepository.save(any()) }
        }
    }

    describe("getEmailByCode") {
        it("should return email if emailCode exists") {
            val emailCode = SampleEntity.emailCode
            val code = "123456"

            every {
                emailCodeRepository.findByCodeAndKind(
                    code,
                    EmailKind.RESET_PASSWORD
                )
            } returns emailCode

            val result = sut.getEmailByCode(code)

            assert(result == emailCode.email)
        }

        it("should throw CustomException(USER_NOT_FOUND) if emailCode is not exists") {
            val code = "123456"

            every {
                emailCodeRepository.findByCodeAndKind(
                    code,
                    EmailKind.RESET_PASSWORD
                )
            } returns null

            val ex = assertThrows<CustomException> {
                sut.getEmailByCode(code)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
        }
    }

    describe("createOrUpdateResetPasswordEmailCode") {
        it("should create reset password email code if not exists") {
            val emailAddress = "test@test.com"
            val code = "123456"

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.RESET_PASSWORD
                )
            } returns null
            every { emailCodeRepository.save(any()) } answers { firstArg() }

            val result = sut.createOrUpdateResetPasswordEmailCode(emailAddress, code)

            assert(result.email == emailAddress)
            assert(result.code == code)
            assert(result.kind == EmailKind.RESET_PASSWORD)
            assert(result != SampleEntity.emailCode)

        }

        it("should update reset password email code if exists") {
            val emailCode = SampleEntity.emailCode
            val emailAddress = emailCode.email
            val code = "123456"

            every {
                emailCodeRepository.findByEmailAndKind(
                    emailAddress,
                    EmailKind.RESET_PASSWORD
                )
            } returns emailCode
            every { emailCodeRepository.save(any()) } answers { firstArg() }

            val result = sut.createOrUpdateResetPasswordEmailCode(emailAddress, code)

            assert(result.email == emailAddress)
            assert(result.code == code)
            assert(result == emailCode)
        }
    }

    describe("deleteResetPasswordCode") {
        it("should call delete if emailCode exists") {
            val code = "123456"

            every {
                emailCodeRepository.findByCodeAndKind(
                    code,
                    EmailKind.RESET_PASSWORD
                )
            } returns SampleEntity.emailCode
            every {
                emailCodeRepository.delete(SampleEntity.emailCode)
            } returns Unit

            sut.deleteResetPasswordCode(code)

            verify { emailCodeRepository.delete(SampleEntity.emailCode) }
        }

        it("should not call delete if emailCode is not exists") {
            val code = "123456"

            every {
                emailCodeRepository.findByCodeAndKind(
                    code,
                    EmailKind.RESET_PASSWORD
                )
            } returns null

            sut.deleteResetPasswordCode(code)

            verify(exactly = 0) { emailCodeRepository.delete(any()) }
        }
    }

    describe("getAllEmailDomains") {
        it("should return all email domains if name is null or blank") {
            val name = null
            val page = 0
            val size = 10

            every { emailDomainRepository.findAll(any<PageRequest>()) } returns PageImpl(
                listOf(
                    SampleEntity.emailDomain
                )
            )

            val result = sut.getAllEmailDomains(name, page, size)

            assert(result.content == listOf(SampleEntity.emailDomain))

            verify { emailDomainRepository.findAll(any<PageRequest>()) }
            verify(exactly = 0) {
                emailDomainRepository.findByNameContains(
                    any(),
                    any<PageRequest>()
                )
            }
        }

        it("should return email domains if name is not null or blank") {
            val name = "test"
            val page = 0
            val size = 10

            every {
                emailDomainRepository.findByNameContains(
                    name,
                    any<PageRequest>()
                )
            } returns PageImpl(listOf(SampleEntity.emailDomain))

            val result = sut.getAllEmailDomains(name, page, size)

            assert(result.content == listOf(SampleEntity.emailDomain))

            verify { emailDomainRepository.findByNameContains(name, any<PageRequest>()) }
            verify(exactly = 0) { emailDomainRepository.findAll(any<PageRequest>()) }
        }
    }

    describe("validateEmailDomainDuplicate") {
        it("should not throw CustomException if email domain is not duplicated") {
            val domain = SampleEntity.emailDomain.domain

            every { emailDomainRepository.findByDomain(domain) } returns null

            sut.validateEmailDomainDuplicate(domain)
        }

        it("should throw CustomException if email domain is duplicated") {
            val domain = SampleEntity.emailDomain.domain

            every { emailDomainRepository.findByDomain(domain) } returns SampleEntity.emailDomain

            val ex = assertThrows<CustomException> {
                sut.validateEmailDomainDuplicate(domain)
            }
            assertEquals(ErrorCode.DUPLICATE_EMAIL_DOMAIN, ex.errorCode)
        }
    }

    describe("registerEmailDomain") {
        it("should return email domain") {
            val name = SampleEntity.emailDomain.name
            val domain = SampleEntity.emailDomain.domain

            every { emailDomainRepository.save(any()) } answers { firstArg() }

            val result = sut.registerEmailDomain(name, domain)

            assert(result.name == name)
            assert(result.domain == domain)
        }
    }

    describe("deleteEmailDomain") {
        it("should return email domain") {
            val domainId = SampleEntity.emailDomain.id

            every { emailDomainRepository.findById(domainId) } returns Optional.of(SampleEntity.emailDomain)
            every { emailDomainRepository.delete(SampleEntity.emailDomain) } returns Unit

            val result = sut.deleteEmailDomain(domainId)

            assert(result == SampleEntity.emailDomain)

            verify { emailDomainRepository.findById(domainId) }
            verify { emailDomainRepository.delete(SampleEntity.emailDomain) }
        }

        it("should throw NoSuchElementException if email domain is not exists") {
            val domainId = SampleEntity.emailDomain.id

            every { emailDomainRepository.findById(domainId) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                sut.deleteEmailDomain(domainId)
            }
        }
    }

    describe("sendEmail") {
        it("should call javaMailSender.send") {
            val address = "test@test.com"
            val subject = "subject"
            val text = "text"

            every { javaMailSender.createMimeMessage() } returns mockk(relaxed = true)
            every { javaMailSender.send(any<MimeMessage>()) } returns Unit

            sut.sendEmail(address, subject, text)

            verify { javaMailSender.send(any<MimeMessage>()) }
        }

        it("should throw CustomException when javaMailSender.send fails") {
            val address = "test@test.com"
            val subject = "subject"
            val text = "text"

            every { javaMailSender.createMimeMessage() } returns mockk(relaxed = true)
            every { javaMailSender.send(any<MimeMessage>()) } throws Exception()

            val ex = assertThrows<CustomException> {
                sut.sendEmail(address, subject, text)
            }
            assertEquals(ErrorCode.EMAIL_SEND_FAILURE, ex.errorCode)
        }
    }
})