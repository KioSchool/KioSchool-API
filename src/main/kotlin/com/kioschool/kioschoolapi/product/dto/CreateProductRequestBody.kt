package com.kioschool.kioschoolapi.product.dto

data class CreateProductRequestBody(
    val name: String,
    val description: String,
    val price: Int,
    val workspaceId: Long,
    val productCategoryId: Long?
)