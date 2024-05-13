package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.factory.AuthenticationSample
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.dto.ChangeOrderStatusRequestBody
import com.kioschool.kioschoolapi.order.service.OrderService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.core.context.SecurityContextHolder

class AdminOrderControllerTest : DescribeSpec({
    val service = mockk<OrderService>()
    val sut = AdminOrderController(service)
    val workspaceId = 1L

    extensions(AuthenticationSample)

    describe("getOrdersByCondition") {
        it("should call orderService.getAllOrdersByCondition") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.getAllOrdersByCondition(
                    "test",
                    workspaceId,
                    null,
                    null,
                    null
                )
            } returns emptyList()

            val result = sut.getOrdersByCondition(authentication, workspaceId, null, null, null)
            result shouldBe emptyList()
        }
    }

    describe("changeOrderStatus") {
        it("should call orderService.changeOrderStatus") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.changeOrderStatus(
                    "test",
                    workspaceId,
                    1,
                    "status"
                )
            } returns SampleEntity.order

            val result = sut.changeOrderStatus(
                authentication,
                ChangeOrderStatusRequestBody(
                    workspaceId,
                    1,
                    "status"
                )
            )
            result shouldBe SampleEntity.order
        }
    }
})