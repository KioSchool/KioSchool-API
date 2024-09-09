package com.kioschool.kioschoolapi.product.facade

import com.kioschool.kioschoolapi.product.service.ProductService
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
) {
    fun getProducts(workspaceId: Long, categoryId: Long?) =
        productService.getAllProductsByCondition(workspaceId, categoryId)

    fun getProductCategories(workspaceId: Long) =
        productService.getAllProductCategories(workspaceId)
}