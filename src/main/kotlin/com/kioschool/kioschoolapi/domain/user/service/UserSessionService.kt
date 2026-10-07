package com.kioschool.kioschoolapi.domain.user.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.entity.UserSession
import com.kioschool.kioschoolapi.domain.user.entity.UserSession.TokenMatch
import com.kioschool.kioschoolapi.domain.user.repository.UserSessionRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.util.Base64
import java.util.HexFormat

sealed interface SessionRefreshResult {
    /** 토큰을 교체했다. 새 refresh token을 쿠키로 내려야 한다. */
    data class Rotated(val user: User, val refreshToken: String) : SessionRefreshResult

    /** 같은 브라우저의 다른 요청이 방금 교체했다. 새 refresh 쿠키는 그 응답이 심으므로 access token만 준다. */
    data class Grace(val user: User) : SessionRefreshResult

    data class Rejected(val reason: RejectReason) : SessionRefreshResult

    enum class RejectReason { INVALID, EXPIRED, REUSED }
}

/**
 * refresh token은 `{세션 ID}.{비밀값}` 형태의 난수 문자열이다. 서명이 없으므로 DB의 해시와 대조해야만 쓸 수 있다.
 */
@Service
class UserSessionService(
    private val userSessionRepository: UserSessionRepository,
    @Value("\${jwt.refresh-token-validity}")
    private val refreshTokenValidity: Duration,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    fun create(user: User, now: LocalDateTime = LocalDateTime.now()): String {
        val secret = generateSecret()
        val session = userSessionRepository.save(
            UserSession(user = user, tokenHash = hash(secret), expiresAt = now.plus(refreshTokenValidity))
        )
        return format(session.id, secret)
    }

    // 예외로 빠져나가면 탈취 의심 세션 삭제까지 롤백되므로, 거부도 결과 값으로 돌려준다.
    @Transactional
    fun refresh(refreshToken: String?, now: LocalDateTime = LocalDateTime.now()): SessionRefreshResult {
        val parsed = parse(refreshToken) ?: return reject(SessionRefreshResult.RejectReason.INVALID)
        val session = userSessionRepository.findByIdForUpdate(parsed.sessionId)
            ?: return reject(SessionRefreshResult.RejectReason.INVALID)

        if (session.isExpired(now)) {
            userSessionRepository.delete(session)
            return reject(SessionRefreshResult.RejectReason.EXPIRED)
        }

        return when (session.match(hash(parsed.secret), now)) {
            TokenMatch.CURRENT -> {
                val secret = generateSecret()
                session.rotate(hash(secret), now, refreshTokenValidity)
                count("rotated")
                SessionRefreshResult.Rotated(loadUser(session), format(session.id, secret))
            }

            TokenMatch.RECENTLY_ROTATED -> {
                count("grace")
                SessionRefreshResult.Grace(loadUser(session))
            }

            TokenMatch.REUSED -> {
                log.warn("[AUDIT] action=REFRESH_TOKEN_REUSE userId={} sessionId={}", session.user.id, session.id)
                userSessionRepository.delete(session)
                reject(SessionRefreshResult.RejectReason.REUSED)
            }

            TokenMatch.UNKNOWN -> reject(SessionRefreshResult.RejectReason.INVALID)
        }
    }

    /** 로그아웃. 그 기기의 토큰을 실제로 가진 요청만 세션을 지울 수 있다. */
    @Transactional
    fun delete(refreshToken: String?, now: LocalDateTime = LocalDateTime.now()) {
        val parsed = parse(refreshToken) ?: return
        val session = userSessionRepository.findById(parsed.sessionId).orElse(null) ?: return

        val match = session.match(hash(parsed.secret), now)
        if (match == TokenMatch.CURRENT || match == TokenMatch.RECENTLY_ROTATED) {
            userSessionRepository.delete(session)
        }
    }

    /** 모든 기기 로그아웃. */
    @Transactional
    fun deleteAllOf(user: User) {
        userSessionRepository.deleteAllByUser(user)
    }

    @Transactional
    fun purgeExpired(now: LocalDateTime = LocalDateTime.now()): Int {
        return userSessionRepository.deleteAllExpired(now)
    }

    // access token을 만들 때 트랜잭션 밖에서 유저 필드를 읽으므로 여기서 프록시를 풀어 둔다.
    private fun loadUser(session: UserSession): User {
        Hibernate.initialize(session.user)
        return session.user
    }

    private fun reject(reason: SessionRefreshResult.RejectReason): SessionRefreshResult.Rejected {
        count(reason.name.lowercase())
        return SessionRefreshResult.Rejected(reason)
    }

    private fun count(result: String) {
        Counter.builder("kioschool.auth.refresh")
            .description("refresh token 갱신 결과")
            .tag("result", result)
            .register(meterRegistry)
            .increment()
    }

    private fun generateSecret(): String {
        val bytes = ByteArray(SECRET_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun format(sessionId: Long, secret: String) = "$sessionId$SEPARATOR$secret"

    private fun parse(refreshToken: String?): ParsedToken? {
        if (refreshToken.isNullOrBlank()) return null

        val sessionId = refreshToken.substringBefore(SEPARATOR, "").toLongOrNull() ?: return null
        val secret = refreshToken.substringAfter(SEPARATOR, "")
        if (secret.isEmpty()) return null

        return ParsedToken(sessionId, secret)
    }

    private data class ParsedToken(val sessionId: Long, val secret: String)

    companion object {
        private const val SECRET_BYTES = 32
        private const val SEPARATOR = '.'

        fun hash(secret: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
            return HexFormat.of().formatHex(digest)
        }
    }
}
