package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
)