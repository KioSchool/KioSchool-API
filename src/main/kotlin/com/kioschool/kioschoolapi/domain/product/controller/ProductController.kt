package com.kioschool.kioschoolapi.domain.product.controller

import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.product.facade.ProductFacade
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Product Controller")
@RestController
class ProductController(
    private val productFacade: ProductFacade
) {
    @Operation(
        summary = "상품 조회",
        description = "워크스페이스에 등록된 상품을 조회합니다. 카테고리를 입력하지 않으면 모든 상품을 조회합니다."
    )
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long,
        @RequestParam categoryId: Long? = null
    ): List<ProductDto> {
        return productFacade.getProducts(workspaceId, categoryId).map { ProductDto.of(it) }
    }

    @Operation(
        summary = "상품 카테고리 조회",
        description = "워크스페이스에 등록된 모든 상품 카테고리를 조회합니다."
    )
    @GetMapping("/product-categories")
    @Cacheable(cacheNames = [CacheNames.PRODUCT_CATEGORIES], key = "#workspaceId")
    fun getProductCategories(
        @RequestParam workspaceId: Long
    ): List<ProductCategoryDto> {
        return productFacade.getProductCategories(workspaceId).map { ProductCategoryDto.of(it) }
    }
}