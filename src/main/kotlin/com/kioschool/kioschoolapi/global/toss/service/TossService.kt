package com.kioschool.kioschoolapi.global.toss.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.toss.exception.DifferentAccountNumberException
import org.springframework.stereotype.Service

@Service
class TossService {
    fun removeAmountQueryFromAccountUrl(accountUrl: String): String {
        return accountUrl.replace(Regex("amount=\\d+&"), "")
    }

    private fun getAccountNumberFromAccountUrl(accountUrl: String): String {
        val accountNoRegex = "accountNo=([^&]+)"
        val accountNoMatcher = Regex(accountNoRegex).find(accountUrl)
        return accountNoMatcher?.groupValues?.get(1) ?: ""
    }

    fun validateAccountUrl(user: User, accountUrl: String) {
        val tossAccountNumber = getAccountNumberFromAccountUrl(accountUrl)
        val userAccountNumber = user.account?.accountNumber

        if (tossAccountNumber != userAccountNumber) {
            throw DifferentAccountNumberException()
        }
    }
}