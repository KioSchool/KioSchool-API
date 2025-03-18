package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.repository.CustomOrderRepository
import com.kioschool.kioschoolapi.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.order.repository.OrderRedisRepository
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.websocket.service.CustomWebSocketService
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class OrderServiceTest : DescribeSpec({
    val repository = mockk<OrderRepository>()
    val workspaceService = mockk<WorkspaceService>()
    val websocketService = mockk<CustomWebSocketService>()
    val customOrderRepository = mockk<CustomOrderRepository>()
    val orderRedisRepository = mockk<OrderRedisRepository>()
    val orderProductRepository = mockk<OrderProductRepository>()

    val sut = OrderService(
        repository,
        websocketService,
        customOrderRepository,
        orderRedisRepository,
        orderProductRepository
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(workspaceService)
        mockkObject(websocketService)
        mockkObject(customOrderRepository)
        mockkObject(orderRedisRepository)
        mockkObject(orderProductRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("saveOrder") {
        it("should save order") {
            // Arrange
            val order = SampleEntity.order

            // Mock
            every { repository.save(order) } returns order

            // Act
            sut.saveOrder(order) shouldBe order

            // Assert
            verify { repository.save(order) }
        }
    }

    describe("saveOrderAndSendWebsocketMessage") {
        it("should save order and send websocket message") {
            // Arrange
            val order = SampleEntity.order

            // Mock
            every { repository.save(order) } returns order
            every {
                websocketService.sendMessage(
                    "/sub/order/${order.workspace.id}",
                    any()
                )
            } returns Unit

            // Act
            sut.saveOrderAndSendWebsocketMessage(order) shouldBe order

            // Assert
            verify { repository.save(order) }
            verify { websocketService.sendMessage("/sub/order/${order.workspace.id}", any()) }
        }
    }

    describe("saveOrderProductAndSendWebsocketMessage") {
        it("should save order product and send websocket message") {
            // Arrange
            val orderProduct = SampleEntity.orderProduct

            // Mock
            every { orderProductRepository.save(orderProduct) } returns orderProduct
            every {
                websocketService.sendMessage(
                    "/sub/order/${orderProduct.order.workspace.id}",
                    any()
                )
            } returns Unit

            // Act
            sut.saveOrderProductAndSendWebsocketMessage(orderProduct) shouldBe orderProduct

            // Assert
            verify { orderProductRepository.save(orderProduct) }
            verify {
                websocketService.sendMessage(
                    "/sub/order/${orderProduct.order.workspace.id}",
                    any()
                )
            }
        }
    }

    describe("getAllOrdersByCondition") {
        it("should call customOrderRepository.findAllByCondition") {
            // Arrange
            val workspaceId = 1L
            val startDate = null
            val endDate = null
            val status = null
            val tableNumber = null

            // Mock
            every {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status,
                    tableNumber
                )
            } returns emptyList()

            // Act
            sut.getAllOrdersByCondition(
                workspaceId,
                startDate,
                endDate,
                status,
                tableNumber
            ) shouldBe emptyList()

            // Assert
            verify {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status,
                    tableNumber
                )
            }
        }
    }

    describe("getOrder") {
        it("should return order") {
            // Arrange
            val orderId = 1L
            val order = SampleEntity.order

            // Mock
            every { repository.findById(orderId) } returns mockk {
                every { get() } returns order
            }

            // Act
            sut.getOrder(orderId) shouldBe order

            // Assert
            verify { repository.findById(orderId) }
        }
    }

    describe("getOrderProduct") {
        it("should return order product") {
            // Arrange
            val orderProductId = 1L
            val orderProduct = SampleEntity.orderProduct

            // Mock
            every { orderProductRepository.findById(orderProductId) } returns mockk {
                every { get() } returns orderProduct
            }

            // Act
            sut.getOrderProduct(orderProductId) shouldBe orderProduct

            // Assert
            verify { orderProductRepository.findById(orderProductId) }
        }
    }

    describe("getOrderNumber") {
        it("should return order number") {
            // Arrange
            val workspaceId = 1L
            val orderNumber = 1L

            // Mock
            every { orderRedisRepository.incrementOrderNumber(workspaceId) } returns orderNumber

            // Act
            sut.getOrderNumber(workspaceId) shouldBe orderNumber

            // Assert
            verify { orderRedisRepository.incrementOrderNumber(workspaceId) }
        }
    }

    describe("getAllOrdersByTable") {
        it("should call orderRepository.findAllByTableNumber") {
            // Arrange
            val workspaceId = 1L
            val tableNumber = 1
            val page = 1
            val size = 10

            // Mock
            every {
                repository.findAllByWorkspaceIdAndTableNumber(
                    workspaceId,
                    tableNumber,
                    PageRequest.of(
                        page, size, Sort.by(
                            Sort.Order.desc("id")
                        )
                    )
                )
            } returns mockk()

            // Act
            sut.getAllOrdersByTable(
                workspaceId,
                tableNumber,
                page,
                size
            )

            // Assert
            verify {
                repository.findAllByWorkspaceIdAndTableNumber(
                    workspaceId,
                    tableNumber,
                    PageRequest.of(
                        page, size, Sort.by(
                            Sort.Order.desc("id")
                        )
                    )
                )
            }
        }
    }
})