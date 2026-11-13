package com.kioschool.kioschoolapi.order.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kioschool.kioschoolapi.domain.order.dto.request.CreateOrderRequestBody
import com.kioschool.kioschoolapi.global.common.enums.PaymentMethod
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CreateOrderRequestBodyTest : DescribeSpec({
    val mapper = jacksonObjectMapper()

    describe("paymentMethod") {
        it("카카오페이를 고른 주문을 받는다") {
            val body = mapper.readValue<CreateOrderRequestBody>(
                """{"workspaceId":1,"tableHash":"h","orderProducts":[],"customerName":"손님","paymentMethod":"KAKAOPAY"}"""
            )

            body.paymentMethod shouldBe PaymentMethod.KAKAOPAY
        }
    }
})
