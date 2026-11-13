package com.kioschool.kioschoolapi.global.kakaopay.service

import org.springframework.stereotype.Service

@Service
class KakaoPayService {

    // 카카오페이 계좌 송금 링크. 공식 문서가 없는 형식이라 카카오가 바꾸면 여기만 고친다.
    // 손님 화면이 뒤에 &amount=를 붙여 연다(토스 링크와 같음). 계좌번호에 하이픈이 있으면 열리지 않는다.
    fun generateKakaoPayAccountUrl(bankCode: String, accountNumber: String): String {
        val digits = accountNumber.filter { it.isDigit() }
        return "$KAKAOPAY_SEND_URL_PREFIX?bank_code=$bankCode&bank_account_number=$digits"
    }

    companion object {
        const val KAKAOPAY_SEND_URL_PREFIX = "https://link.kakaopay.com/t/money/to/bank"
    }
}
