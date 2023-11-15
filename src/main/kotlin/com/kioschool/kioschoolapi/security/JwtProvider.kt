package com.kioschool.kioschoolapi.security

import com.kioschool.kioschoolapi.user.entity.User
import com.nimbusds.jose.util.StandardCharset
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


@Component
class JwtProvider(
    @Value("\${jwt.secret-key}")
    private val salt: String
) {
    private val secretKey = Keys.hmacShaKeyFor(salt.toByteArray(StandardCharset.UTF_8))

    fun createToken(user: User): String {
        val claims = Jwts.claims().setSubject(user.loginId)
        claims["role"] = user.role
        return Jwts.builder()
            .setClaims(claims)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }
}