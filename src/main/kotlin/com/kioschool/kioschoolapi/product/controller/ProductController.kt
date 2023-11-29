package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.dto.CreateProductRequestBody
import com.kioschool.kioschoolapi.product.dto.GetProductsRequestBody
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.dto.ExceptionResponseBody
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
class ProductController(
    private val productService: ProductService
) {
    @GetMapping("/products")
    fun getProducts(
        @RequestBody body: GetProductsRequestBody
    ) = productService.getProducts(body.workspaceId)

    @PostMapping("/admin/product", consumes = [MediaType.ALL_VALUE])
    fun createProduct(
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
            file
        )
    }

    @ExceptionHandler(
        WorkspaceInaccessibleException::class,
    )
    fun handle(e: Exception): ExceptionResponseBody {
        return ExceptionResponseBody(e.message ?: "알 수 없는 오류가 발생했습니다.")
    }
}