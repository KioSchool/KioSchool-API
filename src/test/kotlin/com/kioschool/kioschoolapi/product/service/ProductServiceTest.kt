package com.kioschool.kioschoolapi.product.service

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.SetComponentRepository
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.factory.SampleEntity.newProduct
import com.kioschool.kioschoolapi.factory.SampleEntity.newSetProduct
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

class ProductServiceTest : DescribeSpec({
    val repository = mockk<ProductRepository>()
    val customRepository = mockk<CustomProductRepository>()
    val categoryRepository = mockk<ProductCategoryRepository>()
    val s3Service = mockk<S3Service>()
    val setComponentRepository = mockk<SetComponentRepository>()
    val sut = ProductService(
        "test",
        repository,
        customRepository,
        categoryRepository,
        s3Service,
        setComponentRepository
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(customRepository)
        mockkObject(categoryRepository)
        mockkObject(s3Service)
    }

    afterTest {
        clearAllMocks()
    }

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
            every { file.inputStream } returns java.io.ByteArrayInputStream(ByteArray(0))

            //Mock
            every {
                s3Service.uploadResizedWebpImage(
                    any(),
                    any<String>()
                )
            } returns "test"

            // Act
            sut.getImageUrl(workspaceId, productId, file) shouldBe "test"

            // Assert
            verify { s3Service.uploadResizedWebpImage(any(), any<String>()) }
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
        it("should throw CustomException if productCategory is used by product") {
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
            val ex = shouldThrow<CustomException> {
                sut.checkProductCategoryDeletable(workspaceId, productCategoryId)
            }
            ex.errorCode shouldBe ErrorCode.CANNOT_DELETE_USING_PRODUCT_CATEGORY
        }

        it("should not throw CustomException if productCategory is not used by product") {
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

    describe("getCachedProducts") {
        it("관리자 상품 응답을 만들고, 연결 세트는 일반 상품 id로만 한 번 조회해 싣는다") {
            val single = newProduct(10)
            val set = newSetProduct(20, listOf(single))
            every { customRepository.findAllByCondition(1L, null) } returns listOf(single, set)
            every { setComponentRepository.findAllByConditionProductIdIn(listOf(10L)) } returns set.components

            val result = sut.getCachedProducts(1L)

            result.first { it.id == 10L }.linkedSets.map { it.id } shouldBe listOf(20L)
            result.first { it.id == 20L }.linkedSets shouldBe emptyList()
            result.first { it.id == 20L }.effectiveStatus shouldBe ProductStatus.SELLING
            verify(exactly = 1) { setComponentRepository.findAllByConditionProductIdIn(any()) }
        }
    }

    describe("getLinkedSets") {
        it("연결 상품별로 그 상품을 쓰는 세트를 중복 없이 id 순으로 준다") {
            val single = newProduct(10)
            val set20 = newSetProduct(20, listOf(single, single))
            val set21 = newSetProduct(21, listOf(single))
            every { setComponentRepository.findAllByConditionProductIdIn(listOf(10L)) } returns
                set21.components + set20.components

            val result = sut.getLinkedSets(listOf(10L))

            result[10L]!!.map { it.id } shouldBe listOf(20L, 21L)
        }

        it("빈 입력이면 저장소를 부르지 않는다") {
            sut.getLinkedSets(emptyList()) shouldBe emptyMap()

            verify(exactly = 0) { setComponentRepository.findAllByConditionProductIdIn(any()) }
        }
    }

    describe("validateProducts") {
        it("should run") {
            val workspaceId = 1L
            val productIds = listOf(1L, 2L)

            //Mock
            every {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            } returns listOf(
                SampleEntity.productWithId(1L).apply { status = ProductStatus.SELLING },
                SampleEntity.productWithId(2L).apply { status = ProductStatus.SELLING }
            )

            // Act and Assert
            sut.validateProducts(workspaceId, productIds)

            verify {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            }
        }

        it("should throw CustomException if product is not found") {
            val workspaceId = 1L
            val productIds = listOf(1L, 2L)

            //Mock
            every {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            } returns emptyList()

            // Act and Assert
            val ex = shouldThrow<CustomException> {
                sut.validateProducts(workspaceId, productIds)
            }
            ex.errorCode shouldBe ErrorCode.NOT_FOUND_PRODUCT

            verify {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            }
        }

        it("should throw CustomException if product status is not SELLING") {
            val workspaceId = 1L
            val productIds = listOf(1L, 2L)

            //Mock
            every {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            } returns listOf(
                SampleEntity.productWithId(1L).apply { status = ProductStatus.SOLD_OUT },
                SampleEntity.productWithId(2L)
            )

            // Act and Assert
            val ex = shouldThrow<CustomException> {
                sut.validateProducts(workspaceId, productIds)
            }
            ex.errorCode shouldBe ErrorCode.NOT_SELLABLE_PRODUCT

            verify {
                repository.findAllByIdInAndWorkspaceId(
                    productIds,
                    workspaceId
                )
            }
        }
    }
})