package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.dto.CreateOrUpdateProductRequestBody
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.dto.ExceptionResponseBody
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
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
    @Operation(summary = "상품 조회", description = "워크스페이스에 등록된 모든 상품을 조회합니다.")
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long
    ) = productService.getAllProductsByCondition(workspaceId)

    @Operation(summary = "상품 카테고리 조회", description = "워크스페이스에 등록된 모든 상품 카테고리를 조회합니다.")
    @GetMapping("/product-categories")
    fun getProductCategories(
        @RequestParam workspaceId: Long
    ) = productService.getAllProductCategories(workspaceId)

    @Operation(summary = "상품 생성/수정", description = "상품을 생성/수정합니다.<br>상품 ID가 있으면 수정, 없으면 생성합니다.")
    @PostMapping("/product", consumes = [MediaType.ALL_VALUE])
    fun createOrUpdateProduct(
        authentication: Authentication,
        @RequestPart body: CreateOrUpdateProductRequestBody,
        @RequestPart file: MultipartFile?
    ): Product {
        val username = (authentication.principal as CustomUserDetails).username
        return if (body.productId == null)
            productService.createProduct(
                username,
                body.workspaceId,
                body.name,
                body.description,
                body.price,
                body.productCategoryId,
                file
            ) else
            productService.updateProduct(
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

    @Operation(summary = "상품 카테고리 생성", description = "상품 카테고리를 생성합니다.")
    @PostMapping("/product-category")
    fun createProductCategory(
        authentication: Authentication,
        @RequestBody body: CreateOrUpdateProductRequestBody
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

    @ExceptionHandler(
        WorkspaceInaccessibleException::class,
    )
    fun handle(e: Exception): ExceptionResponseBody {
        return ExceptionResponseBody(e.message ?: "알 수 없는 오류가 발생했습니다.")
    }
}