package com.kioschool.kioschoolapi.global.template

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

class TemplateServiceInquiryTest : DescribeSpec({
    val templateEngine = mockk<SpringTemplateEngine>()
    val sut = TemplateService("https://kio-school.com", templateEngine)

    afterTest { clearAllMocks() }

    describe("getInquiryReceivedEmailTemplate") {
        it("문의 번호와 제목을 컨텍스트에 담는다") {
            val contextSlot = slot<Context>()
            every {
                templateEngine.process("inquiryReceivedEmail", capture(contextSlot))
            } returns "rendered"

            sut.getInquiryReceivedEmailTemplate(123L, "결제 문의") shouldBe "rendered"

            contextSlot.captured.getVariable("inquiryId") shouldBe 123L
            contextSlot.captured.getVariable("title") shouldBe "결제 문의"
        }
    }

    describe("getInquiryReplyEmailTemplate") {
        it("답변 본문을 컨텍스트에 담는다") {
            val contextSlot = slot<Context>()
            every {
                templateEngine.process("inquiryReplyEmail", capture(contextSlot))
            } returns "rendered"

            sut.getInquiryReplyEmailTemplate("확인했습니다.\n감사합니다.") shouldBe "rendered"

            contextSlot.captured.getVariable("content") shouldBe "확인했습니다.\n감사합니다."
        }
    }
})
