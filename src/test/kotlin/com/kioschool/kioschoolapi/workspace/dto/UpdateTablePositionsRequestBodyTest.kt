package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionUpdateDto
import com.kioschool.kioschoolapi.domain.workspace.dto.request.UpdateTablePositionsRequestBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class UpdateTablePositionsRequestBodyTest : DescribeSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    fun positions(count: Int) = (1..count).map {
        TablePositionUpdateDto(it.toLong(), TablePositionDto(it, 0))
    }

    describe("positions validation") {
        it("should reject more positions than a workspace can have tables") {
            val violations = validator.validate(UpdateTablePositionsRequestBody(1L, positions(101)))

            violations.size shouldBe 1
            violations.first().message shouldBe "한 번에 100개 테이블까지 저장할 수 있습니다."
            violations.first().propertyPath.toString() shouldBe "positions"
        }

        it("should accept a request at the limit") {
            validator.validate(UpdateTablePositionsRequestBody(1L, positions(100))).shouldBeEmpty()
        }

        it("should accept an empty request") {
            validator.validate(UpdateTablePositionsRequestBody(1L, emptyList())).shouldBeEmpty()
        }
    }
})
