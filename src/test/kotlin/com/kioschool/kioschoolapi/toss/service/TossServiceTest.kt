package com.kioschool.kioschoolapi.toss.service

import com.kioschool.kioschoolapi.global.toss.service.TossService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TossServiceTest : DescribeSpec({
    val sut = TossService()

    describe("generateTossAccountUrl") {
        it("should encode Korean bank name") {
            val result = sut.generateTossAccountUrl("카카오뱅크", "3333280467267")
            result shouldBe "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%98%A4%EB%B1%85%ED%81%AC&accountNo=3333280467267&origin=qr"
        }

        it("should encode mixed English and Korean bank name") {
            val result = sut.generateTossAccountUrl("NH농협은행", "3521375642063")
            result shouldBe "supertoss://send?bank=NH%EB%86%8D%ED%98%91%EC%9D%80%ED%96%89&accountNo=3521375642063&origin=qr"
        }

        it("should encode bank name starting with English prefix") {
            val result = sut.generateTossAccountUrl("KB국민은행", "81870200066427")
            result shouldBe "supertoss://send?bank=KB%EA%B5%AD%EB%AF%BC%EC%9D%80%ED%96%89&accountNo=81870200066427&origin=qr"
        }
    }

    describe("extractBankNameFromUrl") {
        it("should decode Korean bank name from url") {
            val url = "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%98%A4%EB%B1%85%ED%81%AC&accountNo=3333280467267&origin=qr"
            sut.extractBankNameFromUrl(url) shouldBe "카카오뱅크"
        }

        it("should decode mixed English and Korean bank name from url") {
            val url = "supertoss://send?bank=NH%EB%86%8D%ED%98%91%EC%9D%80%ED%96%89&accountNo=3521375642063&origin=qr"
            sut.extractBankNameFromUrl(url) shouldBe "NH농협은행"
        }

        it("should return null when bank parameter is missing") {
            val url = "supertoss://send?accountNo=3333280467267&origin=qr"
            sut.extractBankNameFromUrl(url) shouldBe null
        }

        it("generateTossAccountUrl and extractBankNameFromUrl should be inverse operations") {
            val tossName = "IBK기업은행"
            val url = sut.generateTossAccountUrl(tossName, "12345")
            sut.extractBankNameFromUrl(url) shouldBe tossName
        }
    }
})
