package com.kioschool.kioschoolapi.product.controller

import com.kioschool.kioschoolapi.factory.AuthenticationSample
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.product.dto.CreateProductCategoryRequestBody
import com.kioschool.kioschoolapi.product.dto.CreateProductRequestBody
import com.kioschool.kioschoolapi.product.dto.UpdateProductRequestBody
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.core.context.SecurityContextHolder

class AdminProductControllerTest : DescribeSpec({
    val service = mockk<ProductService>()
    val sut = AdminProductController(service)
    val workspaceId = 1L

    extensions(AuthenticationSample)

    describe("getProducts") {
        it("should return all products") {
            every { service.getAllProductsByCondition(1L) } returns listOf()
            val result = sut.getProducts(workspaceId)
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

    describe("createOrUpdateProduct") {
        it("should create product") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.createProduct(
                    (authentication.principal as CustomUserDetails).username,
                    1L,
                    "name",
                    "description",
                    1000,
                    null,
                    null
                )
            } returns SampleEntity.product

            val result = sut.createOrUpdateProduct(
                authentication,
                CreateProductRequestBody(
                    "name",
                    "description",
                    1000,
                    1L,
                    null
                ),
                null
            )

            result shouldBe SampleEntity.product
        }
    }

    describe("updateProduct") {
        it("should update product") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.updateProduct(
                    (authentication.principal as CustomUserDetails).username,
                    SampleEntity.workspace.id,
                    SampleEntity.product.id,
                    "name",
                    "description",
                    1000,
                    null,
                    null
                )
            } returns SampleEntity.product

            val result = sut.updateProduct(
                authentication,
                UpdateProductRequestBody(
                    SampleEntity.product.id,
                    "name",
                    "description",
                    1000,
                    SampleEntity.workspace.id,
                    null
                ),
                null
            )

            result shouldBe SampleEntity.product
        }
    }

    describe("deleteProduct") {
        it("should delete product") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.deleteProduct(
                    (authentication.principal as CustomUserDetails).username,
                    SampleEntity.workspace.id,
                    SampleEntity.product.id
                )
            } returns SampleEntity.product

            val result = sut.deleteProduct(
                authentication,
                SampleEntity.workspace.id,
                SampleEntity.product.id
            )

            result shouldBe SampleEntity.product
        }
    }

    describe("createProductCategory") {
        it("should create product category") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.createProductCategory(
                    (authentication.principal as CustomUserDetails).username,
                    SampleEntity.workspace.id,
                    "name"
                )
            } returns SampleEntity.productCategory

            val result = sut.createProductCategory(
                authentication,
                CreateProductCategoryRequestBody(
                    "name",
                    SampleEntity.workspace.id
                )
            )

            result shouldBe SampleEntity.productCategory
        }
    }

    describe("deleteProductCategory") {
        it("should delete product category") {
            val authentication = SecurityContextHolder.getContext().authentication
            every {
                service.deleteProductCategory(
                    (authentication.principal as CustomUserDetails).username,
                    SampleEntity.workspace.id,
                    SampleEntity.productCategory.id
                )
            } returns SampleEntity.productCategory

            val result = sut.deleteProductCategory(
                authentication,
                SampleEntity.workspace.id,
                SampleEntity.productCategory.id
            )

            result shouldBe SampleEntity.productCategory
        }
    }
})