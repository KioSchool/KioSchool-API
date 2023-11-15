package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider
) {
    fun login(loginId: String, loginPassword: String) {
        val user = userRepository.findByLoginId(loginId)
        val token = jwtProvider.createToken(user)
        println(token)
    }
}