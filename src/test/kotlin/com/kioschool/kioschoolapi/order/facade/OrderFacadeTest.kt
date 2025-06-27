package com.kioschool.kioschoolapi.order.facade

import com.kioschool.kioschoolapi.domain.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.exception.NoOrderSessionException
import com.kioschool.kioschoolapi.domain.order.facade.OrderFacade
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.WebsocketType
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime

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
            SampleEntity.order1.orderProducts.clear()
        }

        it("should create order with order products") {
            val workspaceId = 1L
            val tableNumber = 1
            val customerName = "customer"
            val workspace = SampleEntity.workspace
            val rawOrderProducts = listOf(
                OrderProductRequestBody(1L, 1),
            )

            every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
            every {
                workspaceService.getWorkspaceTable(
                    workspace,
                    tableNumber
                )
            } returns SampleEntity.workspaceTable.apply { orderSession = SampleEntity.orderSession }
            every { orderService.getOrderNumber(workspaceId) } returns 1
            every { orderService.saveOrder(any<Order>()) } returns SampleEntity.order1
            every { productService.validateProducts(workspaceId, any()) } just Runs

            every { productService.getAllProductsByCondition(workspaceId) } returns listOf(
                SampleEntity.productWithId(1L)
            )
            every {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.CREATED
                )
            } returns SampleEntity.order1

            val result = sut.createOrder(workspaceId, tableNumber, customerName, rawOrderProducts)

            assert(result == SampleEntity.order1)
            assert(result.orderProducts.size == 1)
            assert(result.totalPrice == 1000)

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.getWorkspaceTable(workspace, tableNumber) }
            verify { orderService.getOrderNumber(workspaceId) }
            verify { orderService.saveOrder(any<Order>()) }
            verify { productService.validateProducts(workspaceId, any()) }
            verify { productService.getAllProductsByCondition(workspaceId) }
            verify {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.CREATED
                )
            }
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
            every {
                workspaceService.getWorkspaceTable(
                    SampleEntity.workspace,
                    tableNumber
                )
            } returns SampleEntity.workspaceTable.apply { orderSession = SampleEntity.orderSession }
            every { orderService.getOrderNumber(workspaceId) } returns 1
            every { orderService.saveOrder(any<Order>()) } returns SampleEntity.order1
            every { productService.validateProducts(workspaceId, any()) } just Runs

            every { productService.getAllProductsByCondition(workspaceId) } returns listOf(
                SampleEntity.productWithId(1L)
            )
            every {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.CREATED
                )
            } returns SampleEntity.order1

            val result = sut.createOrder(workspaceId, tableNumber, customerName, rawOrderProducts)

            assert(result == SampleEntity.order1)
            assert(result.orderProducts.size == 1)
            assert(result.orderProducts.first().productId == 1L)
            assert(result.totalPrice == 1000)

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.getWorkspaceTable(SampleEntity.workspace, tableNumber) }
            verify { orderService.getOrderNumber(workspaceId) }
            verify { orderService.saveOrder(any<Order>()) }
            verify { productService.validateProducts(workspaceId, any()) }
            verify { productService.getAllProductsByCondition(workspaceId) }
            verify {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.CREATED
                )
            }
        }

        it("should throw NoOrderSessionException when no order session exists for the table") {
            val workspaceId = 1L
            val tableNumber = 1
            val customerName = "customer"
            val rawOrderProducts = listOf(
                OrderProductRequestBody(1L, 1),
            )

            every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
            every {
                workspaceService.getWorkspaceTable(
                    SampleEntity.workspace,
                    tableNumber
                )
            } returns SampleEntity.workspaceTable.apply { orderSession = null }

            assertThrows<NoOrderSessionException> {
                sut.createOrder(workspaceId, tableNumber, customerName, rawOrderProducts)
            }

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.getWorkspaceTable(SampleEntity.workspace, tableNumber) }
            verify(exactly = 0) { orderService.getOrderNumber(workspaceId) }
            verify(exactly = 0) { orderService.saveOrder(any<Order>()) }
            verify(exactly = 0) { productService.validateProducts(workspaceId, any()) }
            verify(exactly = 0) { productService.getAllProductsByCondition(workspaceId) }
        }
    }

    describe("getOrder") {
        it("should call orderService.getOrder") {
            val orderId = 1L

            every { orderService.getOrder(orderId) } returns SampleEntity.order1

            val result = sut.getOrder(orderId)

            assert(result == SampleEntity.order1)

            verify { orderService.getOrder(orderId) }
        }
    }

    describe("getOrdersByCondition") {
        it("should call workspaceService.checkAccessible and orderService.getAllOrdersByCondition with parsed parameters") {
            val username = "test"
            val workspaceId = 1L
            val tableNumber = 1
            val startDate = LocalDateTime.of(2021, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2021, 1, 2, 0, 0)
            val statusStr = OrderStatus.PAID.name
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
            } returns listOf(SampleEntity.order1)

            val result = sut.getOrdersByCondition(
                username,
                workspaceId,
                startDate,
                endDate,
                statusStr,
                tableNumber
            )

            assert(result == listOf(SampleEntity.order1))

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
            } returns listOf(SampleEntity.order1)

            val result = sut.getOrdersByCondition(
                username,
                workspaceId,
                null,
                null,
                null,
                tableNumber
            )

            assert(result == listOf(SampleEntity.order1))

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
            } returns listOf(SampleEntity.order1)

            val result = sut.getRealtimeOrders("test", workspaceId)

            assert(result == listOf(SampleEntity.order1))

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
            every { orderService.getOrder(orderId) } returns SampleEntity.order1
            every {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.UPDATED
                )
            } returns SampleEntity.order1

            val result = sut.changeOrderStatus("test", 1L, orderId, status)

            assert(result == SampleEntity.order1)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.getOrder(orderId) }
            verify {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.UPDATED
                )
            }
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
            verify(exactly = 0) {
                orderService.saveOrderAndSendWebsocketMessage(
                    any<Order>(),
                    WebsocketType.UPDATED
                )
            }
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
            } returns PageImpl(listOf(SampleEntity.order1))

            val result = sut.getOrdersByTable(username, workspaceId, tableNumber, page, size)

            assert(result.content == listOf(SampleEntity.order1))

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
            val orderProduct = SampleEntity.orderProduct1.apply {
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
            val orderProduct = SampleEntity.orderProduct1.apply {
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

    describe("resetOrderNumber") {
        it("should call orderService.resetOrderNumber") {
            every { workspaceService.checkAccessible("test", 1L) } just Runs
            every { orderService.resetOrderNumber(1L) } just Runs

            sut.resetOrderNumber("test", 1L)

            verify { workspaceService.checkAccessible("test", 1L) }
            verify { orderService.resetOrderNumber(1L) }
        }

        it("should throw WorkspaceInaccessibleException when workspace is not accessible") {
            every {
                workspaceService.checkAccessible(
                    "test",
                    1L
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.resetOrderNumber("test", 1L)
            }

            verify { workspaceService.checkAccessible("test", 1L) }
            verify(exactly = 0) { orderService.resetOrderNumber(1L) }
        }
    }

    describe("getOrderPricePrefixSum") {
        it("should calculate the prefix sum of order prices") {
            val workspaceId = 1L
            val startDate = LocalDateTime.of(2021, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2021, 1, 2, 0, 0)
            val status = null

            every { workspaceService.checkAccessible("test", workspaceId) } just Runs
            every {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    null,
                    null
                )
            } returns listOf(SampleEntity.order1.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 0, 0)
            }, SampleEntity.order2.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 1, 0)
            }, SampleEntity.order3.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 2, 0)
            })

            val result = sut.getOrderPricePrefixSum("test", workspaceId, startDate, endDate, status)

            assert(result.size == 3)
            assert(result[0].prefixSumPrice == 1000L)
            assert(result[1].prefixSumPrice == 4000L)
            assert(result[2].prefixSumPrice == 6000L)

            verify { workspaceService.checkAccessible("test", workspaceId) }
            verify {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    null,
                    null
                )
            }
        }
    }

    describe("getOrderHourlyPrice") {
        it("should calculate the hourly price of orders") {
            val workspaceId = 1L
            val startDate = LocalDateTime.of(2021, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2021, 1, 2, 0, 0)
            val status = null

            every { workspaceService.checkAccessible("test", workspaceId) } just Runs
            every {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    null,
                    null
                )
            } returns listOf(SampleEntity.order1.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 0, 0)
                totalPrice = 1000
            }, SampleEntity.order2.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 1, 0)
                totalPrice = 3000
            }, SampleEntity.order3.apply {
                createdAt = LocalDateTime.of(2021, 1, 1, 2, 0)
                totalPrice = 2000
            })

            val result = sut.getOrderHourlyPrice("test", workspaceId, startDate, endDate, status)

            assert(result.size == 3)
            assert(result[0].price == 1000L)
            assert(result[1].price == 3000L)
            assert(result[2].price == 2000L)

            verify { workspaceService.checkAccessible("test", workspaceId) }
            verify {
                orderService.getAllOrdersByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    null,
                    null
                )
            }
        }
    }
})