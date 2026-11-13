package com.kioschool.kioschoolapi.kakaopay.service

import com.kioschool.kioschoolapi.global.kakaopay.service.KakaoPayService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class KakaoPayServiceTest : DescribeSpec({
    val sut = KakaoPayService()

    describe("generateKakaoPayAccountUrl") {
        it("은행 코드와 계좌번호로 금액 없는 송금 링크를 만든다") {
            sut.generateKakaoPayAccountUrl("004", "12345678901234") shouldBe
                "https://link.kakaopay.com/t/money/to/bank?bank_code=004&bank_account_number=12345678901234"
        }

        it("계좌번호의 하이픈과 공백을 뺀다") {
            sut.generateKakaoPayAccountUrl("088", "110-123 456789") shouldBe
                "https://link.kakaopay.com/t/money/to/bank?bank_code=088&bank_account_number=110123456789"
        }
    }
})
