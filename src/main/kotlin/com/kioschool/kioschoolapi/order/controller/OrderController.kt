package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.facade.OrderFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Order Controller")
@RestController
class OrderController(
    private val orderFacade: OrderFacade
) {

    @Operation(summary = "주문 생성", description = "주문을 생성합니다.")
    @PostMapping("/order")
    fun createOrder(
        @RequestBody body: CreateOrderRequestBody
    ): Order {
        return orderFacade.createOrder(
            body.workspaceId,
            body.tableNumber,
            body.customerName,
            body.orderProducts
        )
    }

    @Operation(summary = "주문 조회", description = "주문을 조회합니다.")
    @GetMapping("/order")
    fun getOrder(
        @RequestParam("orderId") orderId: Long
    ): Order {
        return orderFacade.getOrder(orderId)
    }
}