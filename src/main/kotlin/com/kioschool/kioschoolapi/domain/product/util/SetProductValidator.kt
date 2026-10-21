package com.kioschool.kioschoolapi.domain.product.util

import com.kioschool.kioschoolapi.domain.product.dto.request.SetComponentRequestBody
import com.kioschool.kioschoolapi.global.error.dto.FieldErrorDetail

// 세트 생성·수정 요청 중 DB를 보지 않고 판단할 수 있는 검사. 오류를 모두 모아 돌려준다.
object SetProductValidator {
    const val MAX_COMPONENTS = 30

    // 세트 주문 수량 상한(999)과 곱해도 Int를 넘지 않게 한다.
    const val MAX_COMPONENT_QUANTITY = 999

    fun validate(
        name: String,
        originalPrice: Int,
        discountPrice: Int,
        components: List<SetComponentRequestBody>
    ): List<FieldErrorDetail> {
        val errors = mutableListOf<FieldErrorDetail>()

        if (name.isBlank()) errors += FieldErrorDetail("name", name, "세트 이름을 입력해주세요.")
        if (originalPrice < 0) {
            errors += FieldErrorDetail("originalPrice", originalPrice.toString(), "기존 금액은 0원 이상이어야 합니다.")
        }
        if (discountPrice < 0) {
            errors += FieldErrorDetail("discountPrice", discountPrice.toString(), "할인 금액은 0원 이상이어야 합니다.")
        }
        if (originalPrice >= 0 && discountPrice >= 0) {
            if (discountPrice > originalPrice) {
                errors += FieldErrorDetail("discountPrice", discountPrice.toString(), "할인 금액은 기존 금액보다 클 수 없습니다.")
            } else if (discountPrice == originalPrice) {
                errors += FieldErrorDetail("discountPrice", discountPrice.toString(), "판매가는 1원 이상이어야 합니다.")
            }
        }

        if (components.isEmpty()) {
            errors += FieldErrorDetail("components", "0", "구성품을 1개 이상 입력해주세요.")
        }
        if (components.size > MAX_COMPONENTS) {
            errors += FieldErrorDetail(
                "components",
                components.size.toString(),
                "구성품은 ${MAX_COMPONENTS}개까지 입력할 수 있습니다."
            )
        }
        components.forEachIndexed { i, component ->
            if (component.name.isBlank()) {
                errors += FieldErrorDetail("components[$i].name", component.name, "구성품 이름을 입력해주세요.", i)
            }
            if (component.quantity < 1) {
                errors += FieldErrorDetail(
                    "components[$i].quantity", component.quantity.toString(), "구성품 수량은 1개 이상이어야 합니다.", i
                )
            } else if (component.quantity > MAX_COMPONENT_QUANTITY) {
                errors += FieldErrorDetail(
                    "components[$i].quantity",
                    component.quantity.toString(),
                    "구성품 수량은 ${MAX_COMPONENT_QUANTITY}개 이하여야 합니다.",
                    i
                )
            }
        }
        return errors
    }
}
