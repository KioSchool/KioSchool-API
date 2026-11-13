package com.kioschool.kioschoolapi.domain.mailbox.signature.service

import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureContact
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureValues
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

class MailSignatureRendererTest : DescribeSpec({
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
    val sut = MailSignatureRenderer(engine, "https://kio-school.com/")

    val full = MailSignatureValues(
        name = "박지인",
        englishName = "PARK JIN",
        organization = "키오스쿨",
        jobTitle = "운영 담당",
        contacts = listOf(MailSignatureContact("Email", "contact@kio-school.com"), MailSignatureContact("Web", "kio-school.com")),
        slogan = "축제 주점 주문을 더 간단하게",
        showLogo = true,
    )

    describe("render") {
        it("이름·소속·연락처·문구·로고를 그리고 글 버전을 만든다") {
            val block = sut.render(full).shouldNotBeNull()
            val doc = Jsoup.parseBodyFragment(block.html)

            doc.selectFirst("strong")!!.text() shouldBe "박지인"
            doc.text() shouldContain "PARK JIN"
            doc.text() shouldContain "키오스쿨 / 운영 담당"
            doc.text() shouldContain "축제 주점 주문을 더 간단하게"
            doc.selectFirst("img")!!.attr("src") shouldBe "https://kio-school.com/mail/kio-logo.png"
            block.text shouldBe "--\n박지인 PARK JIN\n키오스쿨 / 운영 담당\nEmail contact@kio-school.com\nWeb kio-school.com\n축제 주점 주문을 더 간단하게"
        }

        it("이메일은 mailto, 도메인은 https 링크로 감싸고 회색으로 칠한다") {
            val doc = Jsoup.parseBodyFragment(sut.render(full)!!.html)
            val links = doc.select("a")

            links.map { it.attr("href") } shouldBe listOf("mailto:contact@kio-school.com", "https://kio-school.com")
            links.forEach { it.attr("style") shouldContain "color:#888888" }
        }

        it("링크가 될 수 없는 값은 글 그대로 둔다") {
            val values = full.copy(contacts = listOf(MailSignatureContact("Tel", "010-1234-5678")))
            val doc = Jsoup.parseBodyFragment(sut.render(values)!!.html)

            doc.select("a").size shouldBe 0
            doc.text() shouldContain "010-1234-5678"
        }

        it("비운 칸은 줄째 그리지 않는다") {
            val values = MailSignatureValues(name = "키오스쿨 팀", englishName = " ", contacts = emptyList(), showLogo = false)
            val block = sut.render(values).shouldNotBeNull()
            val doc = Jsoup.parseBodyFragment(block.html)

            doc.text() shouldBe "키오스쿨 팀"
            doc.select("img").size shouldBe 0
            block.html shouldNotContain " / "
            block.text shouldBe "--\n키오스쿨 팀"
        }

        it("소속만 있으면 / 없이 그것만 쓴다") {
            val doc = Jsoup.parseBodyFragment(sut.render(MailSignatureValues(organization = "키오스쿨", showLogo = false))!!.html)

            doc.text() shouldBe "키오스쿨"
        }

        it("값을 이스케이프해 태그가 들어가지 않는다") {
            val html = sut.render(MailSignatureValues(name = "<script>x</script>", showLogo = false))!!.html

            html shouldNotContain "<script>"
            html shouldContain "&lt;script&gt;"
        }

        it("그릴 값이 하나도 없으면 null") {
            sut.render(MailSignatureValues(name = "", showLogo = false)).shouldBeNull()
        }
    }
})
