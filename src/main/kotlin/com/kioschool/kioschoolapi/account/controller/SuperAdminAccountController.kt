package com.kioschool.kioschoolapi.account.controller

import com.kioschool.kioschoolapi.account.dto.AddBankRequestBody
import com.kioschool.kioschoolapi.account.dto.DeleteBankRequestBody
import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.facade.AccountFacade
import com.kioschool.kioschoolapi.common.annotation.SuperAdminUsername
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
        @SuperAdminUsername username: String,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<Bank> {
        return accountFacade.getBanks(page, size)
    }

    @Operation(summary = "은행 추가", description = "키오스쿨에서 등록 가능한 은행을 추가합니다.")
    @PostMapping("/bank")
    fun addBank(
        @SuperAdminUsername username: String,
        @RequestBody body: AddBankRequestBody
    ): Bank {
        return accountFacade.addBank(body.name, body.code)
    }

    @Operation(summary = "은행 삭제", description = "키오스쿨에서 등록 가능한 은행을 삭제합니다.")
    @DeleteMapping("/bank")
    fun deleteBank(
        @SuperAdminUsername username: String,
        @RequestBody body: DeleteBankRequestBody
    ) {
        accountFacade.deleteBank(body.id)
    }
}