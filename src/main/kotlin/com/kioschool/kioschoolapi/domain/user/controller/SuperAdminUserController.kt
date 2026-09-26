package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.dto.common.SuperAdminUserDto
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.global.common.enums.UserAccountFilter
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
    @Operation(
        summary = "모든 유저 조회",
        description = "가입한 사용자를 최근 가입 순으로 조회합니다. keyword는 이름·이메일·아이디에서 찾고, accountFilter로 계좌 연동 상태를 거릅니다."
    )
    @GetMapping("/users")
    fun getUser(
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) accountFilter: UserAccountFilter?
    ): Page<SuperAdminUserDto> {
        return userFacade.getAllUsers(keyword, accountFilter, page, size)
    }
}