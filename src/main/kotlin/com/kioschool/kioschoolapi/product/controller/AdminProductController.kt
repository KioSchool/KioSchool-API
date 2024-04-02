package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.dto.CreateProductCategoryRequestBody
import com.kioschool.kioschoolapi.product.dto.CreateProductRequestBody
import com.kioschool.kioschoolapi.product.dto.UpdateProductRequestBody
import com.kioschool.kioschoolapi.product.dto.UpdateProductSellableRequestBody
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Admin Product Controller")
@RestController
@RequestMapping("/admin")
class AdminProductController(
    private val productService: ProductService
) {
    @Operation(summary = "상품 전체 조회", description = "워크스페이스에 등록된 모든 상품을 조회합니다.")
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long
    ) = productService.getAllProductsByCondition(workspaceId)

    @Operation(summary = "상품 조회", description = "상품 하나를 조회합니다.")
    @GetMapping("/product")
    fun getProduct(
        @RequestParam productId: Long
    ) = productService.getProduct(productId)

    @Operation(summary = "상품 카테고리 조회", description = "워크스페이스에 등록된 모든 상품 카테고리를 조회합니다.")
    @GetMapping("/product-categories")
    fun getProductCategories(
        @RequestParam workspaceId: Long
    ) = productService.getAllProductCategories(workspaceId)

    @Operation(summary = "상품 생성", description = "상품을 생성합니다.")
    @PostMapping("/product", consumes = [MediaType.ALL_VALUE])
    fun createOrUpdateProduct(
        authentication: Authentication,
        @RequestPart body: CreateProductRequestBody,
        @RequestPart file: MultipartFile?
    ): Product {
        val username = (authentication.principal as CustomUserDetails).username
        return productService.createProduct(
            username,
            body.workspaceId,
            body.name,
            body.description,
            body.price,
            body.productCategoryId,
            file
        )
    }

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @PutMapping("/product", consumes = [MediaType.ALL_VALUE])
    fun updateProduct(
        authentication: Authentication,
        @RequestPart body: UpdateProductRequestBody,
        @RequestPart file: MultipartFile?
    ): Product {
        val username = (authentication.principal as CustomUserDetails).username
        return productService.updateProduct(
            username,
            body.workspaceId,
            body.productId,
            body.name,
            body.description,
            body.price,
            body.productCategoryId,
            file
        )
    }

    @Operation(summary = "상품 판매 여부 수정", description = "상품의 판매 여부를 수정합니다.")
    @PutMapping("/product/sellable")
    fun updateProductSellable(
        authentication: Authentication,
        @RequestBody body: UpdateProductSellableRequestBody,
    ): Product {
        val username = (authentication.principal as CustomUserDetails).username
        return productService.updateProductSellable(
            username,
            body.workspaceId,
            body.productId,
            body.isSellable
        )
    }

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @DeleteMapping("/product")
    fun deleteProduct(
        authentication: Authentication,
        @RequestParam workspaceId: Long,
        @RequestParam productId: Long
    ) = productService.deleteProduct(
        (authentication.principal as CustomUserDetails).username,
        workspaceId,
        productId
    )

    @Operation(summary = "상품 카테고리 생성", description = "상품 카테고리를 생성합니다.")
    @PostMapping("/product-category")
    fun createProductCategory(
        authentication: Authentication,
        @RequestBody body: CreateProductCategoryRequestBody
    ) = productService.createProductCategory(
        (authentication.principal as CustomUserDetails).username,
        body.workspaceId,
        body.name
    )

    @Operation(summary = "상품 카테고리 삭제", description = "상품 카테고리를 삭제합니다.")
    @DeleteMapping("/product-category")
    fun deleteProductCategory(
        authentication: Authentication,
        @RequestParam workspaceId: Long,
        @RequestParam productCategoryId: Long
    ) = productService.deleteProductCategory(
        (authentication.principal as CustomUserDetails).username,
        workspaceId,
        productCategoryId
    )
}