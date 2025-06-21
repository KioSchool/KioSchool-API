package com.kioschool.kioschoolapi.domain.account.service

import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.account.entity.Bank
import com.kioschool.kioschoolapi.domain.account.repository.AccountRepository
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository
) {
    fun createAccount(bank: Bank, accountNumber: String, accountHolder: String): Account {
        val account = Account(
            bank = bank,
            accountNumber = accountNumber,
            accountHolder = accountHolder
        )

        return accountRepository.save(account)
    }

}