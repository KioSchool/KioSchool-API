package com.kioschool.kioschoolapi.global.resend.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ResendWebhookVerifierTest : DescribeSpec({
    val rawKey = "test-secret-key-0123456789".toByteArray()
    val secret = "whsec_" + Base64.getEncoder().encodeToString(rawKey)
    val sut = ResendWebhookVerifier(secret)
    val body = """{"type":"inbox.email.received","data":{"thread_id":"한글 t1"}}""".toByteArray()
    val now = Instant.ofEpochSecond(1_793_000_000)
    val timestamp = now.epochSecond.toString()

    fun sign(content: ByteArray, id: String = "msg_1", ts: String = timestamp): String {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(rawKey, "HmacSHA256")) }
        return "v1," + Base64.getEncoder().encodeToString(mac.doFinal("$id.$ts.".toByteArray() + content))
    }

    describe("verify") {
        it("맞는 서명이면 true") {
            sut.verify(body, "msg_1", timestamp, sign(body), now) shouldBe true
        }

        it("여러 서명 중 하나만 맞아도 true") {
            sut.verify(body, "msg_1", timestamp, "v1,AAAA ${sign(body)} v2,BBBB", now) shouldBe true
        }

        it("본문이 한 바이트라도 다르면 false") {
            val tampered = body.copyOf().also { it[0] = (it[0] + 1).toByte() }

            sut.verify(tampered, "msg_1", timestamp, sign(body), now) shouldBe false
        }

        it("id가 다르면 false") {
            sut.verify(body, "msg_2", timestamp, sign(body), now) shouldBe false
        }

        it("시각 차이가 5분을 넘으면 false") {
            sut.verify(body, "msg_1", timestamp, sign(body), now.plusSeconds(301)) shouldBe false
            sut.verify(body, "msg_1", timestamp, sign(body), now.minusSeconds(301)) shouldBe false
        }

        it("시각 차이가 5분이면 true") {
            sut.verify(body, "msg_1", timestamp, sign(body), now.plusSeconds(300)) shouldBe true
        }

        it("헤더가 없으면 false") {
            sut.verify(body, null, timestamp, sign(body), now) shouldBe false
            sut.verify(body, "msg_1", null, sign(body), now) shouldBe false
            sut.verify(body, "msg_1", timestamp, null, now) shouldBe false
        }

        it("시각이 숫자가 아니거나 서명이 base64가 아니면 false") {
            sut.verify(body, "msg_1", "어제", sign(body), now) shouldBe false
            sut.verify(body, "msg_1", timestamp, "v1,%%%", now) shouldBe false
        }

        it("비밀키가 비어 있으면 false") {
            ResendWebhookVerifier("").verify(body, "msg_1", timestamp, sign(body), now) shouldBe false
        }
    }
})
