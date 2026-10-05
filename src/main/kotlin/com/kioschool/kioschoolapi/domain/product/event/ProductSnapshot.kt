package com.kioschool.kioschoolapi.domain.product.event

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus

data class ProductSnapshot(
    val name: String,
    val price: Int,
    val status: ProductStatus,
    val categoryId: Long?
) {
    companion object {
        fun of(product: Product) = ProductSnapshot(
            name = product.name,
            price = product.price,
            status = product.status,
            categoryId = product.productCategory?.id
        )
    }
}
