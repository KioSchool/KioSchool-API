package com.kioschool.kioschoolapi.template

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
}