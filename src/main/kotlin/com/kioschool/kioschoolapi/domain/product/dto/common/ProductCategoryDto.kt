package com.kioschool.kioschoolapi.domain.product.dto.common

import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import java.time.LocalDateTime

data class ProductCategoryDto(
    val id: Long,
    val name: String,
    val index: Int?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(productCategory: ProductCategory): ProductCategoryDto {
            return ProductCategoryDto(
                id = productCategory.id,
                name = productCategory.name,
                index = productCategory.index,
                createdAt = productCategory.createdAt,
                updatedAt = productCategory.updatedAt
            )
        }
    }
}
