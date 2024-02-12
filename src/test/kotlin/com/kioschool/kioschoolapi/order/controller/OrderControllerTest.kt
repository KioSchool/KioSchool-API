package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.order.dto.CreateOrderRequestBody
import com.kioschool.kioschoolapi.order.service.OrderService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class OrderControllerTest : DescribeSpec({
    val service = mockk<OrderService>()
    val sut = OrderController(service)
    val workspaceId = 1L

    describe("createOrder") {
        it("should call orderService.createOrder") {
            every {
                service.createOrder(
                    workspaceId,
                    1,
                    "customer name",
                    emptyList()
                )
            } returns SampleEntity.order

            val result = sut.createOrder(
                CreateOrderRequestBody(
                    workspaceId,
                    1,
                    emptyList(),
                    "customer name"
                )
            )
            result shouldBe SampleEntity.order
        }
    }

    describe("getOrder") {
        it("should call service.getOrder") {
            every { service.getOrder(1L) } returns SampleEntity.order

            val result = sut.getOrder(
                1L
            )
            result shouldBe SampleEntity.order
        }
    }
})