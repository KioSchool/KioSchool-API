package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.product.service.ProductService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ProductControllerTest : DescribeSpec({
    val service = mockk<ProductService>()
    val sut = ProductController(service)
    val workspaceId = 1L

    describe("getProducts") {
        it("should return all products") {
            every { service.getAllProductsByCondition(1L, null) } returns listOf()

            val result = sut.getProducts(workspaceId, null)
            result shouldBe emptyList()
        }
    }

    describe("getProductCategories") {
        it("should return all product categories") {
            every { service.getAllProductCategories(1L) } returns listOf()

            val result = sut.getProductCategories(workspaceId)
            result shouldBe emptyList()
        }
    }
})