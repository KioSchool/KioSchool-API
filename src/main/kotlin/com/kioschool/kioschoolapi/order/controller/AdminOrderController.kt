package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.*
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
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

    @Operation(summary = "실시간 주문 조회", description = "실시간 주문을 조회합니다.")
    @GetMapping("/orders/realtime")
    fun getRealtimeOrders(
        authentication: Authentication,
        @RequestParam("workspaceId") workspaceId: Long
    ): List<Order> {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.getRealtimeOrders(username, workspaceId)
    }

    @Operation(summary = "주문 상태 변경", description = "주문을 상태를 변경합니다.")
    @PostMapping("/order/status")
    fun changeOrderStatus(
        authentication: Authentication,
        @RequestBody body: ChangeOrderStatusRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.changeOrderStatus(username, body.workspaceId, body.orderId, body.status)
    }
    
    @Operation(summary = "주문 별 상품 서빙 완료", description = "주문 별 상품의 서빙 상태를 변경합니다.")
    @PostMapping("/order/product")
    fun serveOrderProduct(
        authentication: Authentication,
        @RequestBody body: ServeOrderProductRequestBody
    ): OrderProduct {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.serveOrderProduct(
            username,
            body.workspaceId,
            body.orderProductId,
            body.isServed
        )
    }
}