package com.kioschool.kioschoolapi.domain.inquiry.schedule

import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.inquiry.service.InquiryPurgeService
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class InquiryPurgeSchedulerTest : DescribeSpec({
    val inquiryRepository = mockk<InquiryRepository>()
    val inquiryPurgeService = mockk<InquiryPurgeService>()
    val sut = InquiryPurgeScheduler(inquiryRepository, inquiryPurgeService)

    afterTest { clearAllMocks() }

    describe("purgeExpiredInquiries") {
        it("만료된 문의를 하나씩 지운다") {
            every {
                inquiryRepository.findAllByPurgeAtBefore(any<LocalDateTime>(), any<Pageable>())
            } returns listOf(SampleEntity.newInquiry(id = 1L), SampleEntity.newInquiry(id = 2L))
            every { inquiryPurgeService.purgeOne(any()) } just Runs

            sut.purgeExpiredInquiries()

            verify(exactly = 1) { inquiryPurgeService.purgeOne(1L) }
            verify(exactly = 1) { inquiryPurgeService.purgeOne(2L) }
        }

        it("한 건이 실패해도 나머지를 계속 처리한다") {
            every {
                inquiryRepository.findAllByPurgeAtBefore(any<LocalDateTime>(), any<Pageable>())
            } returns listOf(SampleEntity.newInquiry(id = 1L), SampleEntity.newInquiry(id = 2L))
            every { inquiryPurgeService.purgeOne(1L) } throws RuntimeException("s3 down")
            every { inquiryPurgeService.purgeOne(2L) } just Runs

            shouldNotThrowAny { sut.purgeExpiredInquiries() }

            verify(exactly = 1) { inquiryPurgeService.purgeOne(2L) }
        }

        it("대상이 없으면 아무것도 하지 않는다") {
            every {
                inquiryRepository.findAllByPurgeAtBefore(any<LocalDateTime>(), any<Pageable>())
            } returns emptyList()

            sut.purgeExpiredInquiries()

            verify(exactly = 0) { inquiryPurgeService.purgeOne(any()) }
        }
    }
})
