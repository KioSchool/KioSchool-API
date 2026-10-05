package com.kioschool.kioschoolapi.order.dto

import com.kioschool.kioschoolapi.domain.order.dto.common.SuperAdminOrderDto
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SuperAdminOrderDtoTest : DescribeSpec({
    describe("paymentMethod mapping") {
        it("should carry the payment method of the order") {
            val order = Order(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                customerName = "c1",
                orderNumber = 1,
                orderSession = null,
                paymentMethod = PaymentMethod.BANK_TRANSFER
            )

            SuperAdminOrderDto.of(order).paymentMethod shouldBe PaymentMethod.BANK_TRANSFER
        }

        it("should map a missing payment method to null") {
            val order = Order(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                customerName = "c1",
                orderNumber = 1,
                orderSession = null
            )

            SuperAdminOrderDto.of(order).paymentMethod shouldBe null
        }
    }
})
