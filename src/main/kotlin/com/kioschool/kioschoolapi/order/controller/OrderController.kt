package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.GetOrdersByPhoneNumberRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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

    @PostMapping("/orders/{phoneNumber}")
    fun getOrdersByPhoneNumber(
        @PathVariable phoneNumber: String,
        @RequestBody body: GetOrdersByPhoneNumberRequestBody
    ): List<Order> {
        return orderService.getOrdersByPhoneNumber(body.workspaceId, phoneNumber)
    }
}