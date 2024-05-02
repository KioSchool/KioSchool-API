package com.kioschool.kioschoolapi.product.dto

data class SortProductCategoriesRequestBody(
    val workspaceId: Long,
    val productCategoryIds: List<Long>
)