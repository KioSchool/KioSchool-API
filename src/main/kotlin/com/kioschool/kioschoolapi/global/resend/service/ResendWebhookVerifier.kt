package com.kioschool.kioschoolapi.global.resend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Resend 웹훅은 Svix 방식으로 서명한다
@Component
class ResendWebhookVerifier(
    @Value("\${resend.webhook-secret}")
    private val webhookSecret: String,
) {
    fun verify(
        body: ByteArray,
        id: String?,
        timestamp: String?,
        signature: String?,
        now: Instant = Instant.now(),
    ): Boolean {
        if (webhookSecret.isBlank() || id == null || timestamp == null || signature == null) return false

        val signedAt = timestamp.toLongOrNull()?.let(Instant::ofEpochSecond) ?: return false
        if (Duration.between(signedAt, now).abs() > TOLERANCE) return false

        val expected = hmac("$id.$timestamp.".toByteArray() + body)
        return signature.split(" ")
            .filter { it.startsWith(SIGNATURE_PREFIX) }
            .mapNotNull { decodeOrNull(it.removePrefix(SIGNATURE_PREFIX)) }
            .any { MessageDigest.isEqual(it, expected) }
    }

    private fun hmac(content: ByteArray): ByteArray {
        val key = Base64.getDecoder().decode(webhookSecret.removePrefix(SECRET_PREFIX))
        return Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }.doFinal(content)
    }

    private fun decodeOrNull(value: String): ByteArray? {
        return try {
            Base64.getDecoder().decode(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private const val SECRET_PREFIX = "whsec_"
        private const val SIGNATURE_PREFIX = "v1,"
        private val TOLERANCE = Duration.ofMinutes(5)
    }
}
