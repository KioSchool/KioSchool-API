package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
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

    describe("renderPersonal") {
        it("배경과 카드 없이 보통 메일처럼 그린다") {
            val document = Jsoup.parse(sut.renderPersonal("<p>새 내용</p>"))

            document.selectFirst("[data-mail-content]")!!.text() shouldBe "새 내용"
            document.select("table").size shouldBe 0
            document.html() shouldNotContain "background"
            document.html() shouldNotContain "border-radius"
        }

        it("일반 링크 색은 메일 프로그램에 맡기고 버튼은 버튼으로 그린다") {
            val document = Jsoup.parse(
                sut.renderPersonal(
                    """<p><a href="https://a.com">링크</a></p><p><a href="https://b.com" data-mail-button="solid">버튼</a></p>""",
                ),
            )

            document.selectFirst("""a[href="https://a.com"]""")!!.attr("style") shouldBe ""
            document.selectFirst("""a[href="https://b.com"]""")!!.attr("style") shouldContain "background:#eb6d09"
        }

        it("내용 뒤에 인용을 넣고 수신 거부 문구는 넣지 않는다") {
            val html = sut.renderPersonal("<p>새 내용</p>", "<blockquote>이전 내용</blockquote>")
            val document = Jsoup.parse(html)

            document.selectFirst("[data-mail-content]")!!.text() shouldBe "새 내용"
            document.selectFirst("[data-mail-quote] blockquote")!!.text() shouldBe "이전 내용"
            html shouldNotContain "수신 거부"
        }

        it("인용이 없으면 인용 칸을 넣지 않는다") {
            val document = Jsoup.parse(sut.renderPersonal("<p>새 내용</p>"))

            document.select("[data-mail-quote]").size shouldBe 0
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

    describe("빈 줄") {
        it("편집기에서 엔터로 만든 빈 문단을 한 줄로 남긴다") {
            val doc = Jsoup.parse(sut.renderPersonal("<p>안녕하세요</p><p></p><p> </p><p>본문</p>"))
            val paragraphs = doc.select("[data-mail-content] > p")

            paragraphs.size shouldBe 4
            paragraphs[1].html() shouldBe "<br>"
            paragraphs[2].html() shouldBe "<br>"
            paragraphs[3].text() shouldBe "본문"
        }

        it("버튼만 있는 문단은 건드리지 않는다") {
            val html = sut.render("""<p><a data-mail-button="solid" href="https://x.com">설문</a></p>""")

            html shouldNotContain "<br>"
        }
    }

    describe("서명 자리") {
        val signature = MailSignatureBlock("""<div style="color:#888888;"><a href="mailto:a@b.com" style="color:#888888;">a@b.com</a></div>""", "--\n서명")

        it("첫 번째 서명 자리에 서명을 넣고 나머지 자리는 지운다") {
            val html = sut.renderPersonal("""<p>안녕</p><div data-mail-signature=""></div><div data-mail-signature=""></div>""", signature = signature)

            html.split("mailto:a@b.com\"").size shouldBe 2
            html shouldNotContain "data-mail-signature"
        }

        it("본문 링크 색이 서명 링크 스타일을 덮지 않는다") {
            val doc = Jsoup.parse(sut.render("""<p>안녕</p><div data-mail-signature=""></div>""", signature = signature))

            doc.selectFirst("a[href=mailto:a@b.com]")!!.attr("style") shouldBe "color:#888888;"
        }

        it("서명이 없으면 자리만 지운다") {
            val html = sut.renderPersonal("""<p>안녕</p><div data-mail-signature=""></div>""")

            html shouldNotContain "data-mail-signature"
            html shouldNotContain "mailto:"
        }

        it("글 버전에는 서명 글을 한 번만 넣는다") {
            sut.renderText("""<p>안녕</p><div data-mail-signature=""></div><div data-mail-signature=""></div>""", signature) shouldBe
                "안녕\n\n--\n서명"
        }
    }
})
