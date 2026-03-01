package com.kioschool.kioschoolapi.domain.statistics

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.statistics.service.StatisticsCalculator
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class StatisticsCalculatorTest : BehaviorSpec({
    val orderRepository = mockk<OrderRepository>()
    val orderSessionRepository = mockk<OrderSessionRepository>()
    val dailyOrderStatisticRepository = mockk<DailyOrderStatisticRepository>()
    val workspaceRepository = mockk<WorkspaceRepository>()

    val calculator = StatisticsCalculator(
        orderRepository,
        orderSessionRepository,
        dailyOrderStatisticRepository,
        workspaceRepository
    )

    Given("A workspace with orders and sessions") {
        val workspaceId = 1L
        val referenceDate = LocalDate.of(2026, 2, 24)

        val workspace = mockk<Workspace>()
        every { workspace.id } returns workspaceId

        val session1 = mockk<OrderSession>()
        every { session1.id } returns 1L
        every { session1.tableNumber } returns 1
        every { session1.createdAt } returns referenceDate.atTime(10, 0)
        every { session1.endAt } returns referenceDate.atTime(10, 30) // 30 mins stay

        val session2 = mockk<OrderSession>()
        every { session2.id } returns 2L
        every { session2.tableNumber } returns 2
        every { session2.createdAt } returns referenceDate.atTime(12, 0)
        every { session2.endAt } returns referenceDate.atTime(14, 0) // 120 mins stay

        val sessions = listOf(session1, session2)

        val op1 = mockk<OrderProduct>()
        every { op1.productId } returns 101L
        every { op1.productName } returns "Beer"
        every { op1.quantity } returns 2
        every { op1.totalPrice } returns 10000

        val order1 = mockk<Order>()
        every { order1.id } returns 1L
        every { order1.createdAt } returns referenceDate.atTime(10, 5)
        every { order1.totalPrice } returns 10000
        every { order1.status } returns OrderStatus.SERVED
        every { order1.orderSession } returns session1
        every { order1.orderProducts } returns mutableListOf(op1)

        val op2 = mockk<OrderProduct>()
        every { op2.productId } returns 102L
        every { op2.productName } returns "Chicken"
        every { op2.quantity } returns 1
        every { op2.totalPrice } returns 20000
        
        val order2 = mockk<Order>()
        every { order2.id } returns 2L
        every { order2.createdAt } returns referenceDate.atTime(12, 30)
        every { order2.totalPrice } returns 20000
        every { order2.status } returns OrderStatus.SERVED
        every { order2.orderSession } returns session2
        every { order2.orderProducts } returns mutableListOf(op2)
        
        val orders = listOf(order1, order2)

        every { workspaceRepository.findById(workspaceId) } returns Optional.of(workspace)
        every { 
            orderRepository.findValidOrders(
                workspaceId, 
                referenceDate.atTime(9, 0), 
                referenceDate.plusDays(1).atTime(9, 0)
            )
        } returns orders
        
        every {
            orderSessionRepository.findAllByWorkspaceIdAndCreatedAtBetween(workspaceId, referenceDate.atTime(9, 0), referenceDate.plusDays(1).atTime(9, 0))
        } returns sessions
        
        every {
            dailyOrderStatisticRepository.findByWorkspaceIdAndReferenceDate(workspaceId, referenceDate.minusDays(1))
        } returns Optional.empty()

        every {
            orderRepository.findValidOrders(workspaceId, referenceDate.minusDays(1).atTime(9, 0), referenceDate.atTime(9, 0))
        } returns emptyList()

        When("Calculating statistics") {
            val result = calculator.calculate(workspaceId, referenceDate)

            Then("It calculates basic metrics correctly") {
                result.totalOrders shouldBe 2
                result.totalRevenue shouldBe 30000L
                result.totalSalesVolume shouldBe 3
                result.averageOrderAmount shouldBe 15000
                result.averageOrdersPerTable shouldBe 1.0
                result.averageStayTimeMinutes shouldBe 75.0
                result.tableTurnoverRate shouldBe 1.0
                result.salesByHour.size shouldBe 2
                result.popularProducts.byQuantity[0].name shouldBe "Beer"
                result.popularProducts.byRevenue[0].name shouldBe "Chicken"
            }
        }
    }
})
