package com.kioschool.kioschoolapi.security

import com.kioschool.kioschoolapi.user.entity.User
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
    private val expirationTime = 1000L * 60 * 60 * 24 * 7

    fun createToken(user: User): String {
        val claims = Jwts.claims().setSubject(user.loginId)
        val now = Date()
        claims["roles"] = listOf(user.role.name)
        return Jwts.builder()
            .setIssuedAt(Date())
            .setClaims(claims)
            .setExpiration(Date(now.time + expirationTime))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun resolveToken(request: HttpServletRequest): String? {
        val rawToken = request.getHeader("Authorization") ?: return null

        return rawToken.replace("Bearer ", "")
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
            return claims.body.expiration.after(Date())
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