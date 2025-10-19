package com.kioschool.kioschoolapi.domain.product.dto.request

data class CreateProductCategoryRequestBody(
    val name: String,
    val workspaceId: Long
)