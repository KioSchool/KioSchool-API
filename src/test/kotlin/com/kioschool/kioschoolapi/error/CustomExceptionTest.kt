package com.kioschool.kioschoolapi.error

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CustomExceptionTest : DescribeSpec({
    describe("CustomException") {
        it("message가 없으면 ErrorCode.defaultMessage를 사용한다") {
            val ex = CustomException(ErrorCode.USER_NOT_FOUND)
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            ex.message shouldBe "사용자를 찾을 수 없습니다."
        }

        it("message가 주어지면 그 값을 사용한다") {
            val ex = CustomException(ErrorCode.INVALID_INPUT, "커스텀 메시지")
            ex.message shouldBe "커스텀 메시지"
        }

        it("cause를 전달할 수 있다") {
            val root = IllegalStateException("boom")
            val ex = CustomException(ErrorCode.EMAIL_SEND_FAILURE, cause = root)
            ex.cause shouldBe root
        }
    }

    describe("ErrorCode") {
        it("status가 의도한 값으로 보존된다(하위호환 핵심 표본)") {
            ErrorCode.WORKSPACE_INACCESSIBLE.status.value() shouldBe 405
            ErrorCode.NOT_SELLABLE_PRODUCT.status.value() shouldBe 406
            ErrorCode.NOT_VERIFIED_EMAIL_DOMAIN.status.value() shouldBe 422
            ErrorCode.NO_PERMISSION.status.value() shouldBe 401
            ErrorCode.INCORRECT_ACCOUNT_HOLDER.status.value() shouldBe 404
            ErrorCode.EMAIL_SEND_FAILURE.status.value() shouldBe 500
        }
    }
})
