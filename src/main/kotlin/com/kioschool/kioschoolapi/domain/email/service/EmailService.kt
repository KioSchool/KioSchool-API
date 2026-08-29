package com.kioschool.kioschoolapi.domain.email.service

import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import com.kioschool.kioschoolapi.domain.email.repository.EmailCodeRepository
import com.kioschool.kioschoolapi.domain.email.repository.EmailDomainRepository
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class EmailService(
    @Value("\${spring.mail.from}")
    private val fromAddress: String,
    private val javaMailSender: JavaMailSender,
    private val emailCodeRepository: EmailCodeRepository,
    private val emailDomainRepository: EmailDomainRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
        if (!isEmailDomainVerified(emailAddress)) throw CustomException(ErrorCode.NOT_VERIFIED_EMAIL_DOMAIN)
    }

    /**
     * 동기 발송. 발송 성공을 트랜잭션 커밋 조건으로 삼아야 하는 곳(문의 답변)에서 쓴다.
     * [sendEmail]은 @Async라 즉시 리턴하므로 성공 여부를 호출자가 알 수 없다.
     */
    fun sendEmailSync(address: String, subject: String, text: String) {
        try {
            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromAddress)
            helper.setTo(address)
            helper.setSubject(subject)
            helper.setText(text, true)

            javaMailSender.send(message)
        } catch (e: Exception) {
            log.error("Failed to send email to {}", address, e)
            throw CustomException(ErrorCode.EMAIL_SEND_FAILURE, cause = e)
        }
    }

    @Async
    fun sendEmail(address: String, subject: String, text: String) {
        sendEmailSync(address, subject, text)
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
        return UUID.randomUUID().toString()
    }

    fun verifyRegisterCode(email: String, code: String): Boolean {
        val emailCode =
            emailCodeRepository.findByEmailAndKind(email, EmailKind.REGISTER) ?: return false
        if (emailCode.code != code) return false
        emailCode.isVerified = true
        emailCodeRepository.save(emailCode)
        return true
    }

    fun getEmailByCode(code: String): String {
        val emailCode =
            emailCodeRepository.findByCodeAndKind(code, EmailKind.RESET_PASSWORD)
                ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
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

        return emailDomainRepository.findAll(
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.asc("name")
                )
            )
        )
    }

    fun validateEmailDomainDuplicate(domain: String) {
        if (isEmailDomainDuplicate(domain)) throw CustomException(ErrorCode.DUPLICATE_EMAIL_DOMAIN)
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
