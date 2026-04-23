package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus

class UpdateProductStatusRequestBody(
    override val workspaceId: Long,
    val productId: Long,
    val status: ProductStatus
) : WorkspaceAware