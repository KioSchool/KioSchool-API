package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService
) {
    @PostMapping("/login")
    fun login(): String {
        return userService.login("test", "test")
    }

    @GetMapping("/admin/test")
    fun adminTest(): String {
        return "admin test"
    }
}