package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class UpdateProductSellableRequestBody(
    override val workspaceId: Long,
    val productId: Long,
    val isSellable: Boolean
) : WorkspaceAware
