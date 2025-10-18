package com.kioschool.kioschoolapi.domain.account.controller

import com.kioschool.kioschoolapi.domain.account.dto.BankDto
import com.kioschool.kioschoolapi.domain.account.dto.RegisterAccountRequestBody
import com.kioschool.kioschoolapi.domain.account.dto.RegisterTossAccountRequestBody
import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import com.kioschool.kioschoolapi.domain.user.dto.UserDto
import com.kioschool.kioschoolapi.global.common.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class AdminAccountController(
    private val accountFacade: AccountFacade
) {
    @Operation(summary = "은행 조회", description = "키오스쿨에서 등록 가능한 은행을 조회합니다.")
    @GetMapping("/banks")
    fun getBanks(
        @AdminUsername username: String
    ): List<BankDto> {
        return accountFacade.getAllBanks().map { BankDto.of(it) }
    }

    @Operation(summary = "계좌 등록", description = "계좌를 등록합니다.")
    @PostMapping("/account")
    fun registerAccount(
        @AdminUsername username: String,
        @RequestBody body: RegisterAccountRequestBody
    ): UserDto {
        return UserDto.of(accountFacade.registerAccount(
            username,
            body.bankId,
            body.accountNumber,
            body.accountHolder
        ))
    }

    @Operation(summary = "토스 계좌 URL 등록", description = "토스 계좌 URL을 등록합니다.")
    @PostMapping("/toss-account")
    fun registerTossAccount(
        @AdminUsername username: String,
        @RequestBody body: RegisterTossAccountRequestBody
    ): UserDto {
        return UserDto.of(accountFacade.registerTossAccount(
            username,
            body.accountUrl
        ))
    }

}