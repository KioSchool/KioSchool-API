package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryImage
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import java.util.*

class InquiryPurgeServiceTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val s3Service = mockk<S3Service>()
    val sut = InquiryPurgeService(inquiryRepository, s3Service)

    afterTest { clearAllMocks() }

    describe("purgeOne") {
        it("S3 이미지를 먼저 지우고 DB 행을 지운다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            inquiry.images.add(
                InquiryImage(inquiry, "dev/inquiry/inquiry-5/a.png", "a.png", "image/png", 10L)
            )
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { s3Service.deleteByKey("dev/inquiry/inquiry-5/a.png") } just Runs
            every { inquiryRepository.delete(inquiry) } just Runs

            sut.purgeOne(5L)

            verifyOrder {
                s3Service.deleteByKey("dev/inquiry/inquiry-5/a.png")
                inquiryRepository.delete(inquiry)
            }
        }

        it("S3 삭제가 실패하면 DB 행을 지우지 않는다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            inquiry.images.add(
                InquiryImage(inquiry, "dev/inquiry/inquiry-5/a.png", "a.png", "image/png", 10L)
            )
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { s3Service.deleteByKey(any()) } throws RuntimeException("s3 down")

            shouldThrow<RuntimeException> { sut.purgeOne(5L) }

            verify(exactly = 0) { inquiryRepository.delete(any()) }
        }

        it("이미지가 없으면 바로 지운다") {
            val inquiry = SampleEntity.newInquiry(id = 5L)
            every { inquiryRepository.findById(5L) } returns Optional.of(inquiry)
            every { inquiryRepository.delete(inquiry) } just Runs

            sut.purgeOne(5L)

            verify(exactly = 1) { inquiryRepository.delete(inquiry) }
            verify(exactly = 0) { s3Service.deleteByKey(any()) }
        }

        it("이미 사라진 문의는 조용히 넘어간다 (멱등)") {
            every { inquiryRepository.findById(5L) } returns Optional.empty()

            sut.purgeOne(5L)

            verify(exactly = 0) { inquiryRepository.delete(any()) }
        }
    }
})
