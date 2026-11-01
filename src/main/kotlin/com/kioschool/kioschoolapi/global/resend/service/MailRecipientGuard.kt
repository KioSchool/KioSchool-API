package com.kioschool.kioschoolapi.global.resend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// dev와 prod가 같은 DB 인스턴스를 쓰므로, prod가 아닌 환경에서 실제 운영진에게 메일이 나가지 않게 막는다
@Component
class MailRecipientGuard(
    @Value("\${mail.allowed-recipient-domains}")
    allowedDomains: String,
) {
    private val domains = allowedDomains.split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    fun isAllowed(email: String): Boolean {
        if (domains.isEmpty()) return true
        val at = email.lastIndexOf('@')
        if (at < 0) return false
        return email.substring(at + 1).lowercase() in domains
    }
}
