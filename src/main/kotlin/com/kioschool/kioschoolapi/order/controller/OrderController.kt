package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CancelOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.GetOrdersByPhoneNumberRequestBody
import com.kioschool.kioschoolapi.order.dto.GetOrdersRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class OrderController(
    private val orderService: OrderService
) {
    @GetMapping("/admin/orders")
    fun getAllOrders(
        authentication: Authentication,
        @RequestBody body: GetOrdersRequestBody
    ): List<Order> {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.getAllOrders(username, body.workspaceId)
    }

    @PostMapping("/order")
    fun createOrder(
        @RequestBody body: CreateOrderRequestBody
    ): Order {
        return orderService.createOrder(
            body.workspaceId,
            body.tableNumber,
            body.phoneNumber,
            body.orderProducts
        )
    }

    @PostMapping("/orders/{phoneNumber}")
    fun getOrdersByPhoneNumber(
        @PathVariable phoneNumber: String,
        @RequestBody body: GetOrdersByPhoneNumberRequestBody
    ): List<Order> {
        return orderService.getOrdersByPhoneNumber(body.workspaceId, phoneNumber)
    }

    @PostMapping("/admin/order/{orderId}")
    fun cancelOrder(
        authentication: Authentication,
        @RequestBody body: CancelOrderRequestBody, @PathVariable orderId: Long
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.cancelOrder(username, body.workspaceId, orderId)
    }
}