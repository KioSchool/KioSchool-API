package com.kioschool.kioschoolapi.error

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.dto.ErrorResponse
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class ErrorResponseTest : DescribeSpec({
    describe("ErrorResponse.of") {
        it("ErrorCode로부터 code/message/status를 채운다") {
            val res = ErrorResponse.of(ErrorCode.USER_NOT_FOUND, "/user/me")
            res.code shouldBe "USER_NOT_FOUND"
            res.message shouldBe "사용자를 찾을 수 없습니다."
            res.status shouldBe 404
            res.path shouldBe "/user/me"
            res.errors shouldBe emptyList()
        }

        it("message 오버라이드가 우선한다") {
            val res = ErrorResponse.of(ErrorCode.INVALID_INPUT, "/x", message = "직접 메시지")
            res.message shouldBe "직접 메시지"
        }
    }

    describe("details") {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        it("값이 없으면 JSON에 details 키가 없다") {
            val json = mapper.writeValueAsString(ErrorResponse.of(ErrorCode.INVALID_INPUT, "/x"))
            json shouldNotContain "details"
        }

        it("값이 있으면 그대로 실린다") {
            val res = ErrorResponse.of(
                ErrorCode.NOT_SELLABLE_PRODUCT, "/order",
                details = mapOf("notSellableProductIds" to listOf(20L))
            )
            mapper.readTree(mapper.writeValueAsString(res))["details"]["notSellableProductIds"][0].asLong() shouldBe 20L
        }

        it("새 에러 코드의 HTTP 상태") {
            ErrorCode.PRODUCT_USED_AS_SET_CONDITION.status.value() shouldBe 409
            ErrorCode.PRODUCT_TYPE_MISMATCH.status.value() shouldBe 400
            ErrorCode.NOT_FOUND_ORDER_PRODUCT_COMPONENT.status.value() shouldBe 404
        }
    }
})
