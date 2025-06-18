package com.kioschool.kioschoolapi.domain.product.dto

data class SortProductCategoriesRequestBody(
    val workspaceId: Long,
    val productCategoryIds: List<Long>
)