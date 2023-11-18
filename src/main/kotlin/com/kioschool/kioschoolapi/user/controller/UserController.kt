package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.user.dto.LoginRequestBody
import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService
) {
    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequestBody): String {
        return userService.login(body.id, body.password)
    }
}