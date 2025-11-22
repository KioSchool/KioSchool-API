package com.kioschool.kioschoolapi.domain.product.dto.common

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import java.time.LocalDateTime

data class ProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    val imageUrl: String?,
    val isSellable: Boolean?,
    val status: ProductStatus?,
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
                status = product.status,
                productCategory = product.productCategory?.let { ProductCategoryDto.of(it) },
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}
