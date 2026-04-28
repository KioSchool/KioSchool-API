package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class CategoryProductSortInfo(
    val categoryId: Long?,
    val productIds: List<Long>
)

data class SortProductsRequestBody(
    override val workspaceId: Long,
    val sorts: List<CategoryProductSortInfo>
) : WorkspaceAware
