package com.kioschool.kioschoolapi.global.resend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// dev와 prod가 같은 DB 인스턴스를 쓰므로, prod가 아닌 환경에서 실제 운영진에게 메일이 나가지 않게 막는다
@Component
class MailRecipientGuard(
    @Value("\${mail.allowed-recipient-domains}")
    allowedDomains: String,
) {
    private val allowAll = allowedDomains.trim() == ALLOW_ALL
    private val domains = allowedDomains.split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    // 설정이 실수로 비어도 실제 운영진에게 나가지 않도록, 비어 있으면 모두 막는다
    fun isAllowed(email: String): Boolean {
        if (allowAll) return true
        val at = email.lastIndexOf('@')
        if (at < 0) return false
        return email.substring(at + 1).lowercase() in domains
    }

    companion object {
        private const val ALLOW_ALL = "*"
    }
}
