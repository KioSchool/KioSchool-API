package com.kioschool.kioschoolapi.domain.product.dto.request

data class UpdateProductSellableRequestBody(
    val workspaceId: Long,
    val productId: Long,
    val isSellable: Boolean
)
