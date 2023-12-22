package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.GetOrdersByPhoneNumberRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import org.springframework.web.bind.annotation.*

@RestController
class OrderController(
    private val orderService: OrderService
) {
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

    @GetMapping("/order")
    fun getOrdersByPhoneNumber(
        @RequestParam phoneNumber: String,
        @RequestParam workspaceId: Long,
    ): List<Order> {
        return orderService.getOrdersByPhoneNumber(workspaceId, phoneNumber)
    }
}