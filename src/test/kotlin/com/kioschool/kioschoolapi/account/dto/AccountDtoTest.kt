package com.kioschool.kioschoolapi.account.dto

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.account.entity.Bank
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AccountDtoTest : DescribeSpec({
    describe("of") {
        it("카카오페이 링크를 손님 화면에 내려준다") {
            val url = "https://link.kakaopay.com/t/money/to/bank?bank_code=004&bank_account_number=12345678901234"
            val account = Account(
                bank = Bank(name = "KB국민은행", code = "004"),
                accountNumber = "12345678901234",
                accountHolder = "김운영",
                kakaoPayAccountUrl = url
            )

            AccountDto.of(account).kakaoPayAccountUrl shouldBe url
        }
    }
})
