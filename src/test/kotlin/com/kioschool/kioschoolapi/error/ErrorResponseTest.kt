package com.kioschool.kioschoolapi.error

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.dto.ErrorResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

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
})
