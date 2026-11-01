package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MailTemplateRendererTest : DescribeSpec({
    val sut = MailTemplateRenderer()
    val values = MailTemplateValues("달빛<b>포차", "{사장 이름}", 1042)

    describe("renderText") {
        it("세 값을 치환한다") {
            sut.renderText("{주점 이름}/{주점 번호}", values) shouldBe "달빛<b>포차/1042"
        }

        it("치환 결과를 다시 치환하지 않는다") {
            sut.renderText("{사장 이름}", values) shouldBe "{사장 이름}"
            sut.renderText("{사장 이름}/{주점 번호}", MailTemplateValues("주점", "{주점 번호}", 1)) shouldBe "{주점 번호}/1"
        }

        it("값에 달러 기호와 역슬래시가 있어도 그대로 넣는다") {
            sut.renderText("{주점 이름}", MailTemplateValues("\$1\\포차", "사장", 1)) shouldBe "\$1\\포차"
        }

        it("모르는 값은 그대로 둔다") {
            sut.renderText("{학교}", values) shouldBe "{학교}"
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

    describe("validate") {
        fun invalid(block: () -> Unit) = shouldThrow<CustomException>(block).errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID

        it("쓸 수 있는 값만 있으면 통과한다") {
            shouldNotThrowAny { sut.validate("{주점 이름} 안내", "{사장 이름}님 {주점 번호}", "설문", "https://x.com/?w={주점 번호}") }
            shouldNotThrowAny { sut.validate("제목", "내용", null, null) }
        }

        it("제목이나 내용에 모르는 값이 있으면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("{학교}", "내용", null, null) }
            invalid { sut.validate("제목", "{학교}", null, null) }
            invalid { sut.validate("제목", "내용", "{학교}", "https://x.com") }
            invalid { sut.validate("제목", "내용", "설문", "https://x.com/{학교}") }
        }

        it("https://가 아닌 링크면 MAIL_TEMPLATE_INVALID") {
            invalid { sut.validate("제목", "내용", "설문", "http://x.com") }
            invalid { sut.validate("제목", "내용", "설문", "javascript:alert(1)") }
            invalid { sut.validate("제목", "내용", "설문", "https://") }
            invalid { sut.validate("제목", "내용", "설문", " https://x.com") }
        }

        it("링크 호스트 자리에 값을 넣을 수 없다") {
            invalid { sut.validate("제목", "내용", "설문", "https://{주점 이름}.com") }
        }

        it("버튼 문구와 링크는 둘 다 있거나 둘 다 없어야 한다") {
            invalid { sut.validate("제목", "내용", "설문", null) }
            invalid { sut.validate("제목", "내용", null, "https://x.com") }
            invalid { sut.validate("제목", "내용", " ", "https://x.com") }
        }
    }
})
