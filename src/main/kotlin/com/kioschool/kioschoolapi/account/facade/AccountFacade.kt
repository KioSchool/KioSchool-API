package com.kioschool.kioschoolapi.account.facade

import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.service.AccountService
import com.kioschool.kioschoolapi.account.service.BankService
import com.kioschool.kioschoolapi.portone.service.PortoneService
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AccountFacade(
    private val bankService: BankService,
    private val accountService: AccountService,
    private val userService: UserService,
    private val portoneService: PortoneService
) {
    fun getBanks(page: Int, size: Int): Page<Bank> {
        return bankService.getBanks(page, size)
    }

    fun getAllBanks(): List<Bank> {
        return bankService.getAllBanks()
    }

    fun addBank(name: String, code: String): Bank {
        return bankService.addBank(name, code)
    }

    fun deleteBank(id: Long) {
        bankService.deleteBank(id)
    }

    fun registerAccount(
        username: String,
        bankId: Long,
        accountNumber: String,
        accountHolder: String
    ): User {
        val bank = bankService.getBank(bankId)
        portoneService.validateAccountHolder(bank.code, accountNumber, accountHolder)

        val user = userService.getUser(username)
        user.account = accountService.createAccount(bank, accountNumber, accountHolder)
        return userService.saveUser(user)
    }

    fun registerTossAccount(username: String, accountUrl: String): User {
        val user = userService.getUser(username)
        user.account?.tossAccountUrl = accountUrl
        return userService.saveUser(user)
    }
}