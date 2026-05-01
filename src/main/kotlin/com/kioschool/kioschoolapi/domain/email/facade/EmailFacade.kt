package com.kioschool.kioschoolapi.domain.email.facade

import com.kioschool.kioschoolapi.domain.email.dto.common.EmailDomainDto
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.global.template.TemplateService
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService,
    private val templateService: TemplateService
) {
    fun getAllEmailDomains(name: String?, page: Int, size: Int) =
        emailService.getAllEmailDomains(name, page, size).map { EmailDomainDto.of(it) }

    fun registerEmailDomain(name: String, domain: String): EmailDomainDto {
        val extractedDomain = if (domain.contains("@")) domain.split("@").last() else domain

        emailService.validateEmailDomainDuplicate(extractedDomain)
        val registeredDomain = emailService.registerEmailDomain(name, extractedDomain)

        if (domain.contains("@")) {
            val template = templateService.getEmailDomainAddedEmailTemplate(name, extractedDomain)
            emailService.sendEmail(
                address = domain,
                subject = "키오스쿨 이메일 도메인 추가 안내",
                text = template
            )
        }

        return EmailDomainDto.of(registeredDomain)
    }

    fun deleteEmailDomain(domainId: Long) =
        EmailDomainDto.of(emailService.deleteEmailDomain(domainId))
}