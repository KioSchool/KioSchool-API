package com.kioschool.kioschoolapi.domain.product.dto.common

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import java.time.LocalDateTime

// 손님용 상품 응답. status는 연결 상품까지 본 판매 상태다.
data class ProductDto(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    // 세트의 할인 금액(할인 전 금액 = price + discountPrice). 일반 상품은 null.
    // 배포 전에 캐시된 응답에는 이 필드가 없어서 기본값을 둔다.
    val discountPrice: Int? = null,
    val imageUrl: String?,
    val status: ProductStatus?,
    val productCategory: ProductCategoryDto?,
    val index: Int?,
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
                discountPrice = product.discountPrice,
                imageUrl = product.imageUrl,
                status = product.effectiveStatus(),
                productCategory = product.productCategory?.let { ProductCategoryDto.of(it) },
                index = product.index,
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}
