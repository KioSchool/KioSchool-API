package com.kioschool.kioschoolapi.domain.product.dto.request

data class SortProductCategoriesRequestBody(
    val workspaceId: Long,
    val productCategoryIds: List<Long>
)