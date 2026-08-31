package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class InquiryServiceCloseTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val s3Service = mockk<S3Service>()
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()

    val sut = InquiryService(
        "test", 180L, 90L,
        inquiryRepository, s3Service, userService, emailService
    )

    beforeTest { every { inquiryRepository.save(any()) } answers { firstArg() } }
    afterTest { clearAllMocks() }

    describe("closeInquiry") {
        it("CLOSED로 바꾸고 사유를 남기며 파기 예정일을 90일 뒤로 잡는다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            every { inquiryRepository.findByIdForUpdate(5L) } returns inquiry

            val result = sut.closeInquiry(5L, "스팸")

            result.status shouldBe InquiryStatus.CLOSED
            result.closedReason shouldBe "스팸"
            result.purgeAt shouldBe result.closedAt!!.plusDays(90)
        }

        it("사유 없이도 종결할 수 있다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            every { inquiryRepository.findByIdForUpdate(5L) } returns inquiry

            sut.closeInquiry(5L, null).closedReason shouldBe null
        }

        it("종결해도 메일은 보내지 않는다") {
            every { inquiryRepository.findByIdForUpdate(5L) } returns SampleEntity.newInquiry(id = 5L)

            sut.closeInquiry(5L, null)

            verify(exactly = 0) { emailService.sendEmailSync(any(), any(), any()) }
        }

        it("이미 종결된 문의는 INQUIRY_ALREADY_CLOSED") {
            every { inquiryRepository.findByIdForUpdate(5L) } returns
                SampleEntity.newInquiry(id = 5L, status = InquiryStatus.CLOSED)

            shouldThrow<CustomException> { sut.closeInquiry(5L, null) }
                .errorCode shouldBe ErrorCode.INQUIRY_ALREADY_CLOSED
        }

        it("이미 답변한 문의는 INQUIRY_ALREADY_ANSWERED") {
            every { inquiryRepository.findByIdForUpdate(5L) } returns
                SampleEntity.newInquiry(id = 5L, status = InquiryStatus.ANSWERED)

            shouldThrow<CustomException> { sut.closeInquiry(5L, null) }
                .errorCode shouldBe ErrorCode.INQUIRY_ALREADY_ANSWERED
        }

        it("없는 문의는 INQUIRY_NOT_FOUND") {
            every { inquiryRepository.findByIdForUpdate(99L) } returns null

            shouldThrow<CustomException> { sut.closeInquiry(99L, null) }
                .errorCode shouldBe ErrorCode.INQUIRY_NOT_FOUND
        }
    }
})
