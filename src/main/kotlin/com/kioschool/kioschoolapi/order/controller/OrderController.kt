package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.GetOrdersRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
        return orderService.createOrder(body.workspaceId, body.tableNumber, body.orderProducts)
    }
}