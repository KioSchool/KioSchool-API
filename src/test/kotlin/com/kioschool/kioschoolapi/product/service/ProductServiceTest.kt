package com.kioschool.kioschoolapi.product.service

import com.kioschool.kioschoolapi.aws.S3Service
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.exception.CanNotDeleteUsingProductCategoryException
import com.kioschool.kioschoolapi.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.product.repository.ProductRepository
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.multipart.MultipartFile
import java.util.*

class ProductServiceTest : DescribeSpec({
    val repository = mockk<ProductRepository>()
    val customRepository = mockk<CustomProductRepository>()
    val categoryRepository = mockk<ProductCategoryRepository>()
    val workspaceService = mockk<WorkspaceService>()
    val s3Service = mockk<S3Service>()
    val sut = ProductService(
        "test",
        repository,
        customRepository,
        categoryRepository,
        workspaceService,
        s3Service
    )

    describe("getAllProductsByCondition") {
        it("should call customProductRepository.findAllByCondition") {
            //Mock
            every { customRepository.findAllByCondition(1, null) } returns emptyList()

            // Act
            sut.getAllProductsByCondition(1)

            verify { customRepository.findAllByCondition(1, null) }
        }
    }

    describe("getProduct") {
        it("should call productRepository.findById") {
            //Mock
            every { repository.findById(1) } returns Optional.of(SampleEntity.product)

            // Act
            sut.getProduct(1)

            verify { repository.findById(1) }
        }
    }

    describe("getProductCategory") {
        it("should call productCategoryRepository.findById") {
            //Mock
            every { categoryRepository.findById(1) } returns Optional.of(SampleEntity.productCategory)

            // Act
            sut.getProductCategory(1)

            verify { categoryRepository.findById(1) }
        }
    }

    describe("getProductCategories") {
        it("should call productCategoryRepository.findAllById") {
            //Mock
            every {
                categoryRepository.findAllById(
                    listOf(
                        1,
                        2
                    )
                )
            } returns listOf(SampleEntity.productCategory)

            // Act
            sut.getProductCategories(listOf(1, 2))

            verify { categoryRepository.findAllById(listOf(1, 2)) }
        }
    }

    describe("saveProduct") {
        it("should call productRepository.save") {
            //Mock
            every { repository.save(SampleEntity.product) } returns SampleEntity.product

            // Act
            sut.saveProduct(SampleEntity.product)

            verify { repository.save(SampleEntity.product) }
        }

        it("should call productRepository.save with product") {
            //Mock
            every { repository.save(any<Product>()) } returns SampleEntity.product

            // Act
            sut.saveProduct(
                SampleEntity.product.name,
                SampleEntity.product.price,
                SampleEntity.product.description,
                SampleEntity.workspace,
                null
            )

            verify { repository.save(any<Product>()) }
        }

        it("should call productRepository.save with product and productCategory") {
            //Mock
            every { repository.save(any<Product>()) } returns SampleEntity.product
            every { categoryRepository.findById(1) } returns Optional.of(SampleEntity.productCategory)

            // Act
            sut.saveProduct(
                SampleEntity.product.name,
                SampleEntity.product.price,
                SampleEntity.product.description,
                SampleEntity.workspace,
                1
            )

            verify { repository.save(any<Product>()) }
        }
    }

    describe("saveProductCategories") {
        it("should call productCategoryRepository.saveAll") {
            //Mock
            every { categoryRepository.saveAll(listOf(SampleEntity.productCategory)) } returns listOf(
                SampleEntity.productCategory
            )

            // Act
            sut.saveProductCategories(listOf(SampleEntity.productCategory))

            verify { categoryRepository.saveAll(listOf(SampleEntity.productCategory)) }
        }
    }

    describe("getAllProductCategories") {
        it("should call productCategoryRepository.findAllByWorkspaceIdOrderByIndexAsc") {
            val workspaceId = 1L

            //Mock
            every { categoryRepository.findAllByWorkspaceIdOrderByIndexAsc(1) } returns listOf(
                SampleEntity.productCategory
            )

            // Act
            sut.getAllProductCategories(workspaceId)

            verify { categoryRepository.findAllByWorkspaceIdOrderByIndexAsc(workspaceId) }
        }
    }

    describe("getImageUrl") {
        it("should call s3Service.uploadFile if file is not null") {
            val workspaceId = 1L
            val productId = 1L
            val file = mockk<MultipartFile>()


            //Mock
            every {
                s3Service.uploadFile(
                    file,
                    any<String>()
                )
            } returns "test"

            // Act
            sut.getImageUrl(workspaceId, productId, file) shouldBe "test"

            // Assert
            verify { s3Service.uploadFile(file, any<String>()) }
        }

        it("should return null if file is null") {
            val workspaceId = 1L
            val productId = 1L
            val file = null

            // Act
            sut.getImageUrl(workspaceId, productId, file) shouldBe null
        }
    }

    describe("saveProductCategory") {
        it("should call productCategoryRepository.save") {
            //Mock
            every { categoryRepository.save(SampleEntity.productCategory) } returns SampleEntity.productCategory

            // Act
            sut.saveProductCategory(SampleEntity.productCategory)

            verify { categoryRepository.save(SampleEntity.productCategory) }
        }
    }

    describe("deleteProductCategory") {
        it("should call productCategoryRepository.delete") {
            //Mock
            every { categoryRepository.delete(SampleEntity.productCategory) } returns Unit

            // Act
            sut.deleteProductCategory(SampleEntity.productCategory)

            verify { categoryRepository.delete(SampleEntity.productCategory) }
        }
    }

    describe("checkProductCategoryDeletable") {
        it("should throw CanNotDeleteUsingProductCategoryException if productCategory is used by product") {
            val workspaceId = 1L
            val productCategoryId = 1L

            //Mock
            every {
                repository.countByWorkspaceIdAndProductCategoryId(
                    workspaceId,
                    productCategoryId
                )
            } returns 1L

            // Act and Assert
            shouldThrow<CanNotDeleteUsingProductCategoryException> {
                sut.checkProductCategoryDeletable(workspaceId, productCategoryId)
            }
        }

        it("should not throw CanNotDeleteUsingProductCategoryException if productCategory is not used by product") {
            val workspaceId = 1L
            val productCategoryId = 1L

            //Mock
            every {
                repository.countByWorkspaceIdAndProductCategoryId(
                    workspaceId,
                    productCategoryId
                )
            } returns 0L

            // Act and Assert
            sut.checkProductCategoryDeletable(workspaceId, productCategoryId)
        }
    }

    describe("deleteProduct") {
        it("should call productRepository.delete") {
            val product = SampleEntity.product

            //Mock
            every { repository.delete(product) } returns Unit

            // Act
            sut.deleteProduct(product) shouldBe product

            verify { repository.delete(product) }
        }
    }
})