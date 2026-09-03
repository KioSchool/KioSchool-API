package com.kioschool.kioschoolapi.global.template

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class TemplateService(
    @Value("\${kioschool.base-url}")
    private val baseUrl: String,
    private val templateEngine: SpringTemplateEngine
) {
    fun getRegisterEmailTemplate(code: String): String {
        val context = Context().apply {
            setVariable("code", code)
        }

        return templateEngine.process("registerEmail", context)
    }

    fun getResetPasswordEmailTemplate(code: String): String {
        val context = Context().apply {
            setVariable("code", code)
            setVariable("baseUrl", baseUrl)
        }

        return templateEngine.process("resetPasswordEmail", context)
    }

    fun getEmailDomainAddedEmailTemplate(name: String, domain: String): String {
        val context = Context().apply {
            setVariable("name", name)
            setVariable("domain", domain)
        }

        return templateEngine.process("emailDomainAddedEmail", context)
    }

    fun getInquiryReceivedEmailTemplate(inquiryId: Long, title: String): String {
        val context = Context().apply {
            setVariable("inquiryId", inquiryId)
            setVariable("title", title)
        }

        return templateEngine.process("inquiryReceivedEmail", context)
    }

    fun getInquiryReplyEmailTemplate(content: String, inquiryTitle: String, inquiryContent: String): String {
        val context = Context().apply {
            setVariable("content", content)
            setVariable("inquiryTitle", inquiryTitle)
            setVariable("inquiryContent", inquiryContent)
            setVariable("baseUrl", baseUrl)
        }

        return templateEngine.process("inquiryReplyEmail", context)
    }
}