package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Order Controller")
@RestController
class OrderController(
    private val orderService: OrderService
) {

    @Operation(summary = "주문 생성", description = "주문을 생성합니다.")
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

    @Operation(summary = "전화번호로 주문 조회", description = "전화번호로 주문을 조회합니다.")
    @GetMapping("/order")
    fun getOrdersByPhoneNumber(
        @RequestParam phoneNumber: String,
        @RequestParam workspaceId: Long,
    ): List<Order> {
        return orderService.getOrdersByPhoneNumber(workspaceId, phoneNumber)
    }
}