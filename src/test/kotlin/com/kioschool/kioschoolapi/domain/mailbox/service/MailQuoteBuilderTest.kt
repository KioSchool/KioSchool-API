package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
import java.time.LocalDateTime

class MailQuoteBuilderTest : DescribeSpec({
    val sut = MailQuoteBuilder(MailHtmlSanitizer())

    fun message(
        direction: MailDirection = MailDirection.INBOUND,
        name: String? = "박지인",
        email: String? = "owner@test.com",
        text: String? = null,
        html: String? = null,
        sentAt: LocalDateTime? = LocalDateTime.of(2026, 11, 3, 8, 29),
    ) = MailMessage(
        resendEmailId = "e1",
        direction = direction,
        fromName = name,
        fromEmail = email,
        textBody = text,
        htmlBody = html,
        sentAt = sentAt,
    )

    describe("build") {
        it("보낸 시각과 사람을 머리글로 적고 내용을 인용 칸에 넣는다") {
            val quote = sut.build(message(html = "<p>이전 내용</p>", text = "이전 내용")).shouldNotBeNull()
            val document = Jsoup.parse(quote.html)

            document.selectFirst("p")!!.text() shouldBe "2026년 11월 3일 (화) 오전 8:29, 박지인 <owner@test.com>님이 작성:"
            document.selectFirst("blockquote")!!.text() shouldBe "이전 내용"
            document.selectFirst("blockquote")!!.attr("style") shouldContain "border-left"
        }

        it("글자 본문은 줄마다 인용 표시를 붙인다") {
            val quote = sut.build(message(text = "첫 줄\n\n> 더 이전 내용")).shouldNotBeNull()

            quote.text shouldBe "2026년 11월 3일 (화) 오전 8:29, 박지인 <owner@test.com>님이 작성:\n> 첫 줄\n>\n> > 더 이전 내용"
        }

        it("보낸 사람 이름의 태그를 글자로 적는다") {
            val quote = sut.build(message(name = "<img src=x onerror=alert(1)>", text = "내용")).shouldNotBeNull()

            Jsoup.parse(quote.html).select("img").size shouldBe 0
        }

        it("받은 메일의 스크립트와 스타일, 이벤트, 위험한 링크를 지운다") {
            val quote = sut.build(
                message(
                    html = """<html><head><style>p{color:red}</style><title>제목</title></head><body>
                        <script>alert(1)</script><p onclick="alert(1)" style="position:fixed">내용</p>
                        <a href="javascript:alert(1)">링크</a><img src="cid:inline"><img src="https://test.com/a.png">
                        </body></html>""",
                ),
            ).shouldNotBeNull()
            val document = Jsoup.parse(quote.html)

            quote.html shouldNotContain "alert"
            quote.html shouldNotContain "position:fixed"
            quote.html shouldNotContain "제목"
            quote.html shouldNotContain "cid:"
            document.select("img").size shouldBe 1
        }

        it("글자 본문만 있으면 줄바꿈을 살리고 태그를 글자로 적는다") {
            val quote = sut.build(message(text = "첫 줄\n<b>둘째 줄</b>")).shouldNotBeNull()
            val blockquote = Jsoup.parse(quote.html).selectFirst("blockquote")!!

            blockquote.select("br").size shouldBe 1
            blockquote.select("b").size shouldBe 0
            blockquote.text() shouldContain "<b>둘째 줄</b>"
        }

        it("HTML만 있으면 글자 본문을 HTML에서 뽑는다") {
            val quote = sut.build(message(html = "<p>첫 줄</p><p>둘째 줄</p>")).shouldNotBeNull()

            quote.text shouldContain "> 첫 줄"
            quote.text shouldContain "> 둘째 줄"
        }

        it("우리가 보낸 메일은 내용과 이전 인용만 가져오고 틀과 수신 거부는 뺀다") {
            val quote = sut.build(
                message(
                    direction = MailDirection.OUTBOUND,
                    html = """<table bgcolor="#f6f7f9"><tr><td>
                        <div data-mail-content><p>우리 답장</p></div>
                        <div data-mail-quote><p>머리글</p><blockquote>더 이전 내용</blockquote></div>
                        <p>수신 거부</p></td></tr></table>""",
                ),
            ).shouldNotBeNull()
            val outer = Jsoup.parse(quote.html).selectFirst("blockquote")!!

            outer.text() shouldContain "우리 답장"
            outer.text() shouldContain "더 이전 내용"
            outer.text() shouldNotContain "수신 거부"
            outer.select("table").size shouldBe 0
            outer.selectFirst("blockquote blockquote")!!.attr("style") shouldContain "border-left"
        }

        it("받은 메일은 표시 속성이 남아 있어도 전체를 가져온다") {
            val quote = sut.build(
                message(html = """<p>상대가 새로 쓴 내용</p><div data-mail-content><p>우리가 보낸 내용</p></div>"""),
            ).shouldNotBeNull()

            quote.html shouldContain "상대가 새로 쓴 내용"
            quote.html shouldContain "우리가 보낸 내용"
        }

        it("보낼 내용이 없으면 인용하지 않는다") {
            sut.build(message(html = "  ", text = null)).shouldBeNull()
        }
    }

    describe("visibleHtml") {
        val sent = """<html><body><div data-mail-content><p>새 내용</p></div><div data-mail-quote><p>이전 내용</p></div></body></html>"""

        it("보낸 메일에서는 우리가 붙인 인용을 뺀다") {
            val html = sut.visibleHtml(message(direction = MailDirection.OUTBOUND, html = sent)).shouldNotBeNull()

            html shouldContain "새 내용"
            html shouldNotContain "이전 내용"
        }

        it("받은 메일은 그대로 둔다") {
            sut.visibleHtml(message(direction = MailDirection.INBOUND, html = sent)) shouldBe sent
        }

        it("인용이 없는 메일은 그대로 둔다") {
            sut.visibleHtml(message(direction = MailDirection.OUTBOUND, html = "<p>내용</p>")) shouldBe "<p>내용</p>"
            sut.visibleHtml(message(direction = MailDirection.OUTBOUND, html = null)).shouldBeNull()
        }
    }
})
