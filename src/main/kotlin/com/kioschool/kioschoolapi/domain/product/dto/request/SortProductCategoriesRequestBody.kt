package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class SortProductCategoriesRequestBody(
    override val workspaceId: Long,
    val productCategoryIds: List<Long>
) : WorkspaceAware