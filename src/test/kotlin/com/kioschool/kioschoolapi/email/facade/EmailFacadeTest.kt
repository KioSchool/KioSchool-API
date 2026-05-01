package com.kioschool.kioschoolapi.email.facade

import com.kioschool.kioschoolapi.domain.email.exception.DuplicatedEmailDomainException
import com.kioschool.kioschoolapi.domain.email.facade.EmailFacade
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl

class EmailFacadeTest : DescribeSpec({
    val emailService = mockk<EmailService>()
    val templateService = mockk<com.kioschool.kioschoolapi.global.template.TemplateService>()

    val sut = EmailFacade(emailService, templateService)

    beforeTest {
        mockkObject(emailService)
        mockkObject(templateService)
    }

    afterTest {
        clearAllMocks()
    }

    describe("getAllEmailDomains") {
        it("should call emailService.getAllEmailDomains") {
            val name = "name"
            val page = 1
            val size = 10

            every {
                emailService.getAllEmailDomains(
                    name,
                    page,
                    size
                )
            } returns PageImpl(emptyList())

            sut.getAllEmailDomains(name, page, size)

            verify { emailService.getAllEmailDomains(name, page, size) }
        }
    }

    describe("registerEmailDomain") {
        it("should call emailService.registerEmailDomain if domain is not duplicated") {
            val name = "name"
            val domain = "domain"

            every { emailService.validateEmailDomainDuplicate(domain) } returns Unit
            every {
                emailService.registerEmailDomain(
                    name,
                    domain
                )
            } returns SampleEntity.emailDomain

            val result = sut.registerEmailDomain(name, domain)

            assert(result.name == SampleEntity.emailDomain.name)

            verify { emailService.validateEmailDomainDuplicate(domain) }
            verify { emailService.registerEmailDomain(name, domain) }
            verify(exactly = 0) { templateService.getEmailDomainAddedEmailTemplate(any(), any()) }
            verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
        }

        it("should send email if domain has @") {
            val name = "name"
            val email = "test@domain.com"
            val domain = "domain.com"
            val template = "template"

            every { emailService.validateEmailDomainDuplicate(domain) } returns Unit
            every {
                emailService.registerEmailDomain(
                    name,
                    domain
                )
            } returns SampleEntity.emailDomain
            every { templateService.getEmailDomainAddedEmailTemplate(name, domain) } returns template
            every { emailService.sendEmail(email, "키오스쿨 이메일 도메인 추가 안내", template) } returns Unit

            val result = sut.registerEmailDomain(name, email)

            assert(result.name == SampleEntity.emailDomain.name)

            verify { emailService.validateEmailDomainDuplicate(domain) }
            verify { emailService.registerEmailDomain(name, domain) }
            verify { templateService.getEmailDomainAddedEmailTemplate(name, domain) }
            verify { emailService.sendEmail(email, "키오스쿨 이메일 도메인 추가 안내", template) }
        }

        it("should throw exception if domain is duplicated") {
            val name = "name"
            val domain = "domain"

            every { emailService.validateEmailDomainDuplicate(domain) } throws DuplicatedEmailDomainException()

            assertThrows<DuplicatedEmailDomainException> {
                sut.registerEmailDomain(name, domain)
            }

            verify { emailService.validateEmailDomainDuplicate(domain) }
            verify(exactly = 0) { emailService.registerEmailDomain(name, domain) }
        }
    }

    describe("deleteEmailDomain") {
        it("should call emailService.deleteEmailDomain") {
            val domainId = 1L

            every {
                emailService.deleteEmailDomain(
                    domainId
                )
            } returns SampleEntity.emailDomain

            val result = sut.deleteEmailDomain(domainId)

            assert(result.name == SampleEntity.emailDomain.name)

            verify { emailService.deleteEmailDomain(domainId) }
        }
    }
})