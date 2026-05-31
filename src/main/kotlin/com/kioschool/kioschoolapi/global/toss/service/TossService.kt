package com.kioschool.kioschoolapi.global.toss.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.toss.exception.DifferentAccountNumberException
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class TossService {

    fun generateTossAccountUrl(tossName: String, accountNumber: String): String {
        val encodedBankName = URLEncoder.encode(tossName, StandardCharsets.UTF_8)
        return "supertoss://send?bank=$encodedBankName&accountNo=$accountNumber&origin=qr"
    }

    fun removeAmountQueryFromAccountUrl(accountUrl: String): String {
        return accountUrl.replace(Regex("amount=\\d+&"), "")
    }

    private fun getAccountNumberFromAccountUrl(accountUrl: String): String {
        val accountNoRegex = "accountNo=([^&]+)"
        val accountNoMatcher = Regex(accountNoRegex).find(accountUrl)
        return accountNoMatcher?.groupValues?.get(1) ?: ""
    }

    fun extractBankNameFromUrl(accountUrl: String): String? {
        val match = Regex("bank=([^&]+)").find(accountUrl) ?: return null
        return URLDecoder.decode(match.groupValues[1], StandardCharsets.UTF_8)
    }

    fun validateAccountUrl(user: User, accountUrl: String) {
        val tossAccountNumber = getAccountNumberFromAccountUrl(accountUrl)
        val userAccountNumber = user.account?.accountNumber

        if (tossAccountNumber != userAccountNumber) {
            throw DifferentAccountNumberException()
        }
    }
}