package com.kioschool.kioschoolapi.domain.account.controller

import com.kioschool.kioschoolapi.domain.account.dto.AddBankRequestBody
import com.kioschool.kioschoolapi.domain.account.dto.BankDto
import com.kioschool.kioschoolapi.domain.account.dto.DeleteBankRequestBody
import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

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
        return accountFacade.getBanks(name, page, size).map { BankDto.of(it) }
    }

    @Operation(summary = "은행 추가", description = "키오스쿨에서 등록 가능한 은행을 추가합니다.")
    @PostMapping("/bank")
    fun addBank(
        @RequestBody body: AddBankRequestBody
    ): BankDto {
        return BankDto.of(accountFacade.addBank(body.name, body.code))
    }

    @Operation(summary = "은행 삭제", description = "키오스쿨에서 등록 가능한 은행을 삭제합니다.")
    @DeleteMapping("/bank")
    fun deleteBank(
        @RequestBody body: DeleteBankRequestBody
    ): BankDto {
        return BankDto.of(accountFacade.deleteBank(body.id))
    }
}