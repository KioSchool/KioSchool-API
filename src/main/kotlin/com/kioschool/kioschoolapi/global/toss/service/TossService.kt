package com.kioschool.kioschoolapi.global.toss.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Service
class TossService {

    // 은행 이름(bank=) 대신 금융결제원 은행 코드(bankCode=)를 넘기면 토스 명칭이 없는 은행도 열린다.
    // 손님 화면이 뒤에 &amount=를 붙여 연다.
    fun generateTossAccountUrl(bankCode: String, accountNumber: String): String {
        val digits = accountNumber.filter { it.isDigit() }
        return "${TOSS_SEND_URL_PREFIX}bankCode=$bankCode&accountNo=$digits&origin=qr"
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
        // 손님 화면이 이 링크를 그대로 연다. 토스 송금 QR 링크만 받는다.
        if (!accountUrl.startsWith(TOSS_SEND_URL_PREFIX)) {
            throw CustomException(ErrorCode.INVALID_TOSS_ACCOUNT_URL)
        }

        val tossAccountNumber = getAccountNumberFromAccountUrl(accountUrl)
        val userAccountNumber = user.account?.accountNumber

        if (tossAccountNumber != userAccountNumber) {
            throw CustomException(ErrorCode.DIFFERENT_ACCOUNT_NUMBER)
        }
    }

    companion object {
        const val TOSS_SEND_URL_PREFIX = "supertoss://send?"
    }
}