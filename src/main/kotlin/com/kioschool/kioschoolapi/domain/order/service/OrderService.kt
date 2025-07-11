package com.kioschool.kioschoolapi.domain.order.service

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.domain.order.repository.*
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.WebsocketType
import com.kioschool.kioschoolapi.global.websocket.dto.Message
import com.kioschool.kioschoolapi.global.websocket.service.CustomWebSocketService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val websocketService: CustomWebSocketService,
    private val customOrderRepository: CustomOrderRepository,
    private val orderRedisRepository: OrderRedisRepository,
    private val orderProductRepository: OrderProductRepository,
    private val orderSessionRepository: OrderSessionRepository
) {
    fun saveOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    fun saveOrderAndSendWebsocketMessage(order: Order, type: WebsocketType): Order {
        val savedOrder = orderRepository.save(order)
        websocketService.sendMessage(
            "/sub/order/${order.workspace.id}",
            Message(type, savedOrder)
        )
        return savedOrder
    }

    fun saveOrderProductAndSendWebsocketMessage(orderProduct: OrderProduct): OrderProduct {
        val savedOrderProduct = orderProductRepository.save(orderProduct)
        websocketService.sendMessage(
            "/sub/order/${orderProduct.order.workspace.id}",
            Message(WebsocketType.UPDATED, savedOrderProduct.order)
        )
        return savedOrderProduct
    }

    fun getAllOrdersByCondition(
        workspaceId: Long,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        status: OrderStatus?,
        tableNumber: Int?
    ): List<Order> {
        return customOrderRepository.findAllByCondition(
            workspaceId,
            startDate,
            endDate,
            status,
            tableNumber
        )
    }

    fun getOrder(orderId: Long): Order {
        return orderRepository.findById(orderId).get()
    }

    fun getOrderProduct(orderProductId: Long): OrderProduct {
        return orderProductRepository.findById(orderProductId).get()
    }

    fun getOrderNumber(workspaceId: Long): Long {
        return orderRedisRepository.incrementOrderNumber(workspaceId)
    }

    fun resetOrderNumber(workspaceId: Long) {
        orderRedisRepository.resetOrderNumber(workspaceId)
    }

    fun resetAllOrderNumber() {
        orderRedisRepository.resetAllOrderNumber()
    }

    fun getAllOrdersByTable(
        workspaceId: Long,
        tableNumber: Int,
        page: Int,
        size: Int
    ): Page<Order> {
        return orderRepository.findAllByWorkspaceIdAndTableNumber(
            workspaceId,
            tableNumber,
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("id")
                )
            )
        )
    }

    fun getOrderSession(orderSessionId: Long): OrderSession {
        return orderSessionRepository.findById(orderSessionId).get()
    }

    fun getAllOrdersByOrderSession(orderSession: OrderSession): List<Order> {
        return orderRepository.findAllByOrderSession(orderSession)
    }

    fun saveOrderSession(orderSession: OrderSession): OrderSession {
        return orderSessionRepository.save(orderSession)
    }

    fun createOrderSession(
        workspace: Workspace,
        table: WorkspaceTable,
        workspaceSetting: WorkspaceSetting
    ): OrderSession {
        val expectedEndAt =
            if (workspaceSetting.useOrderSessionTimeLimit) {
                LocalDateTime.now().plusMinutes(workspaceSetting.orderSessionTimeLimitMinutes)
            } else null

        return orderSessionRepository.save(
            OrderSession(
                workspace = workspace,
                expectedEndAt = expectedEndAt,
                tableNumber = table.tableNumber
            )
        )
    }
}