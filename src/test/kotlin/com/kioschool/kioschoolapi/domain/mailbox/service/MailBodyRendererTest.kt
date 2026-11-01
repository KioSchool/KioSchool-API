package com.kioschool.kioschoolapi.domain.mailbox.service

import io.kotest.core.spec.style.DescribeSpec
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
    val sut = MailBodyRenderer(engine)

    describe("render") {
        it("내용의 <script>를 이스케이프하고 줄바꿈을 <br>로 바꾼다") {
            val html = sut.render("안녕하세요\r\n<script>alert(1)</script>")

            html shouldContain "안녕하세요<br>&lt;script&gt;alert(1)&lt;/script&gt;"
            html shouldNotContain "<script>"
        }

        it("버튼과 수신 거부 링크가 없으면 넣지 않는다") {
            val html = sut.render("내용")

            html shouldNotContain "수신 거부"
            html shouldNotContain "<a "
        }

        it("버튼 문구와 수신 거부 링크를 이스케이프해 넣는다") {
            val html = sut.render(
                "내용",
                MailButton("설문 <b>하기</b>", "https://x.com/?a=1&b=2"),
                "https://kio-school.com/mail/unsubscribe?token=abc"
            )

            html shouldContain "설문 &lt;b&gt;하기&lt;/b&gt;"
            html shouldContain "https://x.com/?a=1&amp;b=2"
            html shouldContain "https://kio-school.com/mail/unsubscribe?token=abc"
            html shouldContain "수신 거부"
        }
    }
})
