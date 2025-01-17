package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.email.enum.EmailKind
import com.kioschool.kioschoolapi.email.exception.DuplicatedEmailDomainException
import com.kioschool.kioschoolapi.email.exception.NotVerifiedEmailDomainException
import com.kioschool.kioschoolapi.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.email.repository.EmailDomainRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mail.javamail.JavaMailSender
import java.util.*

class EmailServiceTest : DescribeSpec({
    val javaMailSender = mockk<JavaMailSender>()
    val emailCodeRepository = mockk<EmailCodeRepository>()
    val emailDomainRepository = mockk<EmailDomainRepository>()

    val sut = EmailService(
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
    }

    describe("validateEmailDomain") {
        it("should throw NotVerifiedEmailDomainException if email domain is not verified") {
            val emailAddress = "test@test.com"

            every { emailDomainRepository.findByDomain(any()) } returns null

            assertThrows<NotVerifiedEmailDomainException> {
                sut.validateEmailDomainVerified(emailAddress)
            }
        }

        it("should not throw NotVerifiedEmailDomainException if email domain is verified") {
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
        it("should return 50 length random string") {
            val result = sut.generateResetPasswordCode()

            assert(result.length == 50)
            assert(!result.contains(Regex("[^a-zA-Z0-9]")))
        }
    }

    describe("verifyRegisterCode") {
        it("should return true if emailCode's code is same with input code") {
            val emailCode = SampleEntity.emailCode.apply { isVerified = false }
            val email = SampleEntity.emailCode.email
            val code = SampleEntity.emailCode.code

            every {
                emailCodeRepository.findByEmailAndKind(
                    email,
                    EmailKind.REGISTER
                )
            } returns SampleEntity.emailCode
            every { emailCodeRepository.save(emailCode) } answers { firstArg() }

            val result = sut.verifyRegisterCode(email, code)

            assert(result)
            assert(emailCode.isVerified)

            verify { emailCodeRepository.save(emailCode) }
        }

        it("should return false if emailCode's code is not same with input code") {
            val emailCode = SampleEntity.emailCode.apply { isVerified = false }
            val email = SampleEntity.emailCode.email
            val code = "different code"

            every {
                emailCodeRepository.findByEmailAndKind(
                    email,
                    EmailKind.REGISTER
                )
            } returns SampleEntity.emailCode

            val result = sut.verifyRegisterCode(email, code)

            assert(!result)
            assert(!emailCode.isVerified)

            verify(exactly = 0) { emailCodeRepository.save(emailCode) }
        }

        it("should return false if emailCode is not exists") {
            val email = "test@test.com"
            val code = "123456"

            every {
                emailCodeRepository.findByEmailAndKind(
                    email,
                    EmailKind.REGISTER
                )
            } returns null

            val result = sut.verifyRegisterCode(email, code)

            assert(!result)

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

        it("should throw UserNotFoundException if emailCode is not exists") {
            val code = "123456"

            every {
                emailCodeRepository.findByCodeAndKind(
                    code,
                    EmailKind.RESET_PASSWORD
                )
            } returns null

            assertThrows<UserNotFoundException> {
                sut.getEmailByCode(code)
            }
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
        it("should not throw DuplicatedEmailDomainException if email domain is not duplicated") {
            val domain = SampleEntity.emailDomain.domain

            every { emailDomainRepository.findByDomain(domain) } returns null

            sut.validateEmailDomainDuplicate(domain)
        }

        it("should throw DuplicatedEmailDomainException if email domain is duplicated") {
            val domain = SampleEntity.emailDomain.domain

            every { emailDomainRepository.findByDomain(domain) } returns SampleEntity.emailDomain

            assertThrows<DuplicatedEmailDomainException> {
                sut.validateEmailDomainDuplicate(domain)
            }
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
})