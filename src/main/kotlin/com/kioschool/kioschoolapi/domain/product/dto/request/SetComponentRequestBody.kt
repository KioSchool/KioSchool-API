package com.kioschool.kioschoolapi.domain.product.dto.request

data class SetComponentRequestBody(
    val name: String,
    // 세트 1개당 수량
    val quantity: Int,
    // 품절을 따라갈 같은 주점의 일반 상품. 없으면 null
    val conditionProductId: Long?
)
