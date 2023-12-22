package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.user.dto.ExceptionResponseBody
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(
    private val productService: ProductService
) {
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long
    ) = productService.getProducts(workspaceId)

    @ExceptionHandler(
        WorkspaceInaccessibleException::class,
    )
    fun handle(e: Exception): ExceptionResponseBody {
        return ExceptionResponseBody(e.message ?: "알 수 없는 오류가 발생했습니다.")
    }
}