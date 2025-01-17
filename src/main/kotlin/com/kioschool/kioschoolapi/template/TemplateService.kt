package com.kioschool.kioschoolapi.template

import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class TemplateService(
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
        }
        
        return templateEngine.process("resetPasswordEmail", context)
    }
}