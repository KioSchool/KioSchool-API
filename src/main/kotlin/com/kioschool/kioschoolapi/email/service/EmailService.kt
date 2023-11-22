package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.email.entity.EmailCode
import com.kioschool.kioschoolapi.email.repository.EmailCodeRepository
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    private val emailCodeRepository: EmailCodeRepository
) {
    fun sendRegisterCodeEmail(address: String) {
        val code = generateEmailCode()
        sendEmail(
            address,
            "키오스쿨 회원가입 인증 코드",
            registerCodeEmailText(code)
        )

        val emailCode = emailCodeRepository.findByEmail(address) ?: EmailCode(address, code)
        emailCode.code = code
        emailCodeRepository.save(emailCode)
    }

    fun sendEmail(address: String, subject: String, text: String) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setTo(address)
        helper.setSubject(subject)
        helper.setText(text, true)
        javaMailSender.send(message)
    }

    fun isEmailVerified(address: String): Boolean {
        val emailCode = emailCodeRepository.findByEmail(address) ?: return false
        return emailCode.isVerified
    }

    private fun registerCodeEmailText(code: String): String {
        val context = Context()
        context.setVariable("code", code)
        return templateEngine.process("registerEmail", context)
    }

    private fun generateEmailCode(): String {
        return (100000..999999).random().toString()
    }
}
