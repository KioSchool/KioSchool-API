package com.kioschool.kioschoolapi.domain.product.dto

data class CreateProductCategoryRequestBody(
    val name: String,
    val workspaceId: Long
)