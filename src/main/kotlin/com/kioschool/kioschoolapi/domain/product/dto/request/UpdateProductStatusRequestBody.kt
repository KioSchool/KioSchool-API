package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.enums.ProductStatus

class UpdateProductStatusRequestBody(
    val workspaceId: Long,
    val productId: Long,
    val status: ProductStatus
)