package com.kioschool.kioschoolapi.domain.account.controller

import com.kioschool.kioschoolapi.domain.account.dto.common.BankDto
import com.kioschool.kioschoolapi.domain.account.dto.request.RegisterAccountRequestBody
import com.kioschool.kioschoolapi.domain.account.dto.request.RegisterTossAccountRequestBody
import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
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
        return accountFacade.getAllBanks()
    }

    @Operation(summary = "계좌 등록", description = "계좌를 등록합니다.")
    @PostMapping("/account")
    fun registerAccount(
        @AdminUsername username: String,
        @RequestBody body: RegisterAccountRequestBody
    ): UserDto {
        return accountFacade.registerAccount(
            username,
            body.bankId,
            body.accountNumber,
            body.accountHolder
        )

    }

    @Operation(summary = "계좌 삭제", description = "등록된 계좌를 삭제합니다.")
    @DeleteMapping("/account")
    fun deleteAccount(
        @AdminUsername username: String
    ): UserDto {
        return accountFacade.deleteAccount(
            username
        )
    }

    @Operation(summary = "토스 계좌 URL 등록", description = "토스 계좌 URL을 등록합니다.")
    @PostMapping("/toss-account")
    fun registerTossAccount(
        @AdminUsername username: String,
        @RequestBody body: RegisterTossAccountRequestBody
    ): UserDto {
        return accountFacade.registerTossAccount(
            username,
            body.accountUrl
        )
    }

    @Operation(summary = "토스 계좌 URL 삭제", description = "등록된 토스 계좌 URL을 삭제합니다.")
    @DeleteMapping("/toss-account")
    fun deleteTossAccount(
        @AdminUsername username: String
    ): UserDto {
        return accountFacade.deleteTossAccount(
            username
        )
    }

}