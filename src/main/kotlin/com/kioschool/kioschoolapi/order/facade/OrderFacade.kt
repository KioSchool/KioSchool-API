package com.kioschool.kioschoolapi.order.facade

import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val workspaceService: WorkspaceService,
    private val productService: ProductService
) {
    fun createOrder(
        workspaceId: Long,
        tableNumber: Int,
        customerName: String,
        rawOrderProducts: List<OrderProductRequestBody>
    ): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val order = orderService.saveOrder(
            Order(
                workspace = workspace,
                tableNumber = tableNumber,
                customerName = customerName
            )
        )

        val products = productService.getAllProductsByCondition(workspaceId)
        val productMap = products.associateBy { it.id }
        val orderProducts = rawOrderProducts.filter { productMap.containsKey(it.productId) }.map {
            val product = productMap[it.productId]!!
            OrderProduct(
                order = order,
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = it.quantity,
                totalPrice = product.price * it.quantity
            )
        }

        order.orderProducts.addAll(orderProducts)
        order.totalPrice = orderProducts.sumOf { it.totalPrice }
        return orderService.saveOrderAndSendWebsocketMessage(order)
    }

    fun getOrder(orderId: Long): Order {
        return orderService.getOrder(orderId)
    }

    fun getOrdersByCondition(
        username: String,
        workspaceId: Long,
        startDate: String?,
        endDate: String?,
        status: String?
    ): List<Order> {
        orderService.checkAccessible(username, workspaceId)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedStartDate = startDate?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val parsedEndDate = endDate?.let { LocalDate.parse(it, formatter).atTime(LocalTime.MAX) }
        val parsedStatus = status?.let { OrderStatus.valueOf(it) }

        return orderService.getAllOrdersByCondition(
            workspaceId,
            parsedStartDate,
            parsedEndDate,
            parsedStatus
        )
    }

    fun getRealtimeOrders(username: String, workspaceId: Long): List<Order> {
        orderService.checkAccessible(username, workspaceId)

        val startDate = LocalDateTime.now().minusHours(2)
        val endDate = LocalDateTime.now()

        return orderService.getAllOrdersByCondition(workspaceId, startDate, endDate, null)
    }

    fun changeOrderStatus(
        username: String,
        workspaceId: Long,
        orderId: Long,
        status: String
    ): Order {
        orderService.checkAccessible(username, workspaceId)

        val order = orderService.getOrder(orderId)
        order.status = OrderStatus.valueOf(status)

        return orderService.saveOrderAndSendWebsocketMessage(order)
    }

    fun serveOrderProduct(
        username: String,
        workspaceId: Long,
        orderProductId: Long,
        isServed: Boolean
    ): OrderProduct {
        orderService.checkAccessible(username, workspaceId)

        val orderProduct = orderService.getOrderProduct(orderProductId)
        orderProduct.isServed = isServed
        return orderService.saveOrderProductAndSendWebsocketMessage(orderProduct)
    }
}