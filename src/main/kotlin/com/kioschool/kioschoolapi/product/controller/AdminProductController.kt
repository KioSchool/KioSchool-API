package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.dto.CreateOrUpdateProductRequestBody
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
@RequestMapping("/admin")
class AdminProductController(
    private val productService: ProductService
) {
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long
    ) = productService.getProducts(workspaceId)

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
                file
            ) else
            productService.updateProduct(
                username,
                body.workspaceId,
                body.productId,
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