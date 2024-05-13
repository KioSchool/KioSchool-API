package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.email.entity.EmailCode
import com.kioschool.kioschoolapi.email.enum.EmailKind
import com.kioschool.kioschoolapi.email.exception.NotVerifiedEmailDomainException
import com.kioschool.kioschoolapi.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.email.repository.EmailDomainRepository
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
    private val emailCodeRepository: EmailCodeRepository,
    private val emailDomainRepository: EmailDomainRepository
) {
    fun sendRegisterCodeEmail(address: String) {
        if (!isEmailDomainVerified(address)) throw NotVerifiedEmailDomainException()

        val code = generateEmailCode()
        sendEmail(
            address,
            "키오스쿨 회원가입 인증 코드",
            registerCodeEmailText(code)
        )

        val emailCode =
            emailCodeRepository.findByEmailAndKind(address, EmailKind.REGISTER) ?: EmailCode(
                address,
                code,
                kind = EmailKind.REGISTER
            )
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
        val emailCode =
            emailCodeRepository.findByEmailAndKind(address, EmailKind.REGISTER) ?: return false
        return emailCode.isVerified
    }

    @Transactional
    fun deleteRegisterCode(address: String) {
        emailCodeRepository.deleteByEmailAndKind(address, EmailKind.REGISTER)
    }

    private fun registerCodeEmailText(code: String): String {
        val context = Context()
        context.setVariable("code", code)
        return templateEngine.process("registerEmail", context)
    }

    private fun resetPasswordEmailText(code: String): String {
        val context = Context()
        context.setVariable("code", code)
        return templateEngine.process("resetPasswordEmail", context)
    }

    private fun generateEmailCode(): String {
        return (100000..999999).random().toString()
    }

    private fun generateResetPasswordEmailCode(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..50).map { charset.random() }.joinToString("")
    }

    private fun isEmailDomainVerified(email: String): Boolean {
        val domain = email.substringAfterLast("@")
        return emailDomainRepository.findByDomain(domain) != null
    }

    fun verifyRegisterCode(email: String, code: String): Boolean {
        val emailCode =
            emailCodeRepository.findByEmailAndKind(email, EmailKind.REGISTER) ?: return false
        if (emailCode.code != code) return false
        emailCode.isVerified = true
        emailCodeRepository.save(emailCode)
        return true
    }

    fun getEmailByCode(code: String): String? {
        val emailCode =
            emailCodeRepository.findByCodeAndKind(code, EmailKind.RESET_PASSWORD) ?: return null
        return emailCode.email
    }

    fun sendResetPasswordEmail(email: String) {
        val code = generateResetPasswordEmailCode()
        sendEmail(
            email,
            "키오스쿨 비밀번호 재설정",
            resetPasswordEmailText(code)
        )

        val emailCode =
            emailCodeRepository.findByEmailAndKind(email, EmailKind.RESET_PASSWORD) ?: EmailCode(
                email,
                code,
                kind = EmailKind.RESET_PASSWORD
            )
        emailCode.code = code
        emailCodeRepository.save(emailCode)
    }

    @Transactional
    fun deleteResetPasswordCode(code: String) {
        val emailCode =
            emailCodeRepository.findByCodeAndKind(code, EmailKind.RESET_PASSWORD) ?: return
        emailCodeRepository.delete(emailCode)
    }
}
