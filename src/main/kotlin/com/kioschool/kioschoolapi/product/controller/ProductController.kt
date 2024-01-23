package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.user.dto.ExceptionResponseBody
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Product Controller")
@RestController
class ProductController(
    private val productService: ProductService
) {
    @Operation(
        summary = "상품 조회",
        description = "워크스페이스에 등록된 상품을 조회합니다. 카테고리를 입력하지 않으면 모든 상품을 조회합니다."
    )
    @GetMapping("/products")
    fun getProducts(
        @RequestParam workspaceId: Long,
        @RequestParam categoryId: Long? = null
    ) = productService.getAllProductsByCondition(workspaceId, categoryId)

    @ExceptionHandler(
        WorkspaceInaccessibleException::class,
    )
    fun handle(e: Exception): ExceptionResponseBody {
        return ExceptionResponseBody(e.message ?: "알 수 없는 오류가 발생했습니다.")
    }
}