package com.kioschool.kioschoolapi.domain.product.dto

import com.kioschool.kioschoolapi.domain.product.entity.Product
import java.time.LocalDateTime

data class ProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    val imageUrl: String?,
    val isSellable: Boolean?,
    val productCategory: ProductCategoryDto?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                imageUrl = product.imageUrl,
                isSellable = product.isSellable,
                productCategory = product.productCategory?.let { ProductCategoryDto.of(it) },
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}
