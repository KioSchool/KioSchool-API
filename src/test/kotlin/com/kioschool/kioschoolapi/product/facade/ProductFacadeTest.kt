package com.kioschool.kioschoolapi.product.facade

import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.exception.CanNotDeleteUsingProductCategoryException
import com.kioschool.kioschoolapi.domain.product.facade.ProductFacade
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile

class ProductFacadeTest : DescribeSpec({
    val productService = mockk<ProductService>()
    val workspaceService = mockk<WorkspaceService>()
    val s3Service = mockk<S3Service>()

    val sut = ProductFacade(productService, workspaceService, s3Service)

    beforeTest {
        mockkObject(productService)
        mockkObject(workspaceService)
        mockkObject(s3Service)
    }

    afterTest {
        clearAllMocks()
    }

    describe("getProduct") {
        it("should return product") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, product.workspace.id) } just Runs

            sut.getProduct(username, productId)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    product.workspace.id
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getProduct(username, productId)
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
        }
    }

    describe("getProducts by username and workspaceId") {
        it("should return products") {
            val username = "username"
            val workspaceId = 1L
            val products = listOf(SampleEntity.product)
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.getAllProductsByCondition(workspaceId) } returns products

            sut.getProducts(username, workspaceId)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.getAllProductsByCondition(workspaceId) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getProducts(username, workspaceId)
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getAllProductsByCondition(workspaceId) }
        }
    }

    describe("getProducts by workspaceId and categoryId") {
        it("should return products") {
            val workspaceId = 1L
            val categoryId = 1L
            val products = listOf(SampleEntity.product)
            every {
                productService.getAllProductsByCondition(
                    workspaceId,
                    categoryId
                )
            } returns products

            sut.getProducts(workspaceId, categoryId)

            verify { productService.getAllProductsByCondition(workspaceId, categoryId) }
        }
    }

    describe("getProductCategories by workspaceId") {
        it("should return product categories") {
            val workspaceId = 1L
            val productCategories = listOf(SampleEntity.productCategory)
            every { productService.getAllProductCategories(workspaceId) } returns productCategories

            sut.getProductCategories(workspaceId)

            verify { productService.getAllProductCategories(workspaceId) }
        }
    }

    describe("getProductCategories by username and workspaceId") {
        it("should return product categories") {
            val username = "username"
            val workspaceId = 1L
            val productCategories = listOf(SampleEntity.productCategory)
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.getAllProductCategories(workspaceId) } returns productCategories

            sut.getProductCategories(username, workspaceId)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.getAllProductCategories(workspaceId) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.getProductCategories(username, workspaceId)
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getAllProductCategories(workspaceId) }
        }
    }

    describe("createProduct") {
        it("should create product") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            val description = "description"
            val price = 100
            val productCategoryId = 1L
            val imageUrl = "imageUrl"
            val file = mockk<MultipartFile>()
            val workspace = SampleEntity.workspace
            val product = SampleEntity.product
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                productService.saveProduct(
                    name,
                    price,
                    description,
                    workspace,
                    productCategoryId
                )
            } returns product
            every { productService.getImageUrl(workspaceId, product.id, file) } returns imageUrl
            every { productService.saveProduct(product) } returns product

            val result = sut.createProduct(
                username,
                workspaceId,
                name,
                description,
                price,
                productCategoryId,
                file
            )

            assert(result.id == product.id)
            assert(result.imageUrl == imageUrl)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify {
                productService.saveProduct(
                    name,
                    price,
                    description,
                    workspace,
                    productCategoryId
                )
            }
            verify { productService.getImageUrl(workspaceId, product.id, file) }
            verify { productService.saveProduct(product) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            val description = "description"
            val price = 100
            val productCategoryId = 1L
            val file = mockk<MultipartFile>()
            val workspace = SampleEntity.workspace
            val product = SampleEntity.product
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.createProduct(
                    username,
                    workspaceId,
                    name,
                    description,
                    price,
                    productCategoryId,
                    file
                )
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { workspaceService.getWorkspace(workspaceId) }
            verify(exactly = 0) {
                productService.saveProduct(
                    name,
                    price,
                    description,
                    workspace,
                    productCategoryId
                )
            }
            verify(exactly = 0) { productService.getImageUrl(workspaceId, product.id, file) }
            verify(exactly = 0) { productService.saveProduct(product) }
        }
    }

    describe("updateProduct") {
        it("should update product") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val name = "name"
            val description = "description"
            val price = 100
            val productCategoryId = 1L
            val imageUrl = "imageUrl"
            val file = mockk<MultipartFile>()
            val workspace = SampleEntity.workspaceWithId(workspaceId)
            val product = SampleEntity.productWithId(productId)
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, workspace.id) } just Runs
            every {
                productService.getImageUrl(
                    workspaceId,
                    productId,
                    any<MultipartFile>()
                )
            } returns imageUrl
            every { productService.getProductCategory(productCategoryId) } returns SampleEntity.productCategory
            every { s3Service.deleteFile(product.imageUrl!!) } just Runs
            every { productService.saveProduct(product) } returns product

            val result = sut.updateProduct(
                username,
                workspaceId,
                productId,
                name,
                description,
                price,
                productCategoryId,
                file
            )

            assert(result.id == product.id)
            assert(result.name == name)
            assert(result.description == description)
            assert(result.price == price)
            assert(result.imageUrl == imageUrl)
            assert(result.productCategory?.id == SampleEntity.productCategory.id)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspace.id) }
            verify { productService.getImageUrl(workspaceId, productId, file) }
            verify { s3Service.deleteFile(product.imageUrl!!) }
            verify { productService.getProductCategory(productCategoryId) }
            verify { productService.saveProduct(product) }
        }

        it("should not update product if parameters are null") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val workspace = SampleEntity.workspaceWithId(workspaceId)
            val product = SampleEntity.productWithId(productId)
            val name = product.name
            val description = product.description
            val price = product.price
            val imageUrl = product.imageUrl
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, workspace.id) } just Runs
            every { productService.getImageUrl(workspaceId, productId, null) } returns null
            every { productService.saveProduct(product) } returns product

            val result = sut.updateProduct(
                username,
                workspaceId,
                productId,
                null,
                null,
                null,
                null,
                null
            )

            assert(result.id == product.id)
            assert(result.name == name)
            assert(result.description == description)
            assert(result.price == price)
            assert(result.imageUrl == imageUrl)
            assert(result.productCategory == null)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspace.id) }
            verify { productService.getImageUrl(workspaceId, productId, null) }
            verify(exactly = 0) { s3Service.deleteFile(any()) }
            verify(exactly = 0) { productService.getProductCategory(any()) }
            verify { productService.saveProduct(product) }
        }

        it("should throw WorkspaceInaccessibleException if workspace is inaccessible") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val name = "name"
            val description = "description"
            val price = 100
            val productCategoryId = 1L
            val file = mockk<MultipartFile>()
            val workspace = SampleEntity.workspace
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    workspace.id
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateProduct(
                    username,
                    workspaceId,
                    productId,
                    name,
                    description,
                    price,
                    productCategoryId,
                    file
                )
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspace.id) }
            verify(exactly = 0) { productService.getImageUrl(any(), any(), null) }
            verify(exactly = 0) { s3Service.deleteFile(any()) }
            verify(exactly = 0) { productService.getProductCategory(any()) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }

        it("should throw WorkspaceInaccessibleException if product category is in another workspace") {
            val username = "username"
            val workspaceId = 2L
            val productId = 1L
            val name = "name"
            val description = "description"
            val price = 100
            val productCategoryId = 1L
            val file = mockk<MultipartFile>()
            val workspace = SampleEntity.workspace
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, workspace.id) } just Runs
            every { productService.getImageUrl(workspace.id, product.id, file) } returns null
            every { productService.getProductCategory(productCategoryId) } returns SampleEntity.productCategory

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateProduct(
                    username,
                    workspaceId,
                    productId,
                    name,
                    description,
                    price,
                    productCategoryId,
                    file
                )
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspace.id) }
            verify { productService.getImageUrl(workspace.id, product.id, file) }
            verify(exactly = 0) { s3Service.deleteFile(any()) }
            verify { productService.getProductCategory(productCategoryId) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }
    }

    describe("updateProductSellable") {
        it("should update product sellable") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val isSellable = true
            val product = SampleEntity.product.apply { this.isSellable = false }

            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, product.workspace.id) } just Runs
            every { productService.saveProduct(product) } returns product

            val result = sut.updateProductSellable(username, workspaceId, productId, isSellable)

            assert(result.id == product.id)
            assert(result.isSellable == isSellable)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
            verify { productService.saveProduct(product) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val isSellable = true
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    product.workspace.id
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateProductSellable(username, workspaceId, productId, isSellable)
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
        }
    }

    describe("deleteProduct") {
        it("should delete product") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, product.workspace.id) } just Runs
            every { productService.deleteProduct(product) } returns product

            sut.deleteProduct(username, productId)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
            verify { productService.deleteProduct(product) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    product.workspace.id
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.deleteProduct(username, productId)
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
            verify(exactly = 0) { productService.deleteProduct(any()) }
        }
    }

    describe("createProductCategory") {
        it("should create product category") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            val workspace = SampleEntity.workspace
            val productCategory = SampleEntity.productCategory
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { productService.saveProductCategory(any<ProductCategory>()) } returns productCategory

            val result = sut.createProductCategory(username, workspaceId, name)

            assert(result.id == productCategory.id)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { productService.saveProductCategory(any<ProductCategory>()) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.createProductCategory(username, workspaceId, name)
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { productService.saveProductCategory(any()) }
        }
    }

    describe("deleteProductCategory") {
        it("should delete product category") {
            val username = "username"
            val workspaceId = 1L
            val productCategoryId = 1L
            val productCategory = SampleEntity.productCategory
            every { productService.getProductCategory(productCategoryId) } returns productCategory
            every {
                workspaceService.checkAccessible(
                    username,
                    productCategory.workspace.id
                )
            } just Runs
            every {
                productService.checkProductCategoryDeletable(
                    workspaceId,
                    productCategoryId
                )
            } just Runs
            every { productService.deleteProductCategory(productCategory) } returns productCategory

            sut.deleteProductCategory(username, workspaceId, productCategoryId)

            verify { productService.getProductCategory(productCategoryId) }
            verify { workspaceService.checkAccessible(username, productCategory.workspace.id) }
            verify { productService.checkProductCategoryDeletable(workspaceId, productCategoryId) }
            verify { productService.deleteProductCategory(productCategory) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val productCategoryId = 1L
            val productCategory = SampleEntity.productCategory
            every { productService.getProductCategory(productCategoryId) } returns productCategory
            every {
                workspaceService.checkAccessible(
                    username,
                    productCategory.workspace.id
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.deleteProductCategory(username, workspaceId, productCategoryId)
            }

            verify { productService.getProductCategory(productCategoryId) }
            verify { workspaceService.checkAccessible(username, productCategory.workspace.id) }
            verify(exactly = 0) { productService.checkProductCategoryDeletable(any(), any()) }
            verify(exactly = 0) { productService.deleteProductCategory(any()) }
        }

        it("should throw CanNotDeleteUsingProductCategoryException if product category is not deletable") {
            val username = "username"
            val workspaceId = 2L
            val productCategoryId = 1L
            val productCategory = SampleEntity.productCategory
            every { productService.getProductCategory(productCategoryId) } returns productCategory
            every {
                workspaceService.checkAccessible(
                    username,
                    productCategory.workspace.id
                )
            } just Runs
            every {
                productService.checkProductCategoryDeletable(
                    workspaceId,
                    productCategoryId
                )
            } throws CanNotDeleteUsingProductCategoryException()

            assertThrows<CanNotDeleteUsingProductCategoryException> {
                sut.deleteProductCategory(username, workspaceId, productCategoryId)
            }

            verify { productService.getProductCategory(productCategoryId) }
            verify { workspaceService.checkAccessible(username, productCategory.workspace.id) }
            verify { productService.checkProductCategoryDeletable(workspaceId, productCategoryId) }
            verify(exactly = 0) { productService.deleteProductCategory(any()) }
        }
    }

    describe("sortProductCategories") {
        it("should sort product categories") {
            val username = "username"
            val workspaceId = 1L
            val productCategoryIds = listOf(1L, 2L, 3L)
            val productCategories = SampleEntity.productCategories

            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.getProductCategories(productCategoryIds) } returns productCategories
            every { productService.saveProductCategories(productCategories) } returns productCategories

            val result = sut.sortProductCategories(username, workspaceId, productCategoryIds)

            assert(result.first().id == productCategories.first().id)
            assert(productCategories[0].index == 0)
            assert(productCategories[1].index == 1)
            assert(productCategories[2].index == 2)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.getProductCategories(productCategoryIds) }
            verify { productService.saveProductCategories(productCategories) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val productCategoryIds = listOf(1L, 2L, 3L)
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.sortProductCategories(username, workspaceId, productCategoryIds)
            }

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getProductCategories(any()) }
            verify(exactly = 0) { productService.saveProductCategories(any()) }
        }
    }

    describe("updateProductStatus") {
        it("should update product status") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val status = ProductStatus.SOLD_OUT
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.saveProduct(product) } returns product

            val result = sut.updateProductStatus(username, workspaceId, productId, status)

            assert(result.id == product.id)
            assert(result.status == status)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.saveProduct(product) }
        }

        it("should throw WorkspaceInaccessibleException") {
            val username = "username"
            val workspaceId = 1L
            val productId = 1L
            val status = ProductStatus.SOLD_OUT
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateProductStatus(username, workspaceId, productId, status)
            }

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }
    }
})