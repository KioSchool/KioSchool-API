package com.kioschool.kioschoolapi.product.util

import com.kioschool.kioschoolapi.domain.product.dto.request.SetComponentRequestBody
import com.kioschool.kioschoolapi.domain.product.util.SetProductValidator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SetProductValidatorTest : DescribeSpec({
    val ok = listOf(SetComponentRequestBody("족발 소", 1, 10), SetComponentRequestBody("막국수", 1, 11))

    describe("SetProductValidator.validate") {
        it("정상 입력은 오류 없음") {
            SetProductValidator.validate("세트", 28000, 3000, ok) shouldBe emptyList()
        }

        it("할인 금액이 기존 금액과 같으면 판매가 1원 규칙") {
            val error = SetProductValidator.validate("세트", 28000, 28000, ok).single()
            error.field shouldBe "discountPrice"
            error.reason shouldBe "판매가는 1원 이상이어야 합니다."
        }

        it("할인 금액이 기존 금액보다 크면 그 사유 하나만") {
            SetProductValidator.validate("세트", 1000, 2000, ok).map { it.reason } shouldBe
                listOf("할인 금액은 기존 금액보다 클 수 없습니다.")
        }

        it("음수 할인 금액") {
            SetProductValidator.validate("세트", 1000, -1, ok).map { it.reason } shouldBe
                listOf("할인 금액은 0원 이상이어야 합니다.")
        }

        it("배열 항목 오류는 index를 단다") {
            val errors = SetProductValidator.validate("세트", 28000, 3000, listOf(ok[0], SetComponentRequestBody(" ", 0, null)))
            errors.map { it.field to it.index } shouldBe listOf("components[1].name" to 1, "components[1].quantity" to 1)
            errors.map { it.reason } shouldBe listOf("구성품 이름을 입력해주세요.", "구성품 수량은 1개 이상이어야 합니다.")
        }

        it("구성품 수량 상한 999") {
            val error = SetProductValidator.validate("세트", 28000, 3000, listOf(SetComponentRequestBody("a", Int.MAX_VALUE, null))).single()
            error.field shouldBe "components[0].quantity"
            error.reason shouldBe "구성품 수량은 999개 이하여야 합니다."
            SetProductValidator.validate("세트", 28000, 3000, listOf(SetComponentRequestBody("a", 999, null))) shouldBe emptyList()
        }

        it("구성품 0개, 31개") {
            SetProductValidator.validate("세트", 28000, 3000, emptyList()).single().let {
                it.field shouldBe "components"
                it.reason shouldBe "구성품을 1개 이상 입력해주세요."
            }
            SetProductValidator.validate("세트", 28000, 3000, List(31) { ok[0] }).single().reason shouldBe
                "구성품은 30개까지 입력할 수 있습니다."
            SetProductValidator.validate("세트", 28000, 3000, List(30) { ok[0] }) shouldBe emptyList()
        }

        it("이름 공백, 음수 금액") {
            val errors = SetProductValidator.validate(" ", -1, 0, ok)
            errors.map { it.field } shouldBe listOf("name", "originalPrice")
            errors.map { it.reason } shouldBe listOf("세트 이름을 입력해주세요.", "기존 금액은 0원 이상이어야 합니다.")
            errors.first { it.field == "originalPrice" }.value shouldBe "-1"
        }
    }
})
