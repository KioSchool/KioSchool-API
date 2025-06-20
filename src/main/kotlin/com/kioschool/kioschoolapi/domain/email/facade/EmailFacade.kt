package com.kioschool.kioschoolapi.domain.email.facade

import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService
) {
    fun getAllEmailDomains(name: String?, page: Int, size: Int) =
        emailService.getAllEmailDomains(name, page, size)

    fun registerEmailDomain(name: String, domain: String): EmailDomain {
        emailService.validateEmailDomainDuplicate(domain)

        return emailService.registerEmailDomain(name, domain)
    }

    fun deleteEmailDomain(domainId: Long) = emailService.deleteEmailDomain(domainId)
}