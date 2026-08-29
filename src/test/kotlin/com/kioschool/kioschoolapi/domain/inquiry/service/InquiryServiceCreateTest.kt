package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.inquiry.util.InquiryImageValidator
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

class InquiryServiceCreateTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val s3Service = mockk<S3Service>()
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()

    val sut = InquiryService(
        "test", 180L, 90L,
        inquiryRepository, s3Service, userService, emailService
    )

    fun validatedImage(name: String) = InquiryImageValidator.ValidatedImage(
        file = MockMultipartFile("imageFiles", name, "image/png", ByteArray(10)),
        contentType = "image/png",
        extension = "png",
    )

    afterTest { clearAllMocks() }

    describe("createInquiry") {
        it("PENDING 상태와 미답변 보관기간으로 저장한다") {
            val saved = SampleEntity.newInquiry(id = 1L)
            val slot = slot<Inquiry>()
            every { inquiryRepository.save(capture(slot)) } returns saved

            sut.createInquiry("제목", "본문", "a@b.com", emptyList())

            slot.captured.status shouldBe InquiryStatus.PENDING
            slot.captured.replyEmail shouldBe "a@b.com"
            // purgeAt = privacyAgreedAt + 180일
            slot.captured.purgeAt shouldBe slot.captured.privacyAgreedAt.plusDays(180)
        }

        it("이미지를 업로드하고 키를 엔티티에 담는다") {
            val saved = SampleEntity.newInquiry(id = 7L)
            every { inquiryRepository.save(any()) } returns saved
            every { s3Service.uploadMultipartFile(any(), any(), "image/png") } returns "https://cdn/x.png"

            val result = sut.createInquiry("제목", "본문", "a@b.com", listOf(validatedImage("shot.png")))

            result.images.size shouldBe 1
            result.images[0].storageKey shouldStartWith "test/inquiry/inquiry-7/"
            result.images[0].contentType shouldBe "image/png"
            result.images[0].originalFileName shouldBe "shot.png"
        }

        it("두 번째 업로드가 실패하면 첫 번째 키를 지우고 예외를 전파한다") {
            val saved = SampleEntity.newInquiry(id = 7L)
            every { inquiryRepository.save(any()) } returns saved
            every { s3Service.uploadMultipartFile(any(), any(), any()) } returnsMany
                listOf("https://cdn/1.png") andThenThrows RuntimeException("s3 down")
            every { s3Service.deleteByKey(any()) } just Runs

            shouldThrow<RuntimeException> {
                sut.createInquiry(
                    "제목", "본문", "a@b.com",
                    listOf(validatedImage("a.png"), validatedImage("b.png"))
                )
            }

            verify(exactly = 1) { s3Service.deleteByKey(match { it.startsWith("test/inquiry/inquiry-7/") }) }
        }

        it("트랜잭션이 롤백되면 업로드된 이미지를 정리한다") {
            val saved = SampleEntity.newInquiry(id = 9L)
            every { inquiryRepository.save(any()) } returns saved
            every { s3Service.uploadMultipartFile(any(), any(), any()) } returns "https://cdn/x.png"
            every { s3Service.deleteByKey(any()) } just Runs

            TransactionSynchronizationManager.initSynchronization()
            try {
                sut.createInquiry("제목", "본문", "a@b.com", listOf(validatedImage("shot.png")))

                val synchronizations = TransactionSynchronizationManager.getSynchronizations()
                synchronizations.forEach {
                    it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
                }

                verify(exactly = 1) { s3Service.deleteByKey(match { it.startsWith("test/inquiry/inquiry-9/") }) }
            } finally {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }

        it("트랜잭션이 커밋되면 업로드된 이미지를 정리하지 않는다") {
            val saved = SampleEntity.newInquiry(id = 10L)
            every { inquiryRepository.save(any()) } returns saved
            every { s3Service.uploadMultipartFile(any(), any(), any()) } returns "https://cdn/x.png"

            TransactionSynchronizationManager.initSynchronization()
            try {
                sut.createInquiry("제목", "본문", "a@b.com", listOf(validatedImage("shot.png")))

                val synchronizations = TransactionSynchronizationManager.getSynchronizations()
                synchronizations.forEach {
                    it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
                }

                verify(exactly = 0) { s3Service.deleteByKey(any()) }
            } finally {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }
    }
})
