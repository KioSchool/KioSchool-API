package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.logging.util.LogMasking
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class MailUnsubscribeInfo(
    val maskedEmail: String,
    val unsubscribed: Boolean,
)

@Service
class MailUnsubscribeService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun tokenFor(user: User): String {
        user.mailUnsubscribeToken?.let { return it }

        val token = newToken()
        user.mailUnsubscribeToken = token
        userRepository.save(user)
        return token
    }

    // 메일 보안 프로그램이 링크를 미리 열어도 거부되지 않도록 조회는 상태를 바꾸지 않는다
    fun find(token: String): MailUnsubscribeInfo {
        val user = getUser(token)
        return MailUnsubscribeInfo(
            maskedEmail = LogMasking.maskEmail(user.email.orEmpty()),
            unsubscribed = user.mailUnsubscribedAt != null,
        )
    }

    @Transactional
    fun unsubscribe(token: String) {
        val user = getUser(token)
        if (user.mailUnsubscribedAt != null) return

        user.mailUnsubscribedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    private fun getUser(token: String): User =
        userRepository.findByMailUnsubscribeToken(token) ?: throw CustomException(ErrorCode.MAIL_UNSUBSCRIBE_TOKEN_INVALID)

    private fun newToken() = "${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "")
}
