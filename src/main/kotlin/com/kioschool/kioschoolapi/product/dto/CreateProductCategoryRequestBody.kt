package com.kioschool.kioschoolapi.product.dto

data class CreateProductCategoryRequestBody(
    val name: String,
    val workspaceId: Long
)