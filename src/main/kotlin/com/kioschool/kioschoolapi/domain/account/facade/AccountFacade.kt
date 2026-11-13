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
import com.kioschool.kioschoolapi.global.kakaopay.service.KakaoPayService
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
    private val tossService: TossService,
    private val kakaoPayService: KakaoPayService
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
        // 카카오페이 송금 링크는 하이픈이 있으면 열리지 않으므로 숫자만 저장한다.
        val digits = accountNumber.filter { it.isDigit() }
        val bank = bankService.getBank(bankId)
        val accountHolder = portoneService.getAccountHolder(bank.code, digits)

        val user = userService.getUser(username)
        // 계좌를 바꾸면 계좌 행이 새로 생긴다. 카카오페이를 켜 둔 운영진은 새 계좌로 링크를 다시 만들어 켜 둔 채로 둔다.
        val kakaoPayWasOn = user.account?.kakaoPayAccountUrl != null
        val account = accountService.createAccount(bank, digits, accountHolder)
        if (kakaoPayWasOn) {
            account.kakaoPayAccountUrl = kakaoPayService.generateKakaoPayAccountUrl(bank.code, digits)
        }
        user.account = account
        val saved = userService.saveUser(user)
        log.info(
            "[AUDIT] action=REGISTER_ACCOUNT loginId={} bankId={} accountLast4={} kakaoPay={}",
            username,
            bankId,
            digits.takeLast(4),
            kakaoPayWasOn
        )
        return UserDto.of(saved)
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

        val saved = userService.saveUser(user)
        // validateAccountUrl이 링크의 계좌번호를 등록 계좌와 맞춰 보므로 등록 계좌 끝자리를 남긴다.
        log.info(
            "[AUDIT] action=REGISTER_TOSS_ACCOUNT loginId={} accountLast4={}",
            username,
            user.account?.accountNumber?.takeLast(4)
        )
        return UserDto.of(saved)
    }

    fun registerTossAccountAuto(username: String): UserDto {
        val user = userService.getUser(username)
        val account = user.account ?: throw CustomException(ErrorCode.ACCOUNT_NOT_REGISTERED)
        account.tossAccountUrl = tossService.generateTossAccountUrl(account.bank.code, account.accountNumber)
        val saved = userService.saveUser(user)
        log.info(
            "[AUDIT] action=REGISTER_TOSS_ACCOUNT_AUTO loginId={} accountLast4={}",
            username,
            account.accountNumber.takeLast(4)
        )
        return UserDto.of(saved)
    }

    fun deleteAccount(username: String): UserDto {
        val user = userService.getUser(username)
        val accountLast4 = user.account?.accountNumber?.takeLast(4)
        accountService.deleteAccount(user)
        val saved = userService.saveUser(user)
        log.info("[AUDIT] action=DELETE_ACCOUNT loginId={} accountLast4={}", username, accountLast4)
        return UserDto.of(saved)
    }

    fun deleteTossAccount(username: String): UserDto {
        val user = userService.getUser(username)
        user.account?.tossAccountUrl = null
        val saved = userService.saveUser(user)
        log.info("[AUDIT] action=DELETE_TOSS_ACCOUNT loginId={}", username)
        return UserDto.of(saved)
    }

    fun registerKakaoPayAccount(username: String): UserDto {
        val user = userService.getUser(username)
        val account = user.account ?: throw CustomException(ErrorCode.ACCOUNT_NOT_REGISTERED)
        account.kakaoPayAccountUrl =
            kakaoPayService.generateKakaoPayAccountUrl(account.bank.code, account.accountNumber)
        val saved = userService.saveUser(user)
        log.info(
            "[AUDIT] action=REGISTER_KAKAOPAY_ACCOUNT loginId={} accountLast4={}",
            username,
            account.accountNumber.takeLast(4)
        )
        return UserDto.of(saved)
    }

    fun deleteKakaoPayAccount(username: String): UserDto {
        val user = userService.getUser(username)
        user.account?.kakaoPayAccountUrl = null
        val saved = userService.saveUser(user)
        log.info("[AUDIT] action=DELETE_KAKAOPAY_ACCOUNT loginId={}", username)
        return UserDto.of(saved)
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