package com.kioschool.kioschoolapi.domain.mailbox.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

class MailBodyRendererTest : DescribeSpec({
    val engine = SpringTemplateEngine().apply {
        setTemplateResolver(
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "UTF-8"
                templateMode = TemplateMode.HTML
            }
        )
    }
    val sut = MailBodyRenderer(engine, MailHtmlSanitizer())

    describe("render") {
        it("메일 프로그램에서 보이도록 인라인 스타일을 넣는다") {
            val html = sut.render("<h2>제목</h2><p>본문</p><ul><li>항목</li></ul><hr>")

            html shouldContain """<h2 style="""
            html shouldContain """<p style="""
            html shouldContain """<ul style="""
            html shouldContain """<hr style="""
        }

        it("HTML을 정리해 스크립트를 넣지 않는다") {
            sut.render("<p>안녕</p><script>alert(1)</script>") shouldNotContain "<script>"
        }

        it("버튼 두 모양을 다르게 그리고 표시 속성을 지운다") {
            val html = sut.render(
                """<p><a data-mail-button="solid" href="https://x.com/a">진한</a><a data-mail-button="line" href="https://x.com/b">테두리</a></p>"""
            )

            html shouldContain "background:#eb6d09"
            html shouldContain "border:1px solid #eb6d09"
            html shouldNotContain "data-mail-button"
        }

        it("수신 거부 링크가 없으면 넣지 않는다") {
            sut.render("<p>내용</p>") shouldNotContain "수신 거부"
        }

        it("수신 거부 링크를 넣는다") {
            val html = sut.render("<p>내용</p>", "https://kio-school.com/mail/unsubscribe?token=abc")

            html shouldContain "https://kio-school.com/mail/unsubscribe?token=abc"
            html shouldContain "수신 거부"
        }
    }

    describe("renderText") {
        it("문단과 목록, 구분선을 줄바꿈으로 바꾼다") {
            sut.renderText("<h2>제목</h2><p>첫 줄<br>둘째 줄</p><ul><li><p>항목</p></li></ul><ol><li>하나</li></ol><hr><p>끝</p>") shouldBe
                "제목\n\n첫 줄\n둘째 줄\n\n- 항목\n\n1. 하나\n\n---\n\n끝"
        }

        it("링크와 버튼의 주소를 글자 뒤에 적는다") {
            sut.renderText("""<p><a href="https://x.com">사이트</a></p><p><a data-mail-button="solid" href="https://y.com">설문</a></p>""") shouldBe
                "사이트 (https://x.com)\n\n설문: https://y.com"
        }

        it("글자가 주소와 같으면 한 번만 적는다") {
            sut.renderText("""<p><a href="https://x.com">https://x.com</a></p>""") shouldBe "https://x.com"
        }

        it("HTML을 문자 그대로 적지 않는다") {
            sut.renderText("<p>a &amp; b &lt;c&gt;</p>") shouldBe "a & b <c>"
        }
    }
})
