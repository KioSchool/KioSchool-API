package com.kioschool.kioschoolapi.domain.mailbox.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MailHtmlSanitizerTest : DescribeSpec({
    val sut = MailHtmlSanitizer()

    describe("clean") {
        it("글자 꾸미기와 목록, 제목, 구분선을 남긴다") {
            val html = "<h2>제목</h2><p><strong>굵게</strong><em>기울임</em><u>밑줄</u></p><ul><li><p>항목</p></li></ul><ol><li>하나</li></ol><hr>"

            val cleaned = sut.clean(html)

            listOf("<h2>제목</h2>", "<strong>굵게</strong>", "<em>기울임</em>", "<u>밑줄</u>", "<ul>", "<ol>", "<li>", "<hr>").forEach {
                cleaned shouldContain it
            }
        }

        it("스크립트와 이미지, 스타일, 이벤트 속성을 지운다") {
            val cleaned = sut.clean(
                """<p style="color:red" onclick="x()">글<script>alert(1)</script><img src="https://x.com/a.png" onerror="y()"><iframe src="https://x.com"></iframe></p>"""
            )

            cleaned shouldBe "<p>글</p>"
        }

        it("한글을 문자 참조로 바꾸지 않는다") {
            sut.clean("<p>달빛포차</p>") shouldBe "<p>달빛포차</p>"
        }

        it("https 링크와 값이 들어간 링크를 남긴다") {
            sut.clean("""<a href="https://x.com/?ws={주점 번호}&n=1">설문</a>""") shouldContain """href="https://x.com/?ws={주점 번호}&amp;n=1""""
        }

        it("https가 아닌 링크의 주소를 지운다") {
            listOf("http://x.com", "javascript:alert(1)", "data:text/html,x", "/relative").forEach {
                sut.clean("""<a href="$it">링크</a>""") shouldNotContain "href"
            }
        }

        it("rel과 target을 지운다") {
            sut.clean("""<a href="https://x.com" rel="opener" target="_top">링크</a>""") shouldBe """<a href="https://x.com">링크</a>"""
        }

        it("버튼 표시는 solid와 line만 남긴다") {
            sut.clean("""<a data-mail-button="solid" href="https://x.com">설문</a>""") shouldContain """data-mail-button="solid""""
            sut.clean("""<a data-mail-button="line" href="https://x.com">설문</a>""") shouldContain """data-mail-button="line""""
            sut.clean("""<a data-mail-button="evil" href="https://x.com">설문</a>""") shouldNotContain "data-mail-button"
        }
    }

    describe("isBlank") {
        it("글자가 없으면 true") {
            sut.isBlank("") shouldBe true
            sut.isBlank("<p></p>") shouldBe true
            sut.isBlank("<p> &nbsp; </p><hr>") shouldBe true
        }

        it("글자가 있으면 false") {
            sut.isBlank("<p>안녕</p>") shouldBe false
        }

        it("주소 있는 버튼만 있어도 false") {
            sut.isBlank("""<a data-mail-button="solid" href="https://x.com"></a>""") shouldBe false
        }
    }

    describe("hasInvalidLink") {
        it("주소가 https가 아니어서 지워진 링크가 있으면 true") {
            sut.hasInvalidLink("""<p><a href="http://x.com">링크</a></p>""") shouldBe true
        }

        it("https 링크만 있으면 false") {
            sut.hasInvalidLink("""<p><a href="https://x.com">링크</a></p>""") shouldBe false
        }
    }
})
