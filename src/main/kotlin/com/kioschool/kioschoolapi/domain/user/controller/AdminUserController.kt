package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.user.dto.admin.CreateSuperUserRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.admin.RegisterAccountUrlRequestBody
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.global.common.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin User Controller")
@RestController
@RequestMapping("/admin")
class AdminUserController(
    private val userFacade: UserFacade
) {
    @Operation(summary = "로그인 유저 정보 조회", description = "현재 로그인한 유저의 정보를 조회합니다.")
    @GetMapping("/user")
    fun getUser(@AdminUsername username: String): UserDto {
        return UserDto.of(userFacade.getUser(username))
    }

    @Operation(summary = "유저 탈퇴")
    @DeleteMapping("/user")
    fun deleteUser(@AdminUsername username: String): UserDto {
        return UserDto.of(userFacade.deleteUser(username))
    }

    @Operation(summary = "슈퍼 유저 생성", description = "슈퍼 유저를 생성합니다.<br>슈퍼 유저는 슈퍼 유저만 지정할 수 있습니다.")
    @PostMapping("/super-user")
    fun createSuperUser(
        @AdminUsername username: String,
        @RequestBody body: CreateSuperUserRequestBody
    ): UserDto {
        return UserDto.of(userFacade.createSuperAdminUser(username, body.id))
    }

    @Operation(summary = "토스 계좌 URL 등록", description = "토스 계좌 URL을 등록합니다.")
    @PostMapping("/user/toss-account")
    fun registerAccountUrl(
        @AdminUsername username: String,
        @RequestBody body: RegisterAccountUrlRequestBody
    ): UserDto {
        return UserDto.of(userFacade.registerAccountUrl(username, body.accountUrl))
    }
}