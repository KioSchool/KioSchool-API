package com.kioschool.kioschoolapi.domain.product.controller

import com.kioschool.kioschoolapi.domain.product.dto.*
import com.kioschool.kioschoolapi.domain.product.facade.ProductFacade
import com.kioschool.kioschoolapi.global.common.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Admin Product Controller")
@RestController
@RequestMapping("/admin")
class AdminProductController(
    private val productFacade: ProductFacade
) {
    @Operation(summary = "상품 전체 조회", description = "워크스페이스에 등록된 모든 상품을 조회합니다.")
    @GetMapping("/products")
    fun getProducts(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): List<ProductDto> {
        return productFacade.getProducts(username, workspaceId).map { ProductDto.of(it) }
    }

    @Operation(summary = "상품 조회", description = "상품 하나를 조회합니다.")
    @GetMapping("/product")
    fun getProduct(
        @AdminUsername username: String,
        @RequestParam productId: Long
    ): ProductDto {
        return ProductDto.of(productFacade.getProduct(username, productId))
    }

    @Operation(summary = "상품 카테고리 조회", description = "워크스페이스에 등록된 모든 상품 카테고리를 조회합니다.")
    @GetMapping("/product-categories")
    fun getProductCategories(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): List<ProductCategoryDto> {
        return productFacade.getProductCategories(username, workspaceId).map { ProductCategoryDto.of(it) }
    }

    @Operation(summary = "상품 생성", description = "상품을 생성합니다.")
    @PostMapping("/product", consumes = [MediaType.ALL_VALUE])
    fun createOrUpdateProduct(
        @AdminUsername username: String,
        @RequestPart body: CreateProductRequestBody,
        @RequestPart file: MultipartFile?
    ): ProductDto {
        return ProductDto.of(productFacade.createProduct(
            username,
            body.workspaceId,
            body.name,
            body.description,
            body.price,
            body.productCategoryId,
            file
        ))
    }

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @PutMapping("/product", consumes = [MediaType.ALL_VALUE])
    fun updateProduct(
        @AdminUsername username: String,
        @RequestPart body: UpdateProductRequestBody,
        @RequestPart file: MultipartFile?
    ): ProductDto {
        return ProductDto.of(productFacade.updateProduct(
            username,
            body.workspaceId,
            body.productId,
            body.name,
            body.description,
            body.price,
            body.productCategoryId,
            file
        ))
    }

    @Operation(summary = "상품 판매 여부 수정", description = "상품의 판매 여부를 수정합니다.")
    @PutMapping("/product/sellable")
    fun updateProductSellable(
        @AdminUsername username: String,
        @RequestBody body: UpdateProductSellableRequestBody,
    ): ProductDto {
        return ProductDto.of(productFacade.updateProductSellable(
            username,
            body.workspaceId,
            body.productId,
            body.isSellable
        ))
    }

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @DeleteMapping("/product")
    fun deleteProduct(
        @AdminUsername username: String,
        @RequestParam productId: Long
    ) = productFacade.deleteProduct(username, productId)

    @Operation(summary = "상품 카테고리 생성", description = "상품 카테고리를 생성합니다.")
    @PostMapping("/product-category")
    fun createProductCategory(
        @AdminUsername username: String,
        @RequestBody body: CreateProductCategoryRequestBody
    ) = productFacade.createProductCategory(
        username,
        body.workspaceId,
        body.name
    )

    @Operation(summary = "상품 카테고리 삭제", description = "상품 카테고리를 삭제합니다.")
    @DeleteMapping("/product-category")
    fun deleteProductCategory(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long,
        @RequestParam productCategoryId: Long
    ) = productFacade.deleteProductCategory(username, workspaceId, productCategoryId)

    @Operation(summary = "상품 카테고리 정렬", description = "주어진 순서대로 상품 카테고리를 정렬합니다.")
    @PostMapping("/product-categories/sort")
    fun sortProductCategories(
        @AdminUsername username: String,
        @RequestBody body: SortProductCategoriesRequestBody
    ): List<ProductCategoryDto> {
        return productFacade.sortProductCategories(
            username,
            body.workspaceId,
            body.productCategoryIds
        ).map { ProductCategoryDto.of(it) }
    }
}