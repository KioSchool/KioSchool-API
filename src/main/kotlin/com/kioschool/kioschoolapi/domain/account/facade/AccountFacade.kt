package com.kioschool.kioschoolapi.domain.account.facade

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountConnectionStatusDto
import com.kioschool.kioschoolapi.domain.account.dto.common.BankDto
import com.kioschool.kioschoolapi.domain.account.service.AccountService
import com.kioschool.kioschoolapi.domain.account.service.BankService
import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.portone.service.PortoneService
import com.kioschool.kioschoolapi.global.toss.service.TossService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AccountFacade(
    private val bankService: BankService,
    private val accountService: AccountService,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val portoneService: PortoneService,
    private val tossService: TossService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getBanks(name: String?, page: Int, size: Int): Page<BankDto> {
        return bankService.getBanks(name, page, size).map { BankDto.of(it) }
    }

    fun getAllBanks(): List<BankDto> {
        return bankService.getAllBanks().map { BankDto.of(it) }
    }

    fun addBank(name: String, code: String): BankDto {
        val bank = bankService.addBank(name, code)
        log.info("[AUDIT] action=ADD_BANK bankId={} name={} code={}", bank.id, bank.name, bank.code)
        return BankDto.of(bank)
    }

    fun updateBankTossName(id: Long, tossName: String): BankDto {
        val bank = bankService.updateTossName(id, tossName)
        log.info("[AUDIT] action=UPDATE_BANK_TOSS_NAME bankId={} tossName={}", id, tossName)
        return BankDto.of(bank)
    }

    fun deleteBankTossName(id: Long): BankDto {
        val bank = bankService.deleteTossName(id)
        log.info("[AUDIT] action=DELETE_BANK_TOSS_NAME bankId={}", id)
        return BankDto.of(bank)
    }

    fun deleteBank(id: Long): BankDto {
        val bank = bankService.deleteBank(id)
        log.info("[AUDIT] action=DELETE_BANK bankId={} name={} code={}", id, bank.name, bank.code)
        return BankDto.of(bank)
    }

    fun registerAccount(
        username: String,
        bankId: Long,
        accountNumber: String
    ): UserDto {
        val bank = bankService.getBank(bankId)
        val accountHolder = portoneService.getAccountHolder(bank.code, accountNumber)

        val user = userService.getUser(username)
        user.account = accountService.createAccount(bank, accountNumber, accountHolder)
        return UserDto.of(userService.saveUser(user))
    }

    fun registerTossAccount(username: String, accountUrl: String): UserDto {
        val user = userService.getUser(username)
        tossService.validateAccountUrl(user, accountUrl)
        user.account?.tossAccountUrl = tossService.removeAmountQueryFromAccountUrl(accountUrl)

        val bank = user.account?.bank
        if (bank != null) {
            tossService.extractBankNameFromUrl(accountUrl)
                ?.let { bankService.fillTossNameIfAbsent(bank, it) }
        }

        return UserDto.of(userService.saveUser(user))
    }

    fun registerTossAccountAuto(username: String): UserDto {
        val user = userService.getUser(username)
        val account = user.account ?: throw IllegalStateException("계좌가 등록되어 있지 않습니다.")
        val tossName = account.bank.tossName ?: throw CustomException(ErrorCode.BANK_TOSS_NAME_NOT_FOUND)
        account.tossAccountUrl = tossService.generateTossAccountUrl(tossName, account.accountNumber)
        return UserDto.of(userService.saveUser(user))
    }

    fun deleteAccount(username: String): UserDto {
        val user = userService.getUser(username)
        accountService.deleteAccount(user)
        return UserDto.of(userService.saveUser(user))
    }

    fun deleteTossAccount(username: String): UserDto {
        val user = userService.getUser(username)
        user.account?.tossAccountUrl = null
        return UserDto.of(userService.saveUser(user))
    }

    fun getAccountConnectionStatus(): AccountConnectionStatusDto {
        val total = userRepository.count()
        val withAccount = userRepository.countUsersWithAccount()
        val withoutAccount = userRepository.countUsersWithoutAccount()
        val withToss = userRepository.countUsersWithTossAccount()
        val rate = if (total > 0) withAccount.toDouble() / total else 0.0
        val tossRateOfTotal = if (total > 0) withToss.toDouble() / total else 0.0
        val tossRateOfAccount = if (withAccount > 0) withToss.toDouble() / withAccount else 0.0

        return AccountConnectionStatusDto(
            totalUsers = total,
            usersWithAccount = withAccount,
            usersWithoutAccount = withoutAccount,
            connectionRate = rate,
            usersWithToss = withToss,
            tossRateOfTotal = tossRateOfTotal,
            tossRateOfAccount = tossRateOfAccount
        )
    }
}