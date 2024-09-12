package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.repository.CustomOrderRepository
import com.kioschool.kioschoolapi.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.websocket.service.WebsocketService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OrderServiceTest : DescribeSpec({
    val repository = mockk<OrderRepository>()
    val workspaceService = mockk<WorkspaceService>()
    val websocketService = mockk<WebsocketService>()
    val customOrderRepository = mockk<CustomOrderRepository>()
    val orderProductRepository = mockk<OrderProductRepository>()

    val sut = OrderService(
        repository,
        workspaceService,
        websocketService,
        customOrderRepository,
        orderProductRepository
    )

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

            // Mock
            every {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status
                )
            } returns emptyList()

            // Act
            sut.getAllOrdersByCondition(
                workspaceId,
                startDate,
                endDate,
                status
            ) shouldBe emptyList()

            // Assert
            verify {
                customOrderRepository.findAllByCondition(
                    workspaceId,
                    startDate,
                    endDate,
                    status
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

    describe("checkAccessible") {
        it("should throw WorkspaceInaccessibleException") {
            // Arrange
            val username = "test"
            val workspaceId = 1L

            // Mock
            every { workspaceService.isAccessible(username, workspaceId) } returns false

            // Act & Assert
            shouldThrow<WorkspaceInaccessibleException> {
                sut.checkAccessible(username, workspaceId)
            }
        }

        it("should not throw WorkspaceInaccessibleException") {
            // Arrange
            val username = "test"
            val workspaceId = 1L

            // Mock
            every { workspaceService.isAccessible(username, workspaceId) } returns true

            // Act & Assert
            sut.checkAccessible(username, workspaceId)
        }
    }
})