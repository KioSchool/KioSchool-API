package com.kioschool.kioschoolapi.domain.mailbox.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
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

        it("한 줄에 놓인 버튼 사이만 띄운다") {
            val html = sut.render(
                """<p><a data-mail-button="solid" href="https://x.com/a">진한</a><a data-mail-button="line" href="https://x.com/b">테두리</a></p><p><a data-mail-button="solid" href="https://x.com/c">혼자</a></p>"""
            )

            html.split("margin-left:8px").size shouldBe 2
        }

        it("정렬 표시를 인라인 스타일로 바꾸고 표시 속성을 지운다") {
            val html = sut.render("""<p data-align="center">가운데</p><h2 data-align="right">오른쪽</h2><p>왼쪽</p>""")

            html shouldContain "margin:0 0 12px;text-align:center;\">가운데"
            html shouldContain "text-align:right;\">오른쪽"
            Jsoup.parse(html).select("p").first { it.text() == "왼쪽" }.attr("style") shouldNotContain "text-align"
            html shouldNotContain "data-align"
        }

        it("메일 프로그램이 body 스타일을 지워도 배경과 카드가 남도록 표로 감싼다") {
            val document = Jsoup.parse(sut.render("<p>본문</p>", "https://kio-school.com/mail/unsubscribe?token=abc"))
            val card = document.selectFirst("table[bgcolor=#f6f7f9] table[bgcolor=#ffffff] td")

            card.shouldNotBeNull()
            card.text() shouldContain "본문"
            card.text() shouldContain "수신 거부"
            card.attr("style") shouldContain "font-family"
            card.attr("bgcolor") shouldBe "#ffffff"
            card.attr("style") shouldContain "background-color:#ffffff"
            val page = document.selectFirst("table[bgcolor=#f6f7f9] > tbody > tr > td")
            page.shouldNotBeNull()
            page.attr("bgcolor") shouldBe "#f6f7f9"
            page.attr("style") shouldContain "background-color:#f6f7f9"
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

        it("한 줄에 놓인 버튼은 줄을 나눠 적는다") {
            sut.renderText(
                """<p><a data-mail-button="solid" href="https://x.com/a">설문</a><a data-mail-button="line" href="https://x.com/b">둘러보기</a></p>"""
            ) shouldBe "설문: https://x.com/a\n둘러보기: https://x.com/b"
        }

        it("글자가 주소와 같으면 한 번만 적는다") {
            sut.renderText("""<p><a href="https://x.com">https://x.com</a></p>""") shouldBe "https://x.com"
        }

        it("글자 모양이 바뀌는 자리의 띄어쓰기를 지우지 않는다") {
            sut.renderText("<p><strong>굵은 글 </strong>보통 글 </p>") shouldBe "굵은 글 보통 글"
        }

        it("HTML을 문자 그대로 적지 않는다") {
            sut.renderText("<p>a &amp; b &lt;c&gt;</p>") shouldBe "a & b <c>"
        }
    }
})
