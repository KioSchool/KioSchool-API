package com.kioschool.kioschoolapi.global.security

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.nimbusds.jose.util.StandardCharset
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*


@Component
class JwtProvider(
    @Value("\${jwt.secret-key}")
    private val salt: String,
    private val userDetailService: CustomUserDetailService
) {
    private val secretKey = Keys.hmacShaKeyFor(salt.toByteArray(StandardCharset.UTF_8))
    private val expirationTime = 1000L * 60 * 60 * 24

    fun createToken(user: User): String {
        val now = Date()
        val claims = Jwts.claims().setSubject(user.loginId)
        claims["roles"] = listOf(user.role.name)
        return Jwts.builder()
            .setIssuedAt(now)
            .setClaims(claims)
            .setExpiration(Date(now.time + expirationTime))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun resolveToken(request: HttpServletRequest): String? {
        println("========== [DEBUG] resolveToken Start ==========")
        println("Request URL: ${request.requestURL}")

        // 1. 헤더 전체 출력 (Cookie 헤더가 살아있는지 확인)
        println("--- [Headers] ---")
        request.headerNames?.asIterator()?.forEachRemaining { name ->
            println("Header [$name]: ${request.getHeader(name)}")
        }

        // 2. 파싱된 쿠키 배열 출력 (Tomcat이 쿠키를 인식했는지 확인)
        println("--- [Parsed Cookies] ---")
        val cookies = request.cookies
        if (cookies == null) {
            println("request.cookies is NULL! (Tomcat found no cookies)")
        } else {
            println("Cookie Count: ${cookies.size}")
            cookies.forEach { cookie ->
                println(" - Name: [${cookie.name}], Value: [${cookie.value.take(10)}...], Domain: [${cookie.domain}], Path: [${cookie.path}]")
            }
        }

        // 3. 실제 로직 수행 (accessToken 이름으로 찾기)
        // 주의: 로그인 컨트롤러에서도 쿠키 이름을 "accessToken"으로 바꿨는지 꼭 확인하세요!
        val rawToken = cookies?.find { it.name == "__session" }?.value
            ?: request.getHeader("__session") // 헤더에서도 찾음
            ?: request.getHeader("Authorization") // 혹시 몰라 Authorization 헤더도 찾음

        println("--- [Result] ---")
        println("Resolved Raw Token: ${rawToken?.take(10)}...")
        println("========== [DEBUG] resolveToken End ==========")

        return rawToken?.replace("Bearer ", "")?.trim()
    }

    fun isValidToken(token: String): Boolean {
        return try {
            val now = Date()
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
            return claims.body.expiration.after(now)
        } catch (e: Exception) {
            false
        }
    }

    fun getAuthentication(token: String): Authentication {
        val userDetails = userDetailService.loadUserByUsername(getLoginId(token))
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    fun getLoginId(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
    }
}