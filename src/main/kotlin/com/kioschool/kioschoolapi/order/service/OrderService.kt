package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workspaceService: WorkspaceService,
    private val productService: ProductService
) {
    fun getAllOrders(username: String, workspaceId: Long): List<Order> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        return orderRepository.findAllByWorkspaceId(workspaceId)
    }

    fun createOrder(
        workspaceId: Long,
        tableNumber: Int,
        rawOrderProducts: List<OrderProductRequestBody>
    ): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val order = orderRepository.save(
            Order(
                workspace = workspace,
                tableNumber = tableNumber,
            )
        )
        val productMap = productService.getProducts(workspaceId).associateBy { it.id }
        val orderProducts = rawOrderProducts.filter { productMap.containsKey(it.productId) }.map {
            OrderProduct(
                order = order,
                product = productMap[it.productId]!!,
                quantity = it.quantity
            )
        }

        order.orderProducts.addAll(orderProducts)
        return orderRepository.save(order)
    }
}