package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.email.entity.EmailCode
import com.kioschool.kioschoolapi.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.exception.InvalidEmailAddressException
import jakarta.transaction.Transactional
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
        if (!validateEmailAddress(address)) throw InvalidEmailAddressException()

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

    @Transactional
    fun deleteEmailCode(address: String) {
        emailCodeRepository.deleteByEmail(address)
    }

    private fun registerCodeEmailText(code: String): String {
        val context = Context()
        context.setVariable("code", code)
        return templateEngine.process("registerEmail", context)
    }

    private fun generateEmailCode(): String {
        return (100000..999999).random().toString()
    }

    fun verifyRegisterCode(email: String, code: String): Boolean {
        val emailCode = emailCodeRepository.findByEmail(email) ?: return false
        if (emailCode.code != code) return false
        emailCode.isVerified = true
        emailCodeRepository.save(emailCode)
        return true
    }

    private fun validateEmailAddress(address: String): Boolean {
        return address.matches(Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
    }
}
