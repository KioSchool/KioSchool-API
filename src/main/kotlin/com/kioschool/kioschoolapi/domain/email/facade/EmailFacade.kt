package com.kioschool.kioschoolapi.domain.email.facade

import com.kioschool.kioschoolapi.domain.email.dto.common.EmailDomainDto
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService
) {
    fun getAllEmailDomains(name: String?, page: Int, size: Int) =
        emailService.getAllEmailDomains(name, page, size).map { EmailDomainDto.of(it) }

    fun registerEmailDomain(name: String, domain: String): EmailDomainDto {
        emailService.validateEmailDomainDuplicate(domain)

        return EmailDomainDto.of(emailService.registerEmailDomain(name, domain))
    }

    fun deleteEmailDomain(domainId: Long) =
        EmailDomainDto.of(emailService.deleteEmailDomain(domainId))
}