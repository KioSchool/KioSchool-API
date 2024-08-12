package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin User Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminUserController(
    private val userService: UserService
) {
    @Operation(summary = "모든 유저 조회", description = "현재 키오스쿨에 가입한 모든 사용자를 조회합니다.")
    @GetMapping("/users")
    fun getUser(
        authentication: Authentication,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<User> {
        val username = (authentication.principal as CustomUserDetails).username
        if (userService.isSuperAdminUser(username)) throw NoPermissionException()
        return userService.getAllUsers(page, size)
    }
}