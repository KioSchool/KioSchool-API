package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.factory.AuthenticationSample
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.dto.CancelOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.PayOrderRequestBody
import com.kioschool.kioschoolapi.order.dto.ServeOrderRequestBody
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

    describe("cancelOrder") {
        it("should call orderService.cancelOrder") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.cancelOrder(
                    "test",
                    workspaceId,
                    1
                )
            } returns SampleEntity.order

            val result = sut.cancelOrder(
                authentication,
                CancelOrderRequestBody(
                    workspaceId,
                    1
                )
            )
            result shouldBe SampleEntity.order
        }
    }

    describe("serveOrder") {
        it("should call orderService.serveOrder") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.serveOrder(
                    "test",
                    workspaceId,
                    1
                )
            } returns SampleEntity.order

            val result = sut.serveOrder(
                authentication,
                ServeOrderRequestBody(
                    workspaceId,
                    1
                )
            )
            result shouldBe SampleEntity.order
        }
    }

    describe("payOrder") {
        it("should call orderService.payOrder") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.payOrder(
                    "test",
                    workspaceId,
                    1
                )
            } returns SampleEntity.order

            val result = sut.payOrder(
                authentication,
                PayOrderRequestBody(
                    workspaceId,
                    1
                )
            )
            result shouldBe SampleEntity.order
        }
    }
})