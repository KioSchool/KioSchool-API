package com.kioschool.kioschoolapi.global.security

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.nimbusds.jose.util.StandardCharset
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
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
        var token: String? = null

        // 1. [정석] 쿠키 배열에서 찾기 (Authorization 또는 accessToken)
        token = request.cookies?.find {
            it.name == HttpHeaders.AUTHORIZATION || it.name == "accessToken"
        }?.value

        // 2. [비상대책] Tomcat이 쿠키 파싱을 못했을 경우, Raw Header를 직접 수색
        if (token == null) {
            val cookieHeader = request.getHeader("Cookie")
            if (cookieHeader != null) {
                // "Authorization=eyJ..." 패턴을 정규식으로 직접 찾음
                // (이름을 accessToken으로 바꿨다면 "accessToken=([^;]+)" 로 수정 필요)
                val match = Regex("accessToken=([^;]+)").find(cookieHeader)
                token = match?.groupValues?.get(1)
            }
        }

        // 3. [헤더] Bearer 토큰 확인 (앱/Postman 요청 대비)
        if (token == null) {
            val header = request.getHeader(HttpHeaders.AUTHORIZATION)
            if (header != null && header.startsWith("Bearer ")) {
                token = header.replace("Bearer ", "")
            }
        }

        return token?.trim()
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