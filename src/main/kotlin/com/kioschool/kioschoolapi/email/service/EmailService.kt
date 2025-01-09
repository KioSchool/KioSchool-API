package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.email.entity.EmailCode
import com.kioschool.kioschoolapi.email.entity.EmailDomain
import com.kioschool.kioschoolapi.email.enum.EmailKind
import com.kioschool.kioschoolapi.email.exception.DuplicatedEmailDomainException
import com.kioschool.kioschoolapi.email.exception.NotVerifiedEmailDomainException
import com.kioschool.kioschoolapi.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.email.repository.EmailDomainRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val javaMailSender: JavaMailSender,
    private val emailCodeRepository: EmailCodeRepository,
    private val emailDomainRepository: EmailDomainRepository
) {
    fun createOrUpdateRegisterEmailCode(emailAddress: String, code: String): EmailCode {
        val emailCode =
            emailCodeRepository.findByEmailAndKind(emailAddress, EmailKind.REGISTER) ?: EmailCode(
                emailAddress,
                code,
                kind = EmailKind.REGISTER
            )
        emailCode.code = code
        return emailCodeRepository.save(emailCode)
    }

    fun validateEmailDomainVerified(emailAddress: String) {
        if (!isEmailDomainVerified(emailAddress)) throw NotVerifiedEmailDomainException()
    }

    fun sendEmail(address: String, subject: String, text: String) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setTo(address)
        helper.setSubject(subject)
        helper.setText(text, true)
        javaMailSender.send(message)
    }

    fun isRegisterEmailVerified(address: String): Boolean {
        val emailCode =
            emailCodeRepository.findByEmailAndKind(address, EmailKind.REGISTER) ?: return false
        return emailCode.isVerified
    }

    @Transactional
    fun deleteRegisterCode(address: String) {
        emailCodeRepository.deleteByEmailAndKind(address, EmailKind.REGISTER)
    }

    fun generateRegisterCode(): String {
        return (100000..999999).random().toString()
    }

    fun generateResetPasswordCode(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..50).map { charset.random() }.joinToString("")
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

    fun createOrUpdateResetPasswordEmailCode(email: String, code: String): EmailCode {
        val emailCode =
            emailCodeRepository.findByEmailAndKind(email, EmailKind.RESET_PASSWORD) ?: EmailCode(
                email,
                code,
                kind = EmailKind.RESET_PASSWORD
            )
        emailCode.code = code
        return emailCodeRepository.save(emailCode)
    }

    @Transactional
    fun deleteResetPasswordCode(code: String) {
        val emailCode =
            emailCodeRepository.findByCodeAndKind(code, EmailKind.RESET_PASSWORD) ?: return
        emailCodeRepository.delete(emailCode)
    }

    fun getAllEmailDomains(name: String?, page: Int, size: Int): Page<EmailDomain> {
        if (!name.isNullOrBlank())
            return emailDomainRepository.findByNameContains(
                name,
                PageRequest.of(page, size)
            )

        return emailDomainRepository.findAll(PageRequest.of(page, size))
    }

    fun validateEmailDomainDuplicate(domain: String) {
        if (isEmailDomainDuplicate(domain)) throw DuplicatedEmailDomainException()
    }

    private fun isEmailDomainDuplicate(domain: String): Boolean {
        return emailDomainRepository.findByDomain(domain) != null
    }

    fun registerEmailDomain(name: String, domain: String): EmailDomain {
        return emailDomainRepository.save(EmailDomain(name, domain))
    }

    @Transactional
    fun deleteEmailDomain(domainId: Long): EmailDomain {
        val emailDomain = emailDomainRepository.findById(domainId).orElseThrow()
        emailDomainRepository.delete(emailDomain)
        return emailDomain
    }

    private fun isEmailDomainVerified(email: String): Boolean {
        val domain = email.substringAfterLast("@")
        return emailDomainRepository.findByDomain(domain) != null
    }
}
