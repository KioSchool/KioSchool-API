package com.kioschool.kioschoolapi.domain.mailbox.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MailAddressParserTest : DescribeSpec({
    describe("parse") {
        it("따옴표로 싼 이름과 대문자 주소를 나눠 소문자로 저장한다") {
            val parsed = MailAddressParser.parse("\"박지인\" <A@Naver.com>")

            parsed.name shouldBe "박지인"
            parsed.email shouldBe "a@naver.com"
        }

        it("이름이 없으면 주소만 돌려준다") {
            val parsed = MailAddressParser.parse("a@b.com")

            parsed.name.shouldBeNull()
            parsed.email shouldBe "a@b.com"
        }

        it("꺾쇠만 있어도 주소를 읽는다") {
            val parsed = MailAddressParser.parse("<A@b.com>")

            parsed.name.shouldBeNull()
            parsed.email shouldBe "a@b.com"
        }

        it("인코딩된 이름을 풀어 준다") {
            MailAddressParser.parse("=?UTF-8?B?67CV7KeA7J24?= <a@b.com>").name shouldBe "박지인"
        }

        it("풀 수 없는 인코딩이면 받은 이름을 그대로 둔다") {
            MailAddressParser.parse("=?UNKNOWN?B?67CV?= <a@b.com>").name shouldBe "=?UNKNOWN?B?67CV?="
        }
    }
})
