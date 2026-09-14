package com.kioschool.kioschoolapi.domain.donation.facade

import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import com.kioschool.kioschoolapi.domain.donation.repository.CustomerDonationClickRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class SuperAdminDonationFacadeTest : DescribeSpec({
    val customerDonationClickRepository = mockk<CustomerDonationClickRepository>()
    val orderRepository = mockk<OrderRepository>()
    val workspaceRepository = mockk<WorkspaceRepository>()

    val sut = SuperAdminDonationFacade(customerDonationClickRepository, orderRepository, workspaceRepository)

    fun click(
        at: LocalDateTime,
        orderId: Long? = null,
        workspaceId: Long? = null,
        method: String? = "toss",
        amount: Int? = 2000
    ) = CustomerDonationClick(
        orderId = orderId,
        workspaceId = workspaceId,
        variant = "anchor",
        method = method,
        noteIndex = 0,
        amount = amount
    ).apply { createdAt = at }

    val startDate = LocalDate.of(2026, 9, 10)
    val endDate = LocalDate.of(2026, 9, 11)
    val start = LocalDateTime.of(2026, 9, 10, 9, 0)
    val end = LocalDateTime.of(2026, 9, 12, 9, 0)

    afterTest {
        clearAllMocks()
    }

    describe("getCustomerClickStats") {
        it("시작일 09:00부터 종료일 다음 날 09:00 전까지 조회한다") {
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns emptyList()
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus.CANCELLED, start, end) } returns 0L
            every { workspaceRepository.findAllById(emptyList()) } returns emptyList()

            sut.getCustomerClickStats(startDate, endDate)

            verify { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) }
        }

        it("자정을 넘긴 클릭은 전날 영업일로 묶고, 클릭 없는 날도 0으로 채운다") {
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns listOf(
                click(LocalDateTime.of(2026, 9, 10, 23, 0), orderId = 1),
                click(LocalDateTime.of(2026, 9, 11, 2, 0), orderId = 2)
            )
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()) } returns 10L
            every { workspaceRepository.findAllById(emptyList()) } returns emptyList()

            val result = sut.getCustomerClickStats(startDate, endDate)

            result.daily.map { it.date to it.clicks } shouldBe listOf("2026-09-10" to 2L, "2026-09-11" to 0L)
        }

        it("같은 주문의 중복 클릭은 uniqueOrders에서 한 번만 세고, 클릭률 분모는 비취소 주문 수다") {
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns listOf(
                click(LocalDateTime.of(2026, 9, 10, 20, 0), orderId = 1, method = "account", amount = 1000),
                click(LocalDateTime.of(2026, 9, 10, 20, 1), orderId = 1, method = "toss", amount = 1000),
                click(LocalDateTime.of(2026, 9, 10, 21, 0), orderId = 2, method = "toss", amount = 5000),
                click(LocalDateTime.of(2026, 9, 10, 22, 0), orderId = null, method = "toss", amount = null)
            )
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus.CANCELLED, start, end) } returns 20L
            every { workspaceRepository.findAllById(emptyList()) } returns emptyList()

            val summary = sut.getCustomerClickStats(startDate, endDate).summary

            summary.totalClicks shouldBe 4L
            summary.uniqueOrders shouldBe 2L
            summary.clickedAmountSum shouldBe 7000L
            summary.averageAmount shouldBe 2333L
            summary.clickRatePerOrder shouldBe 0.1
        }

        it("수단별 분해는 클릭 수 내림차순이고 비율 합은 1이다") {
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns listOf(
                click(LocalDateTime.of(2026, 9, 10, 20, 0), method = "account"),
                click(LocalDateTime.of(2026, 9, 10, 20, 1), method = "toss"),
                click(LocalDateTime.of(2026, 9, 10, 20, 2), method = "toss"),
                click(LocalDateTime.of(2026, 9, 10, 20, 3), method = null)
            )
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()) } returns 0L
            every { workspaceRepository.findAllById(emptyList()) } returns emptyList()

            val result = sut.getCustomerClickStats(startDate, endDate)

            result.byMethod.first().key shouldBe "toss"
            result.byMethod.first().ratio shouldBe 0.5
            result.byMethod.sumOf { it.ratio } shouldBe 1.0
            result.summary.clickRatePerOrder shouldBe 0.0
        }

        it("주점별 순위는 클릭 수 기준이며 이름을 함께 내려준다") {
            val workspace = SampleEntity.workspaceWithId(7)
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns listOf(
                click(LocalDateTime.of(2026, 9, 10, 20, 0), orderId = 1, workspaceId = 7),
                click(LocalDateTime.of(2026, 9, 10, 20, 1), orderId = 2, workspaceId = 7),
                click(LocalDateTime.of(2026, 9, 10, 20, 2), orderId = 3, workspaceId = 99)
            )
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()) } returns 0L
            every { workspaceRepository.findAllById(listOf(7L, 99L)) } returns listOf(workspace)

            val top = sut.getCustomerClickStats(startDate, endDate).topWorkspaces

            top.map { it.workspaceId } shouldBe listOf(7L, 99L)
            top.first().workspaceName shouldBe workspace.name
            top.first().uniqueOrders shouldBe 2L
            top.last().workspaceName shouldBe null
        }

        it("시작일이 종료일보다 늦으면 INVALID_INPUT") {
            val exception = shouldThrow<CustomException> {
                sut.getCustomerClickStats(endDate, startDate)
            }
            exception.errorCode shouldBe ErrorCode.INVALID_INPUT
        }

        it("기간이 366일을 넘으면 INVALID_INPUT") {
            val exception = shouldThrow<CustomException> {
                sut.getCustomerClickStats(startDate, startDate.plusDays(366))
            }
            exception.errorCode shouldBe ErrorCode.INVALID_INPUT
        }
    }
})
