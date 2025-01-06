package com.kioschool.kioschoolapi.email.facade

import com.kioschool.kioschoolapi.email.service.EmailService
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService
) {
    fun getAllEmailDomains(name: String?, page: Int, size: Int) =
        emailService.getAllEmailDomains(name, page, size)

    fun registerEmailDomain(name: String, domain: String) =
        emailService.registerEmailDomain(name, domain)

    fun removeEmailDomain(domainId: Long) = emailService.removeEmailDomain(domainId)
}