package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService
)