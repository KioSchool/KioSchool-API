package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class CreateProductRequestBody(
    val name: String,
    val description: String,
    val price: Int,
    override val workspaceId: Long,
    val productCategoryId: Long?
) : WorkspaceAware