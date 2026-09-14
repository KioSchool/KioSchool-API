package com.kioschool.kioschoolapi.global.turnstile.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.turnstile.api.TurnstileApi
import com.kioschool.kioschoolapi.global.turnstile.dto.SiteverifyResponse
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

class TurnstileServiceTest : DescribeSpec({
    val turnstileApi = mockk<TurnstileApi>()
    val call = mockk<Call<SiteverifyResponse>>()
    val sut = TurnstileService(secretKey = "secret", turnstileApi = turnstileApi)

    beforeTest { every { turnstileApi.siteverify("secret", "token") } returns call }

    afterTest { clearAllMocks() }

    describe("verify") {
        it("secret key가 비어 있으면 Cloudflare를 호출하지 않고 통과한다") {
            val disabled = TurnstileService(secretKey = "", turnstileApi = turnstileApi)

            shouldNotThrowAny { disabled.verify(null) }

            verify(exactly = 0) { turnstileApi.siteverify(any(), any()) }
        }

        it("토큰이 없으면 CAPTCHA_VERIFICATION_FAILED") {
            val ex = shouldThrow<CustomException> { sut.verify("  ") }

            ex.errorCode shouldBe ErrorCode.CAPTCHA_VERIFICATION_FAILED
            verify(exactly = 0) { turnstileApi.siteverify(any(), any()) }
        }

        it("검증에 성공하면 통과한다") {
            every { call.execute() } returns Response.success(SiteverifyResponse(success = true))

            shouldNotThrowAny { sut.verify("token") }
        }

        it("토큰이 틀리면 CAPTCHA_VERIFICATION_FAILED") {
            every { call.execute() } returns
                Response.success(SiteverifyResponse(success = false, errorCodes = listOf("invalid-input-response")))

            val ex = shouldThrow<CustomException> { sut.verify("token") }
            ex.errorCode shouldBe ErrorCode.CAPTCHA_VERIFICATION_FAILED
        }

        it("우리 쪽 secret 설정 오류면 통과시킨다 (fail-open)") {
            every { call.execute() } returns
                Response.success(SiteverifyResponse(success = false, errorCodes = listOf("invalid-input-secret")))

            shouldNotThrowAny { sut.verify("token") }
        }

        it("Cloudflare 요청이 실패하면 통과시킨다 (fail-open)") {
            every { call.execute() } throws IOException("timeout")

            shouldNotThrowAny { sut.verify("token") }
        }

        it("Cloudflare가 5xx를 주면 통과시킨다 (fail-open)") {
            every { call.execute() } returns
                Response.error(503, "unavailable".toResponseBody("text/plain".toMediaType()))

            shouldNotThrowAny { sut.verify("token") }
        }
    }
})
