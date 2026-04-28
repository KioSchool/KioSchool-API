package com.kioschool.kioschoolapi.global.security

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.UserRole
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
import org.slf4j.LoggerFactory
import java.util.*


@Component
class JwtProvider(
    @Value("\${jwt.secret-key}")
    private val salt: String,
    private val userDetailService: CustomUserDetailService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val secretKey = Keys.hmacShaKeyFor(salt.toByteArray(StandardCharset.UTF_8))
    private val expirationTime = 1000L * 60 * 60 * 24

    fun createToken(user: User): String {
        val now = Date()
        val claims = Jwts.claims().setSubject(user.loginId)
        claims["roles"] = listOf(user.role.name)
        claims["userId"] = user.id
        return Jwts.builder()
            .setIssuedAt(now)
            .setClaims(claims)
            .setExpiration(Date(now.time + expirationTime))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    companion object {
        const val BEARER_PREFIX = "Bearer "
    }

    fun resolveToken(request: HttpServletRequest): String? {
        val rawToken = request.cookies?.find { it.name == HttpHeaders.AUTHORIZATION }?.value
            ?: request.getHeader(HttpHeaders.AUTHORIZATION)
            ?: return null

        return rawToken.replace(BEARER_PREFIX, "")
    }

    fun isValidToken(token: String): Boolean {
        return try {
            val now = Date()
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
            claims.body.expiration.after(now)
        } catch (e: Exception) {
            log.warn("Invalid JWT token: {}", e.message)
            false
        }
    }

    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body

        val loginId = claims.subject
        val userId = claims["userId"] as? Number
        val roles = claims["roles"] as? List<*>

        if (userId == null || roles.isNullOrEmpty()) {
            // 과거 호환용: 토큰에 userId나 roles가 없으면 기존처럼 DB 조회로 폴백
            val userDetails = userDetailService.loadUserByUsername(loginId)
            return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
        }

        // DB 조회 없이 토큰 정보만으로 인증 객체 생성
        val roleStr = roles.first().toString()
        val userDetails = CustomUserDetails(
            userId = userId.toLong(),
            loginId = loginId,
            role = UserRole.valueOf(roleStr)
        )
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