package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {
    fun login(loginId: String, loginPassword: String): String {
        val user = getUser(loginId)

        if (!passwordEncoder.matches(
                loginPassword,
                user.loginPassword
            )
        ) throw LoginFailedException()

        return jwtProvider.createToken(user)
    }

    fun register(loginId: String, loginPassword: String, name: String, email: String): String {
        if (isDuplicateLoginId(loginId)) throw RegisterException()
        if (!emailService.isEmailVerified(email)) throw RegisterException()

        val user = userRepository.save(
            User(
                loginId = loginId,
                loginPassword = passwordEncoder.encode(loginPassword),
                name = name,
                email = email,
                role = UserRole.ADMIN,
                workspaces = mutableListOf()
            )
        )

        return jwtProvider.createToken(user)
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userRepository.findByLoginId(loginId) != null
    }

    fun getUser(loginId: String): User {
        return userRepository.findByLoginId(loginId) ?: throw LoginFailedException()
    }
}