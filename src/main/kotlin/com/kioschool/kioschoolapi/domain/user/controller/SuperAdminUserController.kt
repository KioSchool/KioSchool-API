package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Super Admin User Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminUserController(
    private val userFacade: UserFacade
) {
    @Operation(summary = "모든 유저 조회", description = "현재 키오스쿨에 가입한 모든 사용자를 조회합니다.")
    @GetMapping("/users")
    fun getUser(
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestParam name: String?
    ): Page<User> {
        return userFacade.getAllUsers(name, page, size)
    }
}