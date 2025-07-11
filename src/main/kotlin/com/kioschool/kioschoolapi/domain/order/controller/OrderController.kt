package com.kioschool.kioschoolapi.domain.order.controller

import com.kioschool.kioschoolapi.domain.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.facade.OrderFacade
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

    @Operation(summary = "주문 가능 여부 조회", description = "주문 가능 여부를 조회합니다.")
    @GetMapping("/order/available")
    fun isOrderAvailable(
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("tableNumber") tableNumber: Int
    ): Boolean {
        return orderFacade.isOrderAvailable(workspaceId, tableNumber)
    }
}