package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.exception.InvalidJwtException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder
) {
    fun login(loginId: String, loginPassword: String): String {
        val user = userRepository.findByLoginId(loginId) ?: throw InvalidJwtException()

        if (!passwordEncoder.matches(loginPassword, user.loginPassword)) throw InvalidJwtException()
        
        return jwtProvider.createToken(user)
    }
}