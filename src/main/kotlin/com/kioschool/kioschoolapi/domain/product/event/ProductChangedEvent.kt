package com.kioschool.kioschoolapi.domain.product.event

data class ProductChangedEvent(
    val workspaceId: Long,
    val productId: Long,
    val before: ProductSnapshot,
    val after: ProductSnapshot
)
