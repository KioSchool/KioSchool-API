package com.kioschool.kioschoolapi.order.facade

import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Component

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
        orderProducts: List<OrderProductRequestBody>
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
        val orderProducts = orderProducts.filter { productMap.containsKey(it.productId) }.map {
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
}