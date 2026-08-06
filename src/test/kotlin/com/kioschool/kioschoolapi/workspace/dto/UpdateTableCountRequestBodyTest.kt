package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.request.UpdateTableCountRequestBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class UpdateTableCountRequestBodyTest : DescribeSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    describe("tableCount validation") {
        it("should reject tableCount below 1") {
            val violations = validator.validate(UpdateTableCountRequestBody(1L, 0))

            violations.size shouldBe 1
            violations.first().message shouldBe "테이블 개수는 1개 이상이어야 합니다."
        }

        it("should reject tableCount above 100") {
            val violations = validator.validate(UpdateTableCountRequestBody(1L, 101))

            violations.size shouldBe 1
            violations.first().message shouldBe "테이블 개수는 100개 이하여야 합니다."
        }

        it("should accept tableCount at both bounds") {
            validator.validate(UpdateTableCountRequestBody(1L, 1)).shouldBeEmpty()
            validator.validate(UpdateTableCountRequestBody(1L, 100)).shouldBeEmpty()
        }
    }
})
