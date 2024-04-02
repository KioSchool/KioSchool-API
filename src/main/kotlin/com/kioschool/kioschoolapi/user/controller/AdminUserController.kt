package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.dto.admin.CreateSuperUserRequestBody
import com.kioschool.kioschoolapi.user.dto.admin.RegisterAccountUrlRequestBody
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin User Controller")
@RestController
@RequestMapping("/admin")
class AdminUserController(
    private val userService: UserService
) {
    @Operation(summary = "로그인 유저 정보 조회", description = "현재 로그인한 유저의 정보를 조회합니다.")
    @GetMapping("/user")
    fun getUser(authentication: Authentication): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.getUser(username)
    }

    @Operation(summary = "유저 탈퇴")
    @DeleteMapping("/user")
    fun deleteUser(authentication: Authentication): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.deleteUser(username)
    }

    @Operation(summary = "슈퍼 유저 생성", description = "슈퍼 유저를 생성합니다.<br>슈퍼 유저는 슈퍼 유저만 지정할 수 있습니다.")
    @PostMapping("/super-user")
    fun createSuperUser(
        authentication: Authentication,
        @RequestBody body: CreateSuperUserRequestBody
    ): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.createSuperUser(username, body.id)
    }

    @Operation(summary = "토스 계좌 URL 등록", description = "토스 계좌 URL을 등록합니다.")
    @PostMapping("/user/toss-account")
    fun registerAccountUrl(
        authentication: Authentication,
        @RequestBody body: RegisterAccountUrlRequestBody
    ): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.registerAccountUrl(username, body.accountUrl)
    }
}