package com.kioschool.kioschoolapi.security

import com.kioschool.kioschoolapi.global.security.AuthCookieManager
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration

class AuthCookieManagerTest : DescribeSpec({
    val sut = AuthCookieManager(
        isSecure = true,
        accessTokenValidity = Duration.ofMinutes(30),
        refreshTokenValidity = Duration.ofDays(7),
    )

    describe("setTokens") {
        it("쿠키 수명을 토큰 수명과 같게 둔다") {
            val response = MockHttpServletResponse()

            sut.setTokens(response, "access", "1.secret")

            response.getHeaders("Set-Cookie") shouldContainExactly listOf(
                "Authorization=access; Path=/; Max-Age=1800; Expires=${response.expiresOf(0)}; Secure; HttpOnly; SameSite=NONE",
                "RefreshToken=1.secret; Path=/; Max-Age=604800; Expires=${response.expiresOf(1)}; Secure; HttpOnly; SameSite=NONE",
            )
        }

        it("secure가 꺼진 로컬에서는 SameSite=LAX로 내린다") {
            val local = AuthCookieManager(false, Duration.ofMinutes(30), Duration.ofDays(7))
            val response = MockHttpServletResponse()

            local.setAccessToken(response, "access")

            response.getHeader("Set-Cookie") shouldBe
                "Authorization=access; Path=/; Max-Age=1800; Expires=${response.expiresOf(0)}; HttpOnly; SameSite=LAX"
        }
    }

    describe("clearTokens") {
        it("두 쿠키를 모두 Max-Age=0으로 지운다") {
            val response = MockHttpServletResponse()

            sut.clearTokens(response)

            response.getHeaders("Set-Cookie") shouldContainExactly listOf(
                "Authorization=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=NONE",
                "RefreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=NONE",
            )
        }
    }

    describe("resolveRefreshToken") {
        it("refresh 쿠키 값을 읽는다") {
            val request = MockHttpServletRequest().apply {
                setCookies(Cookie("Authorization", "access"), Cookie("RefreshToken", "1.secret"))
            }

            sut.resolveRefreshToken(request) shouldBe "1.secret"
        }

        it("쿠키가 없으면 null") {
            sut.resolveRefreshToken(MockHttpServletRequest()) shouldBe null
        }
    }
})

// Expires는 발급 시각에 따라 달라지므로 실제 헤더에서 꺼내 비교한다.
private fun MockHttpServletResponse.expiresOf(index: Int): String =
    getHeaders("Set-Cookie")[index].substringAfter("Expires=").substringBefore(";")
