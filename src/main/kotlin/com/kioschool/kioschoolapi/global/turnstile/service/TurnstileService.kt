package com.kioschool.kioschoolapi.global.turnstile.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.turnstile.api.TurnstileApi
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Cloudflare Turnstile 토큰을 서버에서 검증한다.
 *
 * secret key가 비어 있으면 검증을 건너뛴다(키 발급 전·로컬).
 * Cloudflare 장애나 우리 쪽 secret 설정 오류는 통과시킨다(fail-open) —
 * 이 검증이 문의 접수의 단일 실패 지점이 되어서는 안 된다. 토큰 자체가 틀린 경우만 거절한다.
 */
@Service
class TurnstileService(
    @Value("\${turnstile.secret-key}")
    private val secretKey: String,
    private val turnstileApi: TurnstileApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun warnIfDisabled() {
        if (secretKey.isBlank()) log.warn("Turnstile secret key is not set; captcha verification is disabled")
    }

    fun verify(token: String?) {
        if (secretKey.isBlank()) return
        if (token.isNullOrBlank()) throw CustomException(ErrorCode.CAPTCHA_VERIFICATION_FAILED)

        val response = try {
            turnstileApi.siteverify(secretKey, token).execute()
        } catch (e: IOException) {
            log.warn("Turnstile verification skipped (request failed): {}", e.message)
            return
        }

        val body = response.body()
        if (!response.isSuccessful || body == null) {
            log.warn("Turnstile verification skipped (status={})", response.code())
            return
        }

        if (body.success) return

        if (body.errorCodes.any { it in SERVER_SIDE_ERROR_CODES }) {
            log.error("Turnstile verification skipped (server-side error): {}", body.errorCodes)
            return
        }

        log.info("Turnstile verification rejected: {}", body.errorCodes)
        throw CustomException(ErrorCode.CAPTCHA_VERIFICATION_FAILED)
    }

    companion object {
        private val SERVER_SIDE_ERROR_CODES = setOf("missing-input-secret", "invalid-input-secret", "internal-error")
    }
}
