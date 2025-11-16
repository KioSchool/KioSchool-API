package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.domain.order.repository.CustomOrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.CustomOrderSessionRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRedisRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.WebsocketType
import com.kioschool.kioschoolapi.global.websocket.service.CustomWebSocketService
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
    val orderSessionRepository = mockk<OrderSessionRepository>()
    val customOrderSessionRepository = mockk<CustomOrderSessionRepository>()

    val sut = OrderService(
        repository,
        websocketService,
        customOrderRepository,
        orderRedisRepository,
        orderProductRepository,
        orderSessionRepository,
        customOrderSessionRepository
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(workspaceService)
        mockkObject(websocketService)
        mockkObject(customOrderRepository)
        mockkObject(orderRedisRepository)
        mockkObject(orderProductRepository)
        mockkObject(orderSessionRepository)
        mockkObject(customOrderSessionRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("saveOrder") {
        it("should save order") {
            // Arrange
            val order = SampleEntity.order1

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
            val order = SampleEntity.order1
            val type = WebsocketType.CREATED

            // Mock
            every { repository.save(order) } returns order
            every {
                websocketService.sendMessage(
                    "/sub/order/${order.workspace.id}",
                    any()
                )
            } returns Unit

            // Act
            sut.saveOrderAndSendWebsocketMessage(order, type) shouldBe order

            // Assert
            verify { repository.save(order) }
            verify { websocketService.sendMessage("/sub/order/${order.workspace.id}", any()) }
        }
    }

    describe("saveOrderProductAndSendWebsocketMessage") {
        it("should save order product and send websocket message") {
            // Arrange
            val orderProduct = SampleEntity.orderProduct1

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
            val statuses = null
            val tableNumber = null

            // Mock
            every {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    statuses,
                    tableNumber
                )
            } returns emptyList()

            // Act
            sut.getAllOrdersByCondition(
                workspaceId,
                startDate,
                endDate,
                statuses,
                tableNumber
            ) shouldBe emptyList()

            // Assert
            verify {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    statuses,
                    tableNumber
                )
            }
        }
    }

    describe("getOrder") {
        it("should return order") {
            // Arrange
            val orderId = 1L
            val order = SampleEntity.order1

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
            val orderProduct = SampleEntity.orderProduct1

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

    describe("resetOrderNumber") {
        it("should call orderRedisRepository.resetOrderNumber") {
            // Arrange
            val workspaceId = 1L

            // Mock
            every { orderRedisRepository.resetOrderNumber(workspaceId) } returns Unit

            // Act
            sut.resetOrderNumber(workspaceId)

            // Assert
            verify { orderRedisRepository.resetOrderNumber(workspaceId) }
        }
    }

    describe("resetAllOrderNumber") {
        it("should call orderRedisRepository.resetAllOrderNumber") {
            // Mock
            every { orderRedisRepository.resetAllOrderNumber() } returns Unit

            // Act
            sut.resetAllOrderNumber()

            // Assert
            verify { orderRedisRepository.resetAllOrderNumber() }
        }
    }

    describe("getAllOrderSessionsByCondition") {
        it("should call customOrderSessionRepository.findAllByCondition") {
            // Arrange
            val workspaceId = 1L
            val tableNumber = 1
            val start = java.time.LocalDateTime.now()
            val end = java.time.LocalDateTime.now()

            // Mock
            every {
                customOrderSessionRepository.findAllByCondition(
                    workspaceId,
                    tableNumber,
                    start,
                    end
                )
            } returns emptyList()

            // Act
            sut.getAllOrderSessionsByCondition(
                workspaceId,
                tableNumber,
                start,
                end
            ) shouldBe emptyList()

            // Assert
            verify {
                customOrderSessionRepository.findAllByCondition(
                    workspaceId,
                    tableNumber,
                    start,
                    end
                )
            }
        }
    }

    describe("getOrderSession") {
        it("should return order session") {
            // Arrange
            val orderSessionId = 1L
            val orderSession = SampleEntity.orderSession

            // Mock
            every { orderSessionRepository.findById(orderSessionId) } returns mockk {
                every { get() } returns orderSession
            }

            // Act
            sut.getOrderSession(orderSessionId) shouldBe orderSession

            // Assert
            verify { orderSessionRepository.findById(orderSessionId) }
        }
    }

    describe("getAllOrdersByOrderSession") {
        it("should return orders by order session") {
            // Arrange
            val orderSession = SampleEntity.orderSession
            val orders = listOf(SampleEntity.order1, SampleEntity.order2)

            // Mock
            every { repository.findAllByOrderSession(orderSession) } returns orders

            // Act
            sut.getAllOrdersByOrderSession(orderSession) shouldBe orders

            // Assert
            verify { repository.findAllByOrderSession(orderSession) }
        }
    }

    describe("saveOrderSession") {
        it("should save order session") {
            // Arrange
            val orderSession = SampleEntity.orderSession

            // Mock
            every { orderSessionRepository.save(orderSession) } returns orderSession

            // Act
            sut.saveOrderSession(orderSession) shouldBe orderSession

            // Assert
            verify { orderSessionRepository.save(orderSession) }
        }
    }

    describe("createOrderSession") {
        it("should create order session with time limit") {
            // Arrange
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTable
            val workspaceSetting = SampleEntity.workspaceSetting.apply {
                useOrderSessionTimeLimit = true
                orderSessionTimeLimitMinutes = 60
            }

            // Mock
            every { orderSessionRepository.save(any()) } answers { firstArg() }

            // Act
            val result = sut.createOrderSession(workspace, table, workspaceSetting)

            // Assert
            result.workspace shouldBe workspace
            result.tableNumber shouldBe table.tableNumber
            assert(result.expectedEndAt != null)

            verify { orderSessionRepository.save(any()) }
        }

        it("should create order session without time limit") {
            // Arrange
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTable
            val workspaceSetting = SampleEntity.workspaceSetting.apply {
                useOrderSessionTimeLimit = false
            }

            // Mock
            every { orderSessionRepository.save(any()) } answers { firstArg() }

            // Act
            val result = sut.createOrderSession(workspace, table, workspaceSetting)

            // Assert
            result.workspace shouldBe workspace
            result.tableNumber shouldBe table.tableNumber
            result.expectedEndAt shouldBe null

            verify { orderSessionRepository.save(any()) }
        }
    }
})