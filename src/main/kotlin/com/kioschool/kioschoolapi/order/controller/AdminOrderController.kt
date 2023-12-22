package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CancelOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.PayOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.ServeOrderRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin Order Controller")
@RestController
@RequestMapping("/admin")
class AdminOrderController(
    private val orderService: OrderService
) {

    @Operation(summary = "주문 조회", description = "주문을 조회합니다. 조건을 입력하지 않으면 모든 주문을 조회합니다.")
    @GetMapping("/orders")
    fun getOrdersByCondition(
        authentication: Authentication,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("startDate") startDate: String? = null,
        @RequestParam("endDate") endDate: String? = null,
        @RequestParam("status") status: String? = null
    ): List<Order> {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.getAllOrdersByCondition(
            username,
            workspaceId,
            startDate,
            endDate,
            status
        )
    }

    @Operation(summary = "주문 취소", description = "주문 상태를 취소로 변경합니다.")
    @PostMapping("/order/cancel")
    fun cancelOrder(
        authentication: Authentication,
        @RequestBody body: CancelOrderRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.cancelOrder(username, body.workspaceId, body.orderId)
    }

    @Operation(summary = "주문 완료", description = "주문 상태를 완료로 변경합니다.")
    @PostMapping("/order/serve")
    fun serveOrder(
        authentication: Authentication,
        @RequestBody body: ServeOrderRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.serveOrder(username, body.workspaceId, body.orderId)
    }

    @Operation(summary = "주문 결제 완료", description = "주문 상태를 결제 완료로 변경합니다.")
    @PostMapping("/order/pay")
    fun payOrder(
        authentication: Authentication,
        @RequestBody body: PayOrderRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.payOrder(username, body.workspaceId, body.orderId)
    }
}