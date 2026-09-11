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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.*

class InquiryServiceQueryTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val s3Service = mockk<S3Service>()
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()

    val sut = InquiryService(
        "test", 180L, 90L,
        inquiryRepository, s3Service, userService, emailService
    )

    afterTest { clearAllMocks() }

    describe("getInquiries") {
        it("createdAt DESC, id DESC로 정렬한다") {
            val slot = slot<Pageable>()
            every { inquiryRepository.findAll(capture(slot)) } returns PageImpl(emptyList())

            sut.getInquiries(null, 0, 20)

            slot.captured.sort shouldBe Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        }

        it("페이지 크기를 100으로 제한한다") {
            val slot = slot<Pageable>()
            every { inquiryRepository.findAll(capture(slot)) } returns PageImpl(emptyList())

            sut.getInquiries(null, 0, 500)

            slot.captured.pageSize shouldBe 100
        }

        it("status가 있으면 상태로 필터한다") {
            every {
                inquiryRepository.findAllByStatus(InquiryStatus.CLOSED, any())
            } returns PageImpl(emptyList())

            sut.getInquiries(InquiryStatus.CLOSED, 0, 20)

            verify(exactly = 1) { inquiryRepository.findAllByStatus(InquiryStatus.CLOSED, any()) }
            verify(exactly = 0) { inquiryRepository.findAll(any<Pageable>()) }
        }
    }

    describe("getInquiry") {
        it("없으면 INQUIRY_NOT_FOUND") {
            every { inquiryRepository.findById(99L) } returns Optional.empty()

            shouldThrow<CustomException> { sut.getInquiry(99L) }
                .errorCode shouldBe ErrorCode.INQUIRY_NOT_FOUND
        }
    }
})
