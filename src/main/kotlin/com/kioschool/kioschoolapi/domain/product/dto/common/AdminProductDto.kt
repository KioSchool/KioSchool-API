package com.kioschool.kioschoolapi.domain.product.dto.common

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.SetComponent
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import java.time.LocalDateTime

// 관리자 API용 상품 응답. status는 관리자가 정한 수동 상태이고, 실제로 주문할 수 있는지는 effectiveStatus로 본다.
data class AdminProductDto(
    val id: Long,
    val type: ProductType,
    val name: String,
    val description: String,
    val price: Int,
    val originalPrice: Int?,
    val discountPrice: Int?,
    val imageUrl: String?,
    val status: ProductStatus,
    val effectiveStatus: ProductStatus,
    val productCategory: ProductCategoryDto?,
    val index: Int?,
    val components: List<AdminComponentDto>,
    // 이 상품을 구성품의 연결 상품으로 쓰는 세트. 품절·삭제 전 확인창에 쓴다.
    val linkedSets: List<LinkedSetDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(product: Product, linkedSets: List<Product>): AdminProductDto {
            return AdminProductDto(
                id = product.id,
                type = product.type,
                name = product.name,
                description = product.description,
                price = product.price,
                originalPrice = product.originalPrice,
                discountPrice = product.discountPrice,
                imageUrl = product.imageUrl,
                status = product.status,
                effectiveStatus = product.effectiveStatus(),
                productCategory = product.productCategory?.let { ProductCategoryDto.of(it) },
                index = product.index,
                components = product.components.map { AdminComponentDto.of(it) },
                linkedSets = linkedSets.map { LinkedSetDto.of(it) },
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}

data class AdminComponentDto(
    val id: Long,
    val name: String,
    val quantity: Int,
    val displayOrder: Int,
    val conditionProduct: ConditionProductDto?
) {
    companion object {
        fun of(component: SetComponent) = AdminComponentDto(
            id = component.id,
            name = component.name,
            quantity = component.quantity,
            displayOrder = component.displayOrder,
            conditionProduct = component.conditionProduct?.let { ConditionProductDto.of(it) }
        )
    }
}

// 연결 상품. status는 연결 상품 자체의 상태다.
data class ConditionProductDto(
    val id: Long,
    val name: String,
    val status: ProductStatus
) {
    companion object {
        fun of(product: Product) = ConditionProductDto(product.id, product.name, product.status)
    }
}

data class LinkedSetDto(
    val id: Long,
    val name: String,
    val effectiveStatus: ProductStatus
) {
    companion object {
        fun of(set: Product) = LinkedSetDto(set.id, set.name, set.effectiveStatus())
    }
}
