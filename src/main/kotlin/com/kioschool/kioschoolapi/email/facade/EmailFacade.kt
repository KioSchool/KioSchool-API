package com.kioschool.kioschoolapi.email.facade

import com.kioschool.kioschoolapi.email.entity.EmailDomain
import com.kioschool.kioschoolapi.email.exception.DuplicatedEmailDomainException
import com.kioschool.kioschoolapi.email.service.EmailService
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService
) {
    fun getAllEmailDomains(name: String?, page: Int, size: Int) =
        emailService.getAllEmailDomains(name, page, size)

    fun registerEmailDomain(name: String, domain: String): EmailDomain {
        if (emailService.isEmailDomainDuplicate(domain)) throw DuplicatedEmailDomainException()

        return emailService.registerEmailDomain(name, domain)
    }

    fun removeEmailDomain(domainId: Long) = emailService.removeEmailDomain(domainId)
}