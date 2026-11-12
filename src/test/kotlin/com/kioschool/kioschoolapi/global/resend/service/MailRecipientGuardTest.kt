package com.kioschool.kioschoolapi.global.resend.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MailRecipientGuardTest : DescribeSpec({
    describe("isAllowed") {
        it("*이면 모두 허용한다") {
            MailRecipientGuard("*").isAllowed("a@konkuk.ac.kr") shouldBe true
        }

        it("설정이 비어 있으면 모두 막는다") {
            MailRecipientGuard("").isAllowed("a@konkuk.ac.kr") shouldBe false
            MailRecipientGuard(" ").isAllowed("a@kio-school.com") shouldBe false
        }

        it("허용 도메인이 있으면 그 밖은 막는다") {
            val sut = MailRecipientGuard("kio-school.com")

            sut.isAllowed("a@KIO-school.com") shouldBe true
            sut.isAllowed("a@konkuk.ac.kr") shouldBe false
        }

        it("허용 도메인을 쉼표로 여러 개 받는다") {
            val sut = MailRecipientGuard("kio-school.com, example.org")

            sut.isAllowed("a@example.org") shouldBe true
        }

        it("도메인이 비슷한 주소를 허용하지 않는다") {
            val sut = MailRecipientGuard("kio-school.com")

            sut.isAllowed("a@evil-kio-school.com") shouldBe false
            sut.isAllowed("a@kio-school.com.evil.org") shouldBe false
            sut.isAllowed("kio-school.com") shouldBe false
        }
    }
})
