package com.kioschool.kioschoolapi.email.facade

import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.facade.OrderFacade
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class OrderFacadeTest : DescribeSpec({
    val orderService = mockk<OrderService>()
    val workspaceService = mockk<WorkspaceService>()
    val productService = mockk<ProductService>()

    val sut = OrderFacade(orderService, workspaceService, productService)

    beforeTest {
        mockkObject(orderService)
        mockkObject(workspaceService)
        mockkObject(productService)
    }

    afterTest {
        clearAllMocks()
    }

    describe("createOrder") {
        beforeTest {
            SampleEntity.order.orderProducts.clear()
        }

        it("should create order with order products") {
            val workspaceId = 1L
            val tableNumber = 1
            val customerName = "customer"
            val rawOrderProducts = listOf(
                OrderProductRequestBody(1L, 1),
            )

            every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
            every { orderService.saveOrder(any<Order>()) } returns SampleEntity.order

            every { productService.getAllProductsByCondition(workspaceId) } returns listOf(
                SampleEntity.productWithId(1L)
            )
            every { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) } returns SampleEntity.order

            val result = sut.createOrder(workspaceId, tableNumber, customerName, rawOrderProducts)

            assert(result == SampleEntity.order)
            assert(result.orderProducts.size == 1)
            assert(result.totalPrice == 1000)

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { orderService.saveOrder(any<Order>()) }
            verify { productService.getAllProductsByCondition(workspaceId) }
            verify { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) }
        }

        it("should add only exists products to order products") {
            val workspaceId = 1L
            val tableNumber = 1
            val customerName = "customer"
            val rawOrderProducts = listOf(
                OrderProductRequestBody(1L, 1),
                OrderProductRequestBody(2L, 1),
            )

            every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
            every { orderService.saveOrder(any<Order>()) } returns SampleEntity.order

            every { productService.getAllProductsByCondition(workspaceId) } returns listOf(
                SampleEntity.productWithId(1L)
            )
            every { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) } returns SampleEntity.order

            val result = sut.createOrder(workspaceId, tableNumber, customerName, rawOrderProducts)

            assert(result == SampleEntity.order)
            assert(result.orderProducts.size == 1)
            assert(result.orderProducts.first().productId == 1L)
            assert(result.totalPrice == 1000)

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { orderService.saveOrder(any<Order>()) }
            verify { productService.getAllProductsByCondition(workspaceId) }
            verify { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) }
        }
    }

    describe("getOrder") {
        it("should call orderService.getOrder") {
            val orderId = 1L

            every { orderService.getOrder(orderId) } returns SampleEntity.order

            val result = sut.getOrder(orderId)

            assert(result == SampleEntity.order)

            verify { orderService.getOrder(orderId) }
        }
    }

    describe("getOrdersByCondition") {
        it("should call workspaceService.checkAccessible and orderService.getAllOrdersByCondition with parsed parameters") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1
            val startDateStr = "2021-01-01"
            val endDateStr = "2021-01-02"
            val statusStr = OrderStatus.PAID.name

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = LocalDate.parse(startDateStr, formatter).atStartOfDay()
            val endDate = LocalDate.parse(endDateStr, formatter).atTime(LocalTime.MAX)
            val status = OrderStatus.PAID

            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status,
                    tableNumber
                )
            } returns listOf(SampleEntity.order)

            val result = sut.getOrdersByCondition(
                username,
                workspaceId,
                startDateStr,
                endDateStr,
                statusStr,
                tableNumber
            )

            assert(result == listOf(SampleEntity.order))

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status,
                    tableNumber
                )
            }
        }

        it("should call workspaceService.checkAccessible and orderService.getAllOrdersByCondition with null parameters") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1

            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    null,
                    null,
                    null,
                    tableNumber
                )
            } returns listOf(SampleEntity.order)

            val result = sut.getOrdersByCondition(
                username,
                workspaceId,
                null,
                null,
                null,
                tableNumber
            )

            assert(result == listOf(SampleEntity.order))

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    null,
                    null,
                    null,
                    tableNumber
                )
            }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1

            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getOrdersByCondition(
                    username,
                    workspaceId,
                    null,
                    null,
                    null,
                    tableNumber
                )
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    null,
                    null,
                    null,
                    tableNumber
                )
            }
        }
    }

    describe("getRealtimeOrders") {
        it("should call orderService.getRealtimeOrders with 2 hours ago") {
            val workspaceId = 1L
            val nowDateTime = LocalDateTime.now()
            val twoHoursAgo = nowDateTime.minusHours(2)

            mockkStatic(LocalDateTime::class)
            every { LocalDateTime.now() } returns nowDateTime
            every { workspaceService.checkAccessible("test", workspaceId) } just Runs
            every {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    twoHoursAgo,
                    nowDateTime,
                    null,
                    null
                )
            } returns listOf(SampleEntity.order)

            val result = sut.getRealtimeOrders("test", workspaceId)

            assert(result == listOf(SampleEntity.order))

            verify { workspaceService.checkAccessible("test", workspaceId) }
            verify {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    twoHoursAgo,
                    nowDateTime,
                    null,
                    null
                )
            }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val workspaceId = 1L

            every {
                workspaceService.checkAccessible(
                    "test",
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getRealtimeOrders("test", workspaceId)
            }

            verify { workspaceService.checkAccessible("test", workspaceId) }
            verify(exactly = 0) {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    any(),
                    any(),
                    null,
                    null
                )
            }
        }
    }

    describe("changeOrderStatus") {
        it("should call orderService.getOrder and orderService.saveOrderAndSendWebsocketMessage") {
            val orderId = 1L
            val status = OrderStatus.PAID.name

            every { workspaceService.checkAccessible("test", 1L) } just Runs
            every { orderService.getOrder(orderId) } returns SampleEntity.order
            every { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) } returns SampleEntity.order

            val result = sut.changeOrderStatus("test", 1L, orderId, status)

            assert(result == SampleEntity.order)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.getOrder(orderId) }
            verify { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val orderId = 1L
            val status = OrderStatus.PAID.name

            every {
                workspaceService.checkAccessible(
                    "test",
                    1L
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.changeOrderStatus("test", 1L, orderId, status)
            }

            verify { workspaceService.checkAccessible("test", 1L) }
            verify(exactly = 0) { orderService.getOrder(orderId) }
            verify(exactly = 0) { orderService.saveOrderAndSendWebsocketMessage(any<Order>()) }
        }
    }

    describe("serveOrderProduct") {
        it("should call orderService.getOrderProduct and orderService.saveOrderProductAndSendWebsocketMessage") {
            val orderProductId = 1L
            val isServed = true
            val orderProduct = SampleEntity.orderProduct.apply { this.isServed = false }

            every { workspaceService.checkAccessible("test", 1L) } just Runs
            every { orderService.getOrderProduct(orderProductId) } returns orderProduct
            every { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) } returns orderProduct

            val result = sut.serveOrderProduct("test", 1L, orderProductId, isServed)

            assert(result == orderProduct)
            assert(result.isServed == isServed)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.getOrderProduct(orderProductId) }
            verify { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val orderProductId = 1L
            val isServed = true

            every {
                workspaceService.checkAccessible(
                    "test",
                    1L
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.serveOrderProduct("test", 1L, orderProductId, isServed)
            }

            verify { workspaceService.checkAccessible("test", 1L) }
            verify(exactly = 0) { orderService.getOrderProduct(orderProductId) }
            verify(exactly = 0) { orderService.saveOrderProductAndSendWebsocketMessage(any()) }
        }
    }

    describe("getOrdersByTable") {
        it("should call workspaceService.checkAccessible and orderService.getAllOrdersByTable") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1
            val page = 0
            val size = 10

            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every {
                orderService.getAllOrdersByTable(workspaceId, tableNumber, page, size)
            } returns PageImpl(listOf(SampleEntity.order))

            val result = sut.getOrdersByTable(username, workspaceId, tableNumber, page, size)

            assert(result.content == listOf(SampleEntity.order))

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify {
                orderService.getAllOrdersByTable(workspaceId, tableNumber, page, size)
            }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1
            val page = 0
            val size = 10

            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getOrdersByTable(username, workspaceId, tableNumber, page, size)
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) {
                orderService.getAllOrdersByTable(workspaceId, tableNumber, page, size)
            }
        }
    }

    describe("changeOrderProductServedCount") {
        it("should update servedCount and update isServed to true when servedCount is equal to quantity") {
            val orderProductId = 1L
            val servedCount = 1
            val orderProduct = SampleEntity.orderProduct.apply {
                this.servedCount = 0
                this.quantity = 1
            }

            every { workspaceService.checkAccessible("test", 1L) } just Runs
            every { orderService.getOrderProduct(orderProductId) } returns orderProduct
            every { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) } returns orderProduct

            val result = sut.changeOrderProductServedCount("test", 1L, orderProductId, servedCount)

            assert(result == orderProduct)
            assert(result.servedCount == servedCount)
            assert(result.isServed)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.getOrderProduct(orderProductId) }
            verify { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) }
        }

        it("should update servedCount and update isServed to false when servedCount is less than quantity") {
            val orderProductId = 1L
            val servedCount = 1
            val orderProduct = SampleEntity.orderProduct.apply {
                this.servedCount = 0
                this.quantity = 2
            }

            every { workspaceService.checkAccessible("test", 1L) } just Runs
            every { orderService.getOrderProduct(orderProductId) } returns orderProduct
            every { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) } returns orderProduct

            val result = sut.changeOrderProductServedCount("test", 1L, orderProductId, servedCount)

            assert(result == orderProduct)
            assert(result.servedCount == servedCount)
            assert(!result.isServed)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.getOrderProduct(orderProductId) }
            verify { orderService.saveOrderProductAndSendWebsocketMessage(orderProduct) }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            val orderProductId = 1L
            val servedCount = 1

            every {
                workspaceService.checkAccessible(
                    "test",
                    1L
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.changeOrderProductServedCount("test", 1L, orderProductId, servedCount)
            }

            verify { workspaceService.checkAccessible("test", 1L) }
            verify(exactly = 0) { orderService.getOrderProduct(orderProductId) }
            verify(exactly = 0) { orderService.saveOrderProductAndSendWebsocketMessage(any()) }
        }
    }
})