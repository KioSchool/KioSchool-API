package com.kioschool.kioschoolapi.domain.product.dto

data class CreateProductRequestBody(
    val name: String,
    val description: String,
    val price: Int,
    val workspaceId: Long,
    val productCategoryId: Long?
)