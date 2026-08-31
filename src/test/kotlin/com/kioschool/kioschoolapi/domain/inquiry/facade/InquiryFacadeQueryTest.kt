package com.kioschool.kioschoolapi.domain.inquiry.facade

import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryImage
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryEmailGuard
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.template.TemplateService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class InquiryFacadeQueryTest : DescribeSpec({
    val inquiryService = mockk<InquiryService>()
    val inquiryEmailGuard = mockk<InquiryEmailGuard>()
    val templateService = mockk<TemplateService>()
    val eventPublisher = mockk<ApplicationEventPublisher>()

    val sut = InquiryFacade(inquiryService, inquiryEmailGuard, templateService, eventPublisher)

    afterTest { clearAllMocks() }

    describe("getInquiries") {
        it("요약 DTO로 변환한다") {
            val inquiry = SampleEntity.newInquiry(id = 3L)
            every { inquiryService.getInquiries(null, 0, 20) } returns
                PageImpl(listOf(inquiry), PageRequest.of(0, 20), 1)

            val result = sut.getInquiries(null, 0, 20)

            result.content[0].id shouldBe 3L
            result.content[0].imageCount shouldBe 0
            result.content[0].status shouldBe InquiryStatus.PENDING
        }
    }

    describe("getInquiry") {
        it("storageKey 대신 계산된 accessUrl을 담는다") {
            val inquiry = SampleEntity.newInquiry(id = 3L)
            inquiry.images.add(
                InquiryImage(
                    inquiry = inquiry,
                    storageKey = "dev/inquiry/inquiry-3/uuid.png",
                    originalFileName = "shot.png",
                    contentType = "image/png",
                    size = 100L,
                )
            )
            every { inquiryService.getInquiry(3L) } returns inquiry
            every { inquiryService.getImageAccessUrl("dev/inquiry/inquiry-3/uuid.png") } returns
                "https://cdn/dev/inquiry/inquiry-3/uuid.png"

            val result = sut.getInquiry(3L)

            result.images.size shouldBe 1
            result.images[0].accessUrl shouldBe "https://cdn/dev/inquiry/inquiry-3/uuid.png"
            result.images[0].originalFileName shouldBe "shot.png"
            result.reply shouldBe null
        }
    }
})
