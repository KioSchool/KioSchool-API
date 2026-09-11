package com.kioschool.kioschoolapi.domain.inquiry.facade

import com.kioschool.kioschoolapi.domain.inquiry.dto.request.CreateInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryEmailGuard
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.template.TemplateService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockMultipartFile

class InquiryFacadeCreateTest : DescribeSpec({
    val inquiryService = mockk<InquiryService>()
    val inquiryEmailGuard = mockk<InquiryEmailGuard>()
    val templateService = mockk<TemplateService>()
    val eventPublisher = mockk<ApplicationEventPublisher>()

    val sut = InquiryFacade(inquiryService, inquiryEmailGuard, templateService, eventPublisher)

    fun body(
        title: String = "  결제 문의  ",
        content: String = "  본문  ",
        replyEmail: String = "  Customer@Example.COM  ",
    ) = CreateInquiryRequestBody(title, content, replyEmail, true)

    beforeTest {
        every { inquiryEmailGuard.check(any()) } just Runs
        // ApplicationEventPublisher is a @FunctionalInterface with two overloads:
        // publishEvent(ApplicationEvent) (default method) and publishEvent(Object) (the
        // abstract SAM). InquiryCreatedEvent is a plain class, not an ApplicationEvent, so
        // InquiryFacade's real call always resolves to the Object overload. A bare `any()`
        // here resolves ambiguously to the *other* (ApplicationEvent) overload, leaving the
        // Object overload unstubbed and every real call throwing "no answer found". Pin the
        // type explicitly, matching the `any<Any>()` already used in the assertion below.
        every { eventPublisher.publishEvent(any<Any>()) } just Runs
    }

    afterTest { clearAllMocks() }

    describe("createInquiry") {
        it("공백을 제거하고 이메일을 소문자로 정규화해 넘긴다") {
            every {
                inquiryService.createInquiry("결제 문의", "본문", "customer@example.com", any())
            } returns SampleEntity.newInquiry(id = 1L)

            val result = sut.createInquiry(body(), null)

            result.id shouldBe 1L
            verify(exactly = 1) {
                inquiryService.createInquiry("결제 문의", "본문", "customer@example.com", any())
            }
        }

        it("가드를 정규화된 이메일로 확인한다") {
            every { inquiryService.createInquiry(any(), any(), any(), any()) } returns
                SampleEntity.newInquiry(id = 1L)

            sut.createInquiry(body(), null)

            verify(exactly = 1) { inquiryEmailGuard.check("customer@example.com") }
        }

        it("가드에 걸리면 S3 업로드 전에 중단한다") {
            every { inquiryEmailGuard.check(any()) } throws
                CustomException(ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED)

            shouldThrow<CustomException> {
                sut.createInquiry(body(), listOf(MockMultipartFile("imageFiles", "a.png", "image/png", ByteArray(10))))
            }

            verify(exactly = 0) { inquiryService.createInquiry(any(), any(), any(), any()) }
        }

        it("접수 후 이벤트를 발행한다") {
            every { inquiryService.createInquiry(any(), any(), any(), any()) } returns
                SampleEntity.newInquiry(id = 42L)

            sut.createInquiry(body(), null)

            verify(exactly = 1) { eventPublisher.publishEvent(any<Any>()) }
        }
    }
})
