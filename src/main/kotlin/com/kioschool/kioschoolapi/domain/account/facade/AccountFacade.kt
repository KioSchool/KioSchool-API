package com.kioschool.kioschoolapi.domain.account.facade

import com.kioschool.kioschoolapi.domain.account.dto.common.BankDto
import com.kioschool.kioschoolapi.domain.account.service.AccountService
import com.kioschool.kioschoolapi.domain.account.service.BankService
import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.global.portone.service.PortoneService
import com.kioschool.kioschoolapi.global.toss.service.TossService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AccountFacade(
    private val bankService: BankService,
    private val accountService: AccountService,
    private val userService: UserService,
    private val portoneService: PortoneService,
    private val tossService: TossService
) {
    fun getBanks(name: String?, page: Int, size: Int): Page<BankDto> {
        return bankService.getBanks(name, page, size).map { BankDto.of(it) }
    }

    fun getAllBanks(): List<BankDto> {
        return bankService.getAllBanks().map { BankDto.of(it) }
    }

    fun addBank(name: String, code: String): BankDto {
        return BankDto.of(bankService.addBank(name, code))
    }

    fun deleteBank(id: Long): BankDto {
        return BankDto.of(bankService.deleteBank(id))
    }

    fun registerAccount(
        username: String,
        bankId: Long,
        accountNumber: String,
        accountHolder: String
    ): UserDto {
        val bank = bankService.getBank(bankId)
        portoneService.validateAccountHolder(bank.code, accountNumber, accountHolder)

        val user = userService.getUser(username)
        user.account = accountService.createAccount(bank, accountNumber, accountHolder)
        return UserDto.of(userService.saveUser(user))
    }

    fun registerTossAccount(username: String, accountUrl: String): UserDto {
        val user = userService.getUser(username)
        tossService.validateAccountUrl(user, accountUrl)
        user.account?.tossAccountUrl = tossService.removeAmountQueryFromAccountUrl(accountUrl)
        return UserDto.of(userService.saveUser(user))
    }

    fun deleteAccount(username: String): UserDto {
        val user = userService.getUser(username)
        accountService.deleteAccount(user)
        return UserDto.of(userService.saveUser(user))
    }
}