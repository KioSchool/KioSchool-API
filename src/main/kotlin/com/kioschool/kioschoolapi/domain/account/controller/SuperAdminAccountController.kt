package com.kioschool.kioschoolapi.domain.account.controller

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountConnectionStatusDto
import com.kioschool.kioschoolapi.domain.account.dto.common.BankDto
import com.kioschool.kioschoolapi.domain.account.dto.request.AddBankRequestBody
import com.kioschool.kioschoolapi.domain.account.dto.request.DeleteBankRequestBody
import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Account Controller")

@RestController
@RequestMapping("/super-admin")
class SuperAdminAccountController(
    private val accountFacade: AccountFacade
) {
    @Operation(summary = "은행 조회", description = "키오스쿨에서 등록 가능한 은행을 조회합니다.")
    @GetMapping("/banks")
    fun getBanks(
        @RequestParam(required = false) name: String?,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<BankDto> {
        return accountFacade.getBanks(name, page, size)
    }

    @Operation(summary = "은행 추가", description = "키오스쿨에서 등록 가능한 은행을 추가합니다.")
    @PostMapping("/bank")
    fun addBank(
        @RequestBody body: AddBankRequestBody
    ): BankDto {
        return accountFacade.addBank(body.name, body.code)
    }

    @Operation(summary = "은행 삭제", description = "키오스쿨에서 등록 가능한 은행을 삭제합니다.")
    @DeleteMapping("/bank")
    fun deleteBank(
        @RequestBody body: DeleteBankRequestBody
    ): BankDto {
        return accountFacade.deleteBank(body.id)
    }

    @Operation(
        summary = "계정 연동 현황 조회",
        description = "전체 유저 중 계좌(Account)를 연동한 유저와 미연동 유저의 수 및 비율을 조회합니다."
    )
    @GetMapping("/account-status")
    fun getAccountConnectionStatus(): AccountConnectionStatusDto {
        return accountFacade.getAccountConnectionStatus()
    }
}