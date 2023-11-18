package com.kioschool.kioschoolapi.security

import com.kioschool.kioschoolapi.user.exception.InvalidJwtException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(
    val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        val user = userRepository.findByLoginId(username!!) ?: throw InvalidJwtException()
        return CustomUserDetails(user)
    }
}