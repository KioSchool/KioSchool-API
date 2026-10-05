package com.kioschool.kioschoolapi.domain.user.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime

/**
 * 로그인한 기기 하나. refresh token의 비밀값은 해시로만 들고 있다.
 *
 * 갱신할 때마다 새 행을 만들지 않고 이 행을 덮어쓴다. 직전 해시 하나만 남겨 두면
 * 여러 탭의 동시 갱신(유예)과 옛 토큰 재사용(탈취 의심)을 구분할 수 있다.
 */
@Entity
@Table(name = "user_session")
class UserSession(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,
    var tokenHash: String,
    var expiresAt: LocalDateTime,
    var previousTokenHash: String? = null,
    var rotatedAt: LocalDateTime? = null,
) : BaseEntity() {
    enum class TokenMatch {
        /** 지금 유효한 토큰 */
        CURRENT,

        /** 방금 교체된 토큰. 같은 브라우저의 다른 탭이 동시에 갱신한 경우다. */
        RECENTLY_ROTATED,

        /** 교체된 지 오래된 토큰이 다시 들어왔다. 토큰이 새어 나갔다고 본다. */
        REUSED,

        /** 어느 해시와도 맞지 않는다. 세션 ID만 맞힌 요청일 수 있어 세션을 지우는 근거로 쓰지 않는다. */
        UNKNOWN,
    }

    fun isExpired(now: LocalDateTime) = !expiresAt.isAfter(now)

    fun match(presentedHash: String, now: LocalDateTime): TokenMatch {
        if (isSameHash(presentedHash, tokenHash)) return TokenMatch.CURRENT

        val previous = previousTokenHash ?: return TokenMatch.UNKNOWN
        if (!isSameHash(presentedHash, previous)) return TokenMatch.UNKNOWN

        val rotated = rotatedAt ?: return TokenMatch.REUSED
        return if (now.isAfter(rotated.plus(ROTATION_GRACE))) TokenMatch.REUSED else TokenMatch.RECENTLY_ROTATED
    }

    fun rotate(newTokenHash: String, now: LocalDateTime, validity: Duration) {
        previousTokenHash = tokenHash
        tokenHash = newTokenHash
        rotatedAt = now
        expiresAt = now.plus(validity)
    }

    private fun isSameHash(a: String, b: String) =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    companion object {
        val ROTATION_GRACE: Duration = Duration.ofSeconds(30)
    }
}
