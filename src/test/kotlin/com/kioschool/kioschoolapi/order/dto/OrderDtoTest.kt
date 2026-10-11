package com.kioschool.kioschoolapi.order.dto

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
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
})
