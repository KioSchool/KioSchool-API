package com.kioschool.kioschoolapi.product.dto

data class UpdateProductSellableRequestBody(
    val workspaceId: Long,
    val productId: Long,
    val isSellable: Boolean
)
