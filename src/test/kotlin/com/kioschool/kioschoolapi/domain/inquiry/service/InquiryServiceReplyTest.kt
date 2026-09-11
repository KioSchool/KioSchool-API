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

class InquiryServiceReplyTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val s3Service = mockk<S3Service>()
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()

    val sut = InquiryService(
        "test", 180L, 90L,
        inquiryRepository, s3Service, userService, emailService
    )

    beforeTest {
        every { userService.getUser("admin") } returns SampleEntity.userWithId(1L)
        every { inquiryRepository.save(any()) } answers { firstArg() }
    }

    afterTest { clearAllMocks() }

    describe("replyToInquiry") {
        it("발송에 성공하면 ANSWERED로 바꾸고 파기 예정일을 90일 뒤로 당긴다") {
            val inquiry = SampleEntity.newInquiry(id = 5L, replyEmail = "a@b.com")
            every { inquiryRepository.findByIdForUpdate(5L) } returns inquiry
            every { emailService.sendEmailSync("a@b.com", "답변드립니다", "<p>html</p>") } just Runs

            val result = sut.replyToInquiry("admin", 5L, "답변드립니다", "본문", "<p>html</p>")

            result.status shouldBe InquiryStatus.ANSWERED
            result.answeredAt shouldBe result.reply!!.sentAt
            result.purgeAt shouldBe result.reply!!.sentAt.plusDays(90)
            result.reply!!.recipientEmail shouldBe "a@b.com"
            result.reply!!.subject shouldBe "답변드립니다"
            result.reply!!.content shouldBe "본문"
        }

        it("요청에 담긴 수신자가 아니라 문의의 replyEmail로 보낸다") {
            val inquiry = SampleEntity.newInquiry(id = 5L, replyEmail = "real@b.com")
            every { inquiryRepository.findByIdForUpdate(5L) } returns inquiry
            every { emailService.sendEmailSync(any(), any(), any()) } just Runs

            sut.replyToInquiry("admin", 5L, "제목", "본문", "<p>html</p>")

            verify(exactly = 1) { emailService.sendEmailSync("real@b.com", "제목", "<p>html</p>") }
        }

        it("발송에 실패하면 예외가 전파되고 상태가 PENDING으로 남는다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            every { inquiryRepository.findByIdForUpdate(5L) } returns inquiry
            every { emailService.sendEmailSync(any(), any(), any()) } throws
                CustomException(ErrorCode.EMAIL_SEND_FAILURE)

            val ex = shouldThrow<CustomException> {
                sut.replyToInquiry("admin", 5L, "제목", "본문", "<p>html</p>")
            }

            ex.errorCode shouldBe ErrorCode.EMAIL_SEND_FAILURE
            inquiry.status shouldBe InquiryStatus.PENDING
            inquiry.reply shouldBe null
            verify(exactly = 0) { inquiryRepository.save(any()) }
        }

        it("없는 문의는 INQUIRY_NOT_FOUND") {
            every { inquiryRepository.findByIdForUpdate(99L) } returns null

            shouldThrow<CustomException> {
                sut.replyToInquiry("admin", 99L, "제목", "본문", "<p>html</p>")
            }.errorCode shouldBe ErrorCode.INQUIRY_NOT_FOUND
        }

        it("이미 답변한 문의는 INQUIRY_ALREADY_ANSWERED") {
            every { inquiryRepository.findByIdForUpdate(5L) } returns
                SampleEntity.newInquiry(id = 5L, status = InquiryStatus.ANSWERED)

            shouldThrow<CustomException> {
                sut.replyToInquiry("admin", 5L, "제목", "본문", "<p>html</p>")
            }.errorCode shouldBe ErrorCode.INQUIRY_ALREADY_ANSWERED

            verify(exactly = 0) { emailService.sendEmailSync(any(), any(), any()) }
        }

        it("종결된 문의는 INQUIRY_ALREADY_CLOSED") {
            every { inquiryRepository.findByIdForUpdate(5L) } returns
                SampleEntity.newInquiry(id = 5L, status = InquiryStatus.CLOSED)

            shouldThrow<CustomException> {
                sut.replyToInquiry("admin", 5L, "제목", "본문", "<p>html</p>")
            }.errorCode shouldBe ErrorCode.INQUIRY_ALREADY_CLOSED
        }
    }
})
