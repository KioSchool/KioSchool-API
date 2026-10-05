package com.kioschool.kioschoolapi.domain.product.event

data class ProductDeletedEvent(
    val workspaceId: Long,
    val productId: Long,
    val snapshot: ProductSnapshot
)
