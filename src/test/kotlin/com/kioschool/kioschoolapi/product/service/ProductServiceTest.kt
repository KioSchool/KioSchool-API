package com.kioschool.kioschoolapi.product.service

import com.kioschool.kioschoolapi.aws.S3Service
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.entity.ProductCategory
import com.kioschool.kioschoolapi.product.exception.CanNotDeleteUsingProductCategoryException
import com.kioschool.kioschoolapi.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.product.repository.ProductRepository
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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

    val workspaceId = 0L
    val file = mockk<MultipartFile>()

    describe("getAllProductsByCondition") {
        it("should call customRepository.findAllByCondition") {
            every { customRepository.findAllByCondition(workspaceId, null) } returns listOf()
            sut.getAllProductsByCondition(workspaceId, null)
        }
    }

    describe("getProduct") {
        it("should call repository.findById") {
            every { repository.findById(SampleEntity.product.id) } returns Optional.of(SampleEntity.product)
            sut.getProduct(SampleEntity.product.id)
        }
    }

    describe("createProduct") {
        every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace

        context("without WorkspaceInaccessibleException") {
            every {
                workspaceService.isAccessible(
                    "username",
                    SampleEntity.workspace
                )
            } returns true

            it("should create product") {
                every { repository.save(any()) } returns SampleEntity.product

                val result = sut.createProduct(
                    "username",
                    workspaceId,
                    "name",
                    "description",
                    1000,
                    null,
                    null
                )

                result shouldBe SampleEntity.product
            }

            it("should product imageUrl is null if file is null") {
                every { repository.save(any()) } answers {
                    val product = arg<Product>(0)
                    product.imageUrl shouldBe null
                    product
                }

                sut.createProduct(
                    "username",
                    workspaceId,
                    "test",
                    "description",
                    1000,
                    null,
                    null
                )
            }

            it("should product imageUrl is not null if file is not null") {
                every { repository.save(any()) } answers {
                    val product = arg<Product>(0)
                    product
                } andThenAnswer {
                    val product = arg<Product>(0)
                    product.imageUrl shouldBe "testImageUrl"
                    product
                }
                every { s3Service.uploadFile(any(), any()) } returns "testImageUrl"

                sut.createProduct(
                    "username",
                    workspaceId,
                    "test",
                    "description",
                    1000,
                    null,
                    file
                )
            }

            it("should product productCategory is null if productCategoryId is null") {
                every { repository.save(any()) } answers {
                    val product = arg<Product>(0)
                    product.productCategory shouldBe null
                    product
                }

                sut.createProduct(
                    "username",
                    workspaceId,
                    "test",
                    "description",
                    1000,
                    null,
                    null
                )
            }

            it("should product productCategory is not null if productCategoryId is not null") {
                every { repository.save(any()) } answers {
                    val product = arg<Product>(0)
                    product.productCategory shouldBe SampleEntity.productCategory
                    product
                }
                every { categoryRepository.findById(1L) } returns Optional.of(SampleEntity.productCategory)

                sut.createProduct(
                    "username",
                    workspaceId,
                    "test",
                    "description",
                    1000,
                    1L,
                    null
                )
            }
        }

        context("with WorkspaceInaccessibleException") {
            it("should raise WorkspaceInaccessibleException if given workspaceId is not accessible") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns false

                try {
                    sut.createProduct(
                        "username",
                        workspaceId,
                        "name",
                        "description",
                        1000,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e shouldBe WorkspaceInaccessibleException()
                }
            }

            it("should raise WorkspaceInaccessibleException if given productCategoryId is from not accessible workspace") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns true
                every { categoryRepository.findById(1L) } returns Optional.of(
                    ProductCategory(
                        "name",
                        SampleEntity.workspaceWithId(2L)
                    )
                )

                try {
                    sut.createProduct(
                        "username",
                        workspaceId,
                        "name",
                        "description",
                        1000,
                        1L,
                        null
                    )
                } catch (e: Exception) {
                    e shouldBe WorkspaceInaccessibleException()
                }
            }
        }
    }


    describe("updateProduct") {
        every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
        every { repository.findById(SampleEntity.product.id) } returns Optional.of(SampleEntity.product)

        context("without WorkspaceInaccessibleException") {
            every {
                workspaceService.isAccessible(
                    "username",
                    SampleEntity.workspace
                )
            } returns true

            it("should update product") {
                every { repository.save(any()) } answers {
                    val product = arg<Product>(0)
                    product.name shouldBe "update name"
                    product.description shouldBe "update description"
                    product.price shouldBe 2000
                    product

                }

                val result = sut.updateProduct(
                    "username",
                    workspaceId,
                    SampleEntity.product.id,
                    "update name",
                    "update description",
                    2000,
                    null,
                    null
                )

                result shouldBe SampleEntity.product
            }
        }
    }

    describe("getAllProductCategories") {
        it("should call categoryRepository.findAllByWorkspaceId") {
            every { categoryRepository.findAllByWorkspaceIdOrderByIndexAsc(workspaceId) } returns listOf()
            sut.getAllProductCategories(workspaceId)
        }
    }

    describe("createProductCategory") {
        every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace

        context("without WorkspaceInaccessibleException") {
            every {
                workspaceService.isAccessible(
                    "username",
                    SampleEntity.workspace
                )
            } returns true

            it("should create product category") {
                every { categoryRepository.save(any()) } returns SampleEntity.productCategory

                val result = sut.createProductCategory(
                    "username",
                    workspaceId,
                    "name"
                )

                result shouldBe SampleEntity.productCategory
            }
        }

        context("with WorkspaceInaccessibleException") {
            it("should raise WorkspaceInaccessibleException if given workspaceId is not accessible") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns false

                try {
                    sut.createProductCategory(
                        "username",
                        workspaceId,
                        "name"
                    )
                } catch (e: Exception) {
                    e shouldBe WorkspaceInaccessibleException()
                }
            }
        }
    }

    describe("deleteProductCategory") {
        every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
        every { categoryRepository.findById(SampleEntity.productCategory.id) } returns Optional.of(
            SampleEntity.productCategory
        )

        context("without WorkspaceInaccessibleException") {
            every {
                workspaceService.isAccessible(
                    "username",
                    SampleEntity.workspace
                )
            } returns true

            it("should delete product category") {
                every {
                    repository.countByWorkspaceIdAndProductCategoryId(
                        workspaceId,
                        SampleEntity.productCategory.id
                    )
                } returns 0
                every { categoryRepository.findById(SampleEntity.productCategory.id) } returns Optional.of(
                    SampleEntity.productCategory
                )
                every { categoryRepository.delete(SampleEntity.productCategory) } returns Unit

                val result = sut.deleteProductCategory(
                    "username",
                    workspaceId,
                    SampleEntity.productCategory.id
                )

                result shouldBe SampleEntity.productCategory
            }
        }

        context("with WorkspaceInaccessibleException") {
            it("should raise WorkspaceInaccessibleException if given workspaceId is not accessible") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns false

                try {
                    sut.deleteProductCategory(
                        "username",
                        workspaceId,
                        SampleEntity.productCategory.id
                    )
                } catch (e: Exception) {
                    e shouldBe WorkspaceInaccessibleException()
                }
            }

            it("should raise CanNotDeleteUsingProductCategoryException if productCategory is used by product") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns true
                every {
                    repository.countByWorkspaceIdAndProductCategoryId(
                        workspaceId,
                        SampleEntity.productCategory.id
                    )
                } returns 1

                try {
                    sut.deleteProductCategory(
                        "username",
                        workspaceId,
                        SampleEntity.productCategory.id
                    )
                } catch (e: Exception) {
                    e shouldBe CanNotDeleteUsingProductCategoryException()
                }
            }
        }
    }

    describe("deleteProduct") {
        every { workspaceService.getWorkspace(workspaceId) } returns SampleEntity.workspace
        every { repository.findById(SampleEntity.product.id) } returns Optional.of(SampleEntity.product)

        context("without WorkspaceInaccessibleException") {
            every {
                workspaceService.isAccessible(
                    "username",
                    SampleEntity.workspace
                )
            } returns true

            it("should delete product") {
                every { repository.delete(SampleEntity.product) } returns Unit

                val result = sut.deleteProduct(
                    "username",
                    workspaceId,
                    SampleEntity.product.id
                )

                result shouldBe SampleEntity.product
            }
        }

        context("with WorkspaceInaccessibleException") {
            it("should raise WorkspaceInaccessibleException if given workspaceId is not accessible") {
                every {
                    workspaceService.isAccessible(
                        "username",
                        SampleEntity.workspace
                    )
                } returns false

                try {
                    sut.deleteProduct(
                        "username",
                        workspaceId,
                        SampleEntity.product.id
                    )
                } catch (e: Exception) {
                    e shouldBe WorkspaceInaccessibleException()
                }
            }
        }
    }
})