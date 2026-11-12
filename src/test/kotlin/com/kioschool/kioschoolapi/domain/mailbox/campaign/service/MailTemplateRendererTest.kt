package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class MailTemplateRendererTest : DescribeSpec({
    val sut = MailTemplateRenderer(MailHtmlSanitizer())
    val stats = MailAudienceStats(
        schoolName = "가온대학교",
        tableCount = 20,
        orderCount = 1812,
        revenue = 12400000,
        operatingDays = 3,
        firstOperatingDate = LocalDate.of(2026, 9, 24),
        lastOperatingDate = LocalDate.of(2026, 9, 26),
        topMenu = "김치전",
    )
    val values = MailTemplateValues("달빛<b>포차", "{사장 이름}", 1042, stats)

    describe("renderText") {
        it("주점 값을 치환한다") {
            sut.renderText("{주점 이름}/{주점 번호}/{학교}/{테이블 수}", values) shouldBe "달빛<b>포차/1042/가온대학교/20"
        }

        it("고른 기간 값을 읽기 좋게 치환한다") {
            sut.renderText("{주문 수}건 {매출} {운영일}일 {첫 운영일}~{마지막 운영일} {인기 메뉴}", values) shouldBe
                "1,812건 12,400,000원 3일 9월 24일~9월 26일 김치전"
        }

        it("치환 결과를 다시 치환하지 않는다") {
            sut.renderText("{사장 이름}", values) shouldBe "{사장 이름}"
            sut.renderText("{사장 이름}/{주점 번호}", MailTemplateValues("주점", "{주점 번호}", 1)) shouldBe "{주점 번호}/1"
        }

        it("값에 달러 기호와 역슬래시가 있어도 그대로 넣는다") {
            sut.renderText("{주점 이름}", MailTemplateValues("\$1\\포차", "사장", 1)) shouldBe "\$1\\포차"
        }

        it("모르는 값은 그대로 둔다") {
            sut.renderText("{모름}", values) shouldBe "{모름}"
        }

        it("기간 값이 없으면 비워 둔다") {
            sut.renderText("[{인기 메뉴}][{첫 운영일}]", MailTemplateValues("주점", "사장", 1)) shouldBe "[][]"
        }
    }

    describe("renderUrl") {
        it("링크의 값은 URL 인코딩한다") {
            sut.renderUrl("https://x.com/?n={주점 이름}", values) shouldBe
                "https://x.com/?n=%EB%8B%AC%EB%B9%9B%3Cb%3E%ED%8F%AC%EC%B0%A8"
        }

        it("주점 번호를 넣고 공백은 %20으로 바꾼다") {
            sut.renderUrl("https://x.com/{주점 번호}?n={사장 이름}", MailTemplateValues("주점", "김 사장", 7)) shouldBe
                "https://x.com/7?n=%EA%B9%80%20%EC%82%AC%EC%9E%A5"
        }
    }

    describe("renderHtml") {
        it("글자 값은 이스케이프해 태그가 되지 않는다") {
            val html = sut.renderHtml("<p><strong>{주점 이름}</strong> 안내</p>", values)

            html shouldContain "달빛&lt;b&gt;포차"
            html shouldNotContain "달빛<b>포차"
        }

        it("링크의 값은 URL 인코딩해 넣는다") {
            val html = sut.renderHtml("""<p><a data-mail-button="solid" href="https://x.com/?n={주점 이름}&amp;w={주점 번호}">{주점 이름} 설문</a></p>""", values)

            html shouldContain "n=%EB%8B%AC%EB%B9%9B%3Cb%3E%ED%8F%AC%EC%B0%A8&amp;w=1042"
            html shouldContain "달빛&lt;b&gt;포차 설문"
        }

        it("치환 결과를 태그로 다시 읽지 않는다") {
            val hostile = MailTemplateValues("<script>alert(1)</script>", "사장", 1)

            sut.renderHtml("<p>{주점 이름}</p>", hostile) shouldNotContain "<script>"
        }
    }

    describe("validate") {
        fun invalid(block: () -> Unit) = shouldThrow<CustomException>(block).errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID

        it("쓸 수 있는 값만 있으면 통과한다") {
            shouldNotThrowAny {
                sut.validate(
                    "{주점 이름} 안내",
                    """<p>{사장 이름}님 {주문 수}건 {매출} {인기 메뉴}</p><a data-mail-button="solid" href="https://x.com/?w={주점 번호}">설문</a>""",
                )
            }
        }

        it("제목이나 내용, 링크에 모르는 값이 있으면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("{모름}", "<p>내용</p>") }
            invalid { sut.validate("제목", "<p>{모름}</p>") }
            invalid { sut.validate("제목", """<p><a href="https://x.com/{모름}">링크</a></p>""") }
        }

        it("값이 꾸미기로 쪼개졌으면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("제목", "<p>{주점 <strong>이름}</strong></p>") }
        }

        it("https가 아닌 링크면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("제목", """<p><a href="http://x.com">링크</a></p>""") }
            invalid { sut.validate("제목", """<p><a href="javascript:alert(1)">링크</a></p>""") }
        }

        it("링크 호스트 자리에 값을 넣을 수 없다") {
            invalid { sut.validate("제목", """<p><a href="https://{주점 이름}.com">링크</a></p>""") }
        }

        it("링크 주소에 공백이 있으면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("제목", """<p><a href="https://x.com/a b">링크</a></p>""") }
        }

        it("내용이 비어 있으면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("제목", "<p> </p>") }
        }
    }

    describe("validateValues") {
        it("내용이 비어 있어도 값과 링크만 보고 통과한다") {
            shouldNotThrowAny { sut.validateValues("제목", "<p></p>") }
        }

        it("모르는 값이 있으면 MAIL_TEMPLATE_INVALID") {
            shouldThrow<CustomException> { sut.validateValues("제목", "<p>{모름}</p>") }.errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID
        }
    }
})
