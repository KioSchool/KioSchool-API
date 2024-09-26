package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.repository.CustomOrderRepository
import com.kioschool.kioschoolapi.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.websocket.dto.Message
import com.kioschool.kioschoolapi.websocket.service.WebsocketService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workspaceService: WorkspaceService,
    private val websocketService: WebsocketService,
    private val customOrderRepository: CustomOrderRepository,
    private val orderProductRepository: OrderProductRepository
) {
    fun saveOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    fun saveOrderAndSendWebsocketMessage(order: Order): Order {
        val savedOrder = orderRepository.save(order)
        websocketService.sendMessage(
            "/sub/order/${order.workspace.id}",
            Message("CREATE", savedOrder)
        )
        return savedOrder
    }

    fun saveOrderProductAndSendWebsocketMessage(orderProduct: OrderProduct): OrderProduct {
        val savedOrderProduct = orderProductRepository.save(orderProduct)
        websocketService.sendMessage(
            "/sub/order/${orderProduct.order.workspace.id}",
            Message("CREATE", savedOrderProduct)
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

    fun checkAccessible(username: String, workspaceId: Long) {
        if (!workspaceService.isAccessible(username, workspaceId)) {
            throw WorkspaceInaccessibleException()
        }
    }

    fun getAllOrdersByTable(
        workspaceId: Long,
        tableNumber: Int,
        page: Int,
        size: Int
    ): Page<Order> {
        return orderRepository.findAllByTableNumber(
            workspaceId,
            tableNumber,
            PageRequest.of(page, size)
        )
    }
}