package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class CreateProductCategoryRequestBody(
    val name: String,
    override val workspaceId: Long
) : WorkspaceAware