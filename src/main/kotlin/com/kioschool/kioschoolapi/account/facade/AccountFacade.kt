package com.kioschool.kioschoolapi.account.facade

import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.service.BankService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AccountFacade(
    private val bankService: BankService
) {
    fun getBanks(page: Int, size: Int): Page<Bank> {
        return bankService.getBanks(page, size)
    }

    fun addBank(name: String, code: String): Bank {
        return bankService.addBank(name, code)
    }

    fun deleteBank(id: Long) {
        bankService.deleteBank(id)
    }
}