package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.websocket.dto.Message
import com.kioschool.kioschoolapi.websocket.service.WebsocketService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workspaceService: WorkspaceService,
    private val productService: ProductService,
    private val websocketService: WebsocketService
) {
    fun getAllOrders(username: String, workspaceId: Long): List<Order> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        return orderRepository.findAllByWorkspaceId(workspaceId)
    }

    fun createOrder(
        workspaceId: Long,
        tableNumber: Int,
        phoneNumber: String,
        rawOrderProducts: List<OrderProductRequestBody>
    ): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val order = orderRepository.save(
            Order(
                workspace = workspace,
                tableNumber = tableNumber,
                phoneNumber = phoneNumber
            )
        )
        val productMap = productService.getProducts(workspaceId).associateBy { it.id }
        val orderProducts = rawOrderProducts.filter { productMap.containsKey(it.productId) }.map {
            val product = productMap[it.productId]!!
            OrderProduct(
                order = order,
                product = product,
                quantity = it.quantity,
                totalPrice = product.price * it.quantity
            )
        }

        order.orderProducts.addAll(orderProducts)
        order.totalPrice = orderProducts.sumOf { it.totalPrice }
        return saveOrderAndSendWebsocketMessage(order)
    }

    fun cancelOrder(username: String, workspaceId: Long, orderId: Long): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val order = orderRepository.findById(orderId).get()
        order.status = OrderStatus.CANCELLED
        return orderRepository.save(order)
    }

    fun getOrdersByPhoneNumber(workspaceId: Long, phoneNumber: String): List<Order> {
        return orderRepository.findAllByWorkspaceIdAndPhoneNumber(workspaceId, phoneNumber)
    }

    fun serveOrder(username: String, workspaceId: Long, orderId: Long): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val order = orderRepository.findById(orderId).get()
        order.status = OrderStatus.SERVED
        return orderRepository.save(order)
    }

    private fun saveOrderAndSendWebsocketMessage(order: Order): Order {
        val savedOrder = orderRepository.save(order)
        websocketService.sendMessage(
            "/sub/order/${order.workspace.id}",
            Message("CREATE", savedOrder)
        )
        return savedOrder
    }
}