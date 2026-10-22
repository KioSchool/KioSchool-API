package com.kioschool.kioschoolapi.order.dto

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderProductDto
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class OrderDtoTest : DescribeSpec({
    describe("paymentMethod mapping") {
        it("should include the selected payment method") {
            val order = Order(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                customerName = "c1",
                orderNumber = 1,
                orderSession = null,
                paymentMethod = PaymentMethod.TOSS
            )

            OrderDto.of(order).paymentMethod shouldBe PaymentMethod.TOSS
        }

        it("should include null for an order without a recorded payment method") {
            val order = Order(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                customerName = "c1",
                orderNumber = 1,
                orderSession = null
            )

            OrderDto.of(order).paymentMethod shouldBe null
        }
    }

    describe("OrderProductDto") {
        it("세트는 할인 금액과 구성품, 구성품 전체 수량을 싣는다") {
            val dto = OrderProductDto.of(SampleEntity.newSetOrderProduct(1, quantity = 2, componentQuantities = listOf(1, 2)))

            dto.discountPrice shouldBe 3000
            dto.components.map { it.totalQuantity } shouldBe listOf(2, 4)
            dto.components.map { it.quantity } shouldBe listOf(1, 2)
            dto.components.map { it.displayOrder } shouldBe listOf(0, 1)
            dto.components.map { it.id } shouldBe listOf(200L, 201L)
        }

        it("일반 상품은 구성품이 비고 할인 금액이 null") {
            val dto = OrderProductDto.of(SampleEntity.orderProduct1)

            dto.components shouldBe emptyList()
            dto.discountPrice shouldBe null
        }
    }
})
