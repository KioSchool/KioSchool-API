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
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
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
        amount: Int? = 2000,
        depositAmount: Int? = null
    ) = CustomerDonationClick(
        orderId = orderId,
        workspaceId = workspaceId,
        variant = "anchor",
        method = method,
        noteIndex = 0,
        amount = amount,
        depositAmount = depositAmount
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

        it("입금 확인은 확인된 클릭만 합산하고, 입금률은 입금 확인 주문 ÷ 클릭한 고유 주문이다") {
            val workspace = SampleEntity.workspaceWithId(7)
            every { customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) } returns listOf(
                click(LocalDateTime.of(2026, 9, 10, 20, 0), orderId = 1, workspaceId = 7, method = "account", amount = null),
                click(LocalDateTime.of(2026, 9, 10, 20, 1), orderId = 1, workspaceId = 7, method = "account", amount = null, depositAmount = 3000),
                click(LocalDateTime.of(2026, 9, 11, 21, 0), orderId = 2, workspaceId = 7, amount = 1000, depositAmount = 2000),
                click(LocalDateTime.of(2026, 9, 11, 21, 5), orderId = 3, workspaceId = 7, amount = 5000),
                click(LocalDateTime.of(2026, 9, 11, 21, 6), orderId = 4, workspaceId = 7, amount = 1000)
            )
            every { orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()) } returns 10L
            every { workspaceRepository.findAllById(listOf(7L)) } returns listOf(workspace)

            val result = sut.getCustomerClickStats(startDate, endDate)

            result.summary.depositedClicks shouldBe 2L
            result.summary.depositedOrders shouldBe 2L
            result.summary.depositAmountSum shouldBe 5000L
            result.summary.depositRatePerOrder shouldBe 0.5
            result.daily.map { it.date to it.depositAmountSum } shouldBe listOf("2026-09-10" to 3000L, "2026-09-11" to 2000L)
            result.topWorkspaces.single().depositAmountSum shouldBe 5000L
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

    describe("getCustomerClickItems") {
        it("그 영업일의 클릭을 시각 순으로 주점 이름과 함께 내려준다") {
            val workspace = SampleEntity.workspaceWithId(7)
            every {
                customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    LocalDateTime.of(2026, 9, 10, 9, 0),
                    LocalDateTime.of(2026, 9, 11, 9, 0)
                )
            } returns listOf(
                click(LocalDateTime.of(2026, 9, 11, 1, 0), orderId = 2, workspaceId = 7),
                click(LocalDateTime.of(2026, 9, 10, 20, 0), orderId = 1, workspaceId = 99, depositAmount = 2000)
            )
            every { workspaceRepository.findAllById(listOf(99L, 7L)) } returns listOf(workspace)

            val items = sut.getCustomerClickItems(startDate)

            items.map { it.orderId } shouldBe listOf(1L, 2L)
            items.first().workspaceName shouldBe null
            items.first().depositAmount shouldBe 2000
            items.last().workspaceName shouldBe workspace.name
        }
    }

    describe("confirmDeposit") {
        it("금액·메모·확인 시각을 기록하고 메모 앞뒤 공백을 지운다") {
            val target = click(LocalDateTime.of(2026, 9, 10, 20, 0), method = "account", amount = null)
            val saved = slot<CustomerDonationClick>()
            every { customerDonationClickRepository.findByIdOrNull(1L) } returns target
            every { customerDonationClickRepository.save(capture(saved)) } answers { saved.captured }

            val item = sut.confirmDeposit(1L, 3000, "  홍*동 21:34 ")

            item.depositAmount shouldBe 3000
            item.depositMemo shouldBe "홍*동 21:34"
            (item.depositConfirmedAt != null) shouldBe true
            saved.captured.isDeposited shouldBe true
        }

        it("빈 메모는 null로 저장한다") {
            val target = click(LocalDateTime.of(2026, 9, 10, 20, 0))
            every { customerDonationClickRepository.findByIdOrNull(1L) } returns target
            every { customerDonationClickRepository.save(any()) } answers { firstArg() }

            sut.confirmDeposit(1L, 1000, "   ").depositMemo shouldBe null
        }

        it("금액이 1원 미만이면 INVALID_INPUT") {
            val exception = shouldThrow<CustomException> {
                sut.confirmDeposit(1L, 0, null)
            }
            exception.errorCode shouldBe ErrorCode.INVALID_INPUT
        }

        it("메모가 100자를 넘으면 INVALID_INPUT") {
            val exception = shouldThrow<CustomException> {
                sut.confirmDeposit(1L, 1000, "가".repeat(101))
            }
            exception.errorCode shouldBe ErrorCode.INVALID_INPUT
        }

        it("없는 클릭이면 DONATION_CLICK_NOT_FOUND") {
            every { customerDonationClickRepository.findByIdOrNull(404L) } returns null

            val exception = shouldThrow<CustomException> {
                sut.confirmDeposit(404L, 1000, null)
            }
            exception.errorCode shouldBe ErrorCode.DONATION_CLICK_NOT_FOUND
        }
    }

    describe("cancelDeposit") {
        it("입금 확인 정보를 모두 지워 미확인으로 되돌린다") {
            val target = click(LocalDateTime.of(2026, 9, 10, 20, 0)).apply {
                confirmDeposit(2000, "메모", LocalDateTime.of(2026, 9, 11, 10, 0))
            }
            every { customerDonationClickRepository.findByIdOrNull(1L) } returns target
            every { customerDonationClickRepository.save(any()) } answers { firstArg() }

            val item = sut.cancelDeposit(1L)

            item.depositAmount shouldBe null
            item.depositMemo shouldBe null
            item.depositConfirmedAt shouldBe null
        }
    }
})
