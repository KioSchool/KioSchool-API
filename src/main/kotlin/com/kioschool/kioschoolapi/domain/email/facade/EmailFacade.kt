package com.kioschool.kioschoolapi.domain.email.facade

import com.kioschool.kioschoolapi.domain.email.dto.common.EmailDomainDto
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.global.template.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmailFacade(
    private val emailService: EmailService,
    private val templateService: TemplateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

        log.info(
            "[AUDIT] action=REGISTER_EMAIL_DOMAIN domainId={} name={} domain={}",
            registeredDomain.id,
            name,
            extractedDomain
        )
        return EmailDomainDto.of(registeredDomain)
    }

    fun deleteEmailDomain(domainId: Long): EmailDomainDto {
        val deleted = emailService.deleteEmailDomain(domainId)
        log.info("[AUDIT] action=DELETE_EMAIL_DOMAIN domainId={} name={} domain={}", domainId, deleted.name, deleted.domain)
        return EmailDomainDto.of(deleted)
    }
}