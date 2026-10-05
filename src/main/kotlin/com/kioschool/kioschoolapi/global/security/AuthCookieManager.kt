package com.kioschool.kioschoolapi.global.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.server.Cookie.SameSite
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.util.WebUtils
import java.time.Duration

/**
 * 인증 쿠키 두 개(access, refresh)를 한곳에서 만든다.
 * 쿠키 수명은 토큰 수명과 같게 둬서, 만료된 토큰은 브라우저가 아예 보내지 않게 한다.
 */
@Component
class AuthCookieManager(
    @Value("\${kioschool.cookie.secure}")
    private val isSecure: Boolean,
    @Value("\${jwt.access-token-validity}")
    private val accessTokenValidity: Duration,
    @Value("\${jwt.refresh-token-validity}")
    private val refreshTokenValidity: Duration,
) {
    fun setTokens(response: HttpServletResponse, accessToken: String, refreshToken: String) {
        setAccessToken(response, accessToken)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_TOKEN, refreshToken, refreshTokenValidity))
    }

    fun setAccessToken(response: HttpServletResponse, accessToken: String) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_TOKEN, accessToken, accessTokenValidity))
    }

    fun clearTokens(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_TOKEN, "", Duration.ZERO))
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_TOKEN, "", Duration.ZERO))
    }

    fun resolveRefreshToken(request: HttpServletRequest): String? {
        return WebUtils.getCookie(request, REFRESH_TOKEN)?.value
    }

    private fun cookie(name: String, value: String, maxAge: Duration): String {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(isSecure)
            .path("/")
            .maxAge(maxAge)
            .sameSite(if (isSecure) SameSite.NONE.name else SameSite.LAX.name)
            .build()
            .toString()
    }

    companion object {
        // JwtProvider와 웹소켓 핸드셰이크가 이 이름으로 access token을 읽는다.
        const val ACCESS_TOKEN = HttpHeaders.AUTHORIZATION
        const val REFRESH_TOKEN = "RefreshToken"
    }
}
