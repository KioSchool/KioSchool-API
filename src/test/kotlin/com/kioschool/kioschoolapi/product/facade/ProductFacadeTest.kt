package com.kioschool.kioschoolapi.product.facade

import com.kioschool.kioschoolapi.domain.product.dto.common.AdminProductDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.request.CreateSetProductRequestBody
import com.kioschool.kioschoolapi.domain.product.dto.request.SetComponentRequestBody
import com.kioschool.kioschoolapi.domain.product.dto.request.UpdateSetProductRequestBody
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.event.ProductChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductDeletedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductSnapshot
import com.kioschool.kioschoolapi.domain.product.facade.ProductFacade
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.multipart.MultipartFile

class ProductFacadeTest : DescribeSpec({
    val productService = mockk<ProductService>()
    val workspaceService = mockk<WorkspaceService>()
    val s3Service = mockk<S3Service>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val sut = ProductFacade(productService, workspaceService, s3Service, eventPublisher)

    beforeTest {
        mockkObject(productService)
        mockkObject(workspaceService)
        mockkObject(s3Service)
        every { productService.getLinkedSets(any()) } returns emptyMap()
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    product.workspace.id
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.getProduct(username, productId)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
        }
    }

    describe("getProducts by username and workspaceId") {
        it("should return products") {
            val username = "username"
            val workspaceId = 1L
            val products = listOf(AdminProductDto.of(SampleEntity.product, emptyList()))
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.getCachedProducts(workspaceId) } returns products

            val result = sut.getProducts(username, workspaceId)

            assertEquals(products, result)
            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.getCachedProducts(workspaceId) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val workspaceId = 1L
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.getProducts(username, workspaceId)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getCachedProducts(workspaceId) }
        }
    }

    describe("createSetProduct") {
        fun body(name: String = "족발 막국수 세트", components: List<SetComponentRequestBody>) = CreateSetProductRequestBody(
            workspaceId = 1L,
            name = name,
            description = "족발 소와 막국수를 함께",
            productCategoryId = null,
            originalPrice = 28000,
            discountPrice = 3000,
            components = components
        )

        fun stubCreate(conditionProducts: List<Product>): CapturingSlot<Product> {
            val saved = slot<Product>()
            every { workspaceService.checkAccessible("username", 1L) } just Runs
            every { workspaceService.getWorkspace(1L) } returns SampleEntity.newWorkspace(1)
            every { productService.getProducts(any()) } returns conditionProducts
            every { productService.saveProduct(capture(saved)) } answers { firstArg() }
            every { productService.getImageUrl(1L, any(), null) } returns null
            return saved
        }

        it("세트를 SELLING으로 만들고 판매가를 서버가 계산한다") {
            val saved = stubCreate(listOf(SampleEntity.newProduct(10), SampleEntity.newProduct(11)))

            val result = sut.createSetProduct(
                "username",
                body(components = listOf(SetComponentRequestBody("족발 소", 1, 10), SetComponentRequestBody("막국수", 1, 11))),
                null
            )

            saved.captured.type shouldBe ProductType.SET
            saved.captured.price shouldBe 25000
            saved.captured.originalPrice shouldBe 28000
            saved.captured.discountPrice shouldBe 3000
            saved.captured.status shouldBe ProductStatus.SELLING
            saved.captured.components.map { it.name } shouldBe listOf("족발 소", "막국수")
            saved.captured.components.map { it.displayOrder } shouldBe listOf(0, 1)
            saved.captured.components.map { it.conditionProduct?.id } shouldBe listOf(10L, 11L)
            saved.captured.components.all { it.setProduct === saved.captured } shouldBe true
            result.effectiveStatus shouldBe ProductStatus.SELLING
        }

        it("세트를 연결 상품으로 지정하면 INVALID_INPUT") {
            stubCreate(listOf(SampleEntity.newProduct(10), SampleEntity.newSetProduct(21, listOf(null))))

            val ex = assertThrows<CustomException> {
                sut.createSetProduct(
                    "username",
                    body(components = listOf(SetComponentRequestBody("족발 소", 1, 10), SetComponentRequestBody("세트", 1, 21))),
                    null
                )
            }

            ex.errorCode shouldBe ErrorCode.INVALID_INPUT
            ex.errors.single().field shouldBe "components[1].conditionProductId"
            ex.errors.single().index shouldBe 1
            ex.errors.single().reason shouldBe "같은 주점의 일반 상품만 연결할 수 있습니다."
            verify(exactly = 0) { productService.saveProduct(any<Product>()) }
        }

        it("다른 주점 상품이나 없는 상품을 연결하면 INVALID_INPUT") {
            stubCreate(listOf(SampleEntity.newProduct(10, workspace = SampleEntity.newWorkspace(2))))

            val ex = assertThrows<CustomException> {
                sut.createSetProduct(
                    "username",
                    body(components = listOf(SetComponentRequestBody("족발 소", 1, 10), SetComponentRequestBody("막국수", 1, 99))),
                    null
                )
            }

            ex.errorCode shouldBe ErrorCode.INVALID_INPUT
            ex.errors.map { it.field } shouldBe listOf("components[0].conditionProductId", "components[1].conditionProductId")
        }

        it("검증기 오류와 연결 상품 오류를 한 번에 돌려준다") {
            stubCreate(emptyList())

            val ex = assertThrows<CustomException> {
                sut.createSetProduct("username", body(name = " ", components = listOf(SetComponentRequestBody("족발 소", 1, 99))), null)
            }

            ex.errors.map { it.field } shouldBe listOf("name", "components[0].conditionProductId")
        }
    }

    describe("updateSetProduct") {
        fun body(workspaceId: Long = 1L, components: List<SetComponentRequestBody>) = UpdateSetProductRequestBody(
            productId = 20L,
            workspaceId = workspaceId,
            name = "새 세트",
            description = "설명",
            productCategoryId = null,
            originalPrice = 30000,
            discountPrice = 5000,
            components = components
        )

        it("일반 상품이면 PRODUCT_TYPE_MISMATCH") {
            every { productService.getProduct(20L) } returns SampleEntity.newProduct(20)
            every { workspaceService.checkAccessible("username", 1L) } just Runs

            val ex = assertThrows<CustomException> {
                sut.updateSetProduct("username", body(components = listOf(SetComponentRequestBody("a", 1, null))), null)
            }

            ex.errorCode shouldBe ErrorCode.PRODUCT_TYPE_MISMATCH
            verify(exactly = 0) { productService.saveProduct(any<Product>()) }
        }

        it("세트의 실제 주점과 요청 workspaceId가 다르면 WORKSPACE_INACCESSIBLE") {
            every { productService.getProduct(20L) } returns SampleEntity.newSetProduct(20, listOf(null))
            every { workspaceService.checkAccessible("username", 1L) } just Runs

            val ex = assertThrows<CustomException> {
                sut.updateSetProduct("username", body(workspaceId = 2L, components = listOf(SetComponentRequestBody("a", 1, null))), null)
            }

            ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE
            verify { workspaceService.checkAccessible("username", 1L) }
        }

        it("세트 자신을 연결 상품으로 지정하면 INVALID_INPUT") {
            val set = SampleEntity.newSetProduct(20, listOf(null))
            every { productService.getProduct(20L) } returns set
            every { workspaceService.checkAccessible("username", 1L) } just Runs
            every { productService.getProducts(any()) } returns listOf(set)

            val ex = assertThrows<CustomException> {
                sut.updateSetProduct("username", body(components = listOf(SetComponentRequestBody("a", 1, 20))), null)
            }

            ex.errorCode shouldBe ErrorCode.INVALID_INPUT
            ex.errors.single().field shouldBe "components[0].conditionProductId"
        }

        it("구성품을 보낸 목록으로 통째로 바꾸고 판매가를 다시 계산한다") {
            val set = SampleEntity.newSetProduct(20, listOf(SampleEntity.newProduct(10), SampleEntity.newProduct(11)))
            every { productService.getProduct(20L) } returns set
            every { workspaceService.checkAccessible("username", 1L) } just Runs
            every { productService.getProducts(any()) } returns listOf(SampleEntity.newProduct(12))
            every { productService.getImageUrl(1L, 20L, null) } returns null
            every { productService.saveProduct(set) } returns set

            val result = sut.updateSetProduct("username", body(components = listOf(SetComponentRequestBody("보쌈", 2, 12))), null)

            set.components.map { it.name } shouldBe listOf("보쌈")
            set.components.single().conditionProduct?.id shouldBe 12L
            set.price shouldBe 25000
            set.originalPrice shouldBe 30000
            set.discountPrice shouldBe 5000
            set.name shouldBe "새 세트"
            result.components.single().quantity shouldBe 2
            verify { eventPublisher.publishEvent(any<ProductChangedEvent>()) }
        }
    }

    describe("updateProduct on set") {
        it("세트는 일반 상품 수정 API로 고칠 수 없다") {
            every { productService.getProduct(20L) } returns SampleEntity.newSetProduct(20, listOf(null))
            every { workspaceService.checkAccessible("username", 1L) } just Runs

            val ex = assertThrows<CustomException> {
                sut.updateProduct("username", 1L, 20L, "이름", null, 1000, null, null)
            }

            ex.errorCode shouldBe ErrorCode.PRODUCT_TYPE_MISMATCH
            verify(exactly = 0) { productService.saveProduct(any<Product>()) }
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
            val productCategories = listOf(ProductCategoryDto.of(SampleEntity.productCategory))
            every { productService.getCachedProductCategories(workspaceId) } returns productCategories

            val result = sut.getProductCategories(workspaceId)

            assertEquals(productCategories, result)
            verify { productService.getCachedProductCategories(workspaceId) }
        }
    }

    describe("getProductCategories by username and workspaceId") {
        it("should return product categories") {
            val username = "username"
            val workspaceId = 1L
            val productCategories = listOf(ProductCategoryDto.of(SampleEntity.productCategory))
            every { workspaceService.checkAccessible(username, workspaceId) } just Runs
            every { productService.getCachedProductCategories(workspaceId) } returns productCategories

            val result = sut.getProductCategories(username, workspaceId)

            assertEquals(productCategories, result)
            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify { productService.getCachedProductCategories(workspaceId) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val workspaceId = 1L
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.getProductCategories(username, workspaceId)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getCachedProductCategories(workspaceId) }
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
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
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
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
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) if workspace is inaccessible") {
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
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
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
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspace.id) }
            verify(exactly = 0) { productService.getImageUrl(any(), any(), null) }
            verify(exactly = 0) { s3Service.deleteFile(any()) }
            verify(exactly = 0) { productService.getProductCategory(any()) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) if product category is in another workspace") {
            val username = "username"
            val workspace = SampleEntity.workspace(SampleEntity.user).withId(1L)
            val product = Product(
                name = "감자전",
                description = "description",
                price = 5000,
                workspace = workspace
            ).withId(40L)
            val otherCategory = ProductCategory(
                name = "other",
                workspace = SampleEntity.workspace(SampleEntity.user).withId(2L)
            ).withId(5L)
            val file = mockk<MultipartFile>()
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible(username, 1L) } just Runs
            every { productService.getImageUrl(1L, 40L, file) } returns null
            every { productService.getProductCategory(5L) } returns otherCategory

            val ex = assertThrows<CustomException> {
                sut.updateProduct(username, 1L, 40L, "name", "description", 100, 5L, file)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProduct(40L) }
            verify { workspaceService.checkAccessible(username, 1L) }
            verify { productService.getImageUrl(1L, 40L, file) }
            verify(exactly = 0) { s3Service.deleteFile(any()) }
            verify { productService.getProductCategory(5L) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) if the product is not in the requested workspace") {
            // 두 주점 모두 접근할 수 있는 계정이 한 주점 id로 다른 주점 상품을 고치려는 경우
            val username = "username"
            val product = Product(
                name = "감자전",
                description = "description",
                price = 5000,
                workspace = SampleEntity.workspace(SampleEntity.user).withId(7L)
            ).withId(40L)
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible(username, 7L) } just Runs

            val ex = assertThrows<CustomException> {
                sut.updateProduct(username, 1L, 40L, "name", null, null, 5L, null)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            assertEquals("감자전", product.name)
            verify(exactly = 0) { productService.getProductCategory(any()) }
            verify(exactly = 0) { productService.saveProduct(any()) }
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val productId = 1L
            val product = SampleEntity.product
            every { productService.getProduct(productId) } returns product
            every {
                workspaceService.checkAccessible(
                    username,
                    product.workspace.id
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.deleteProduct(username, productId)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, product.workspace.id) }
            verify(exactly = 0) { productService.deleteProduct(any()) }
        }

        it("세트 구성품의 연결 상품이면 PRODUCT_USED_AS_SET_CONDITION과 연결 세트 목록") {
            val single = SampleEntity.newProduct(10)
            val set = SampleEntity.newSetProduct(20, listOf(single)).apply { name = "족발 막국수 세트" }
            every { productService.getProduct(10L) } returns single
            every { workspaceService.checkAccessible("username", 1L) } just Runs
            every { productService.getLinkedSets(listOf(10L)) } returns mapOf(10L to listOf(set))

            val ex = assertThrows<CustomException> {
                sut.deleteProduct("username", 10L)
            }

            ex.errorCode shouldBe ErrorCode.PRODUCT_USED_AS_SET_CONDITION
            ex.details shouldBe mapOf("linkedSets" to listOf(mapOf("id" to 20L, "name" to "족발 막국수 세트")))
            verify(exactly = 0) { productService.deleteProduct(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<ProductDeletedEvent>()) }
        }

        it("세트는 구성품과 함께 지운다") {
            val set = SampleEntity.newSetProduct(20, listOf(SampleEntity.newProduct(10)))
            every { productService.getProduct(20L) } returns set
            every { workspaceService.checkAccessible("username", 1L) } just Runs
            every { productService.deleteProduct(set) } returns set

            val result = sut.deleteProduct("username", 20L)

            result.type shouldBe ProductType.SET
            result.components.size shouldBe 1
            verify { productService.deleteProduct(set) }
            verify { eventPublisher.publishEvent(any<ProductDeletedEvent>()) }
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.createProductCategory(username, workspaceId, name)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
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
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.deleteProductCategory(username, workspaceId, productCategoryId)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProductCategory(productCategoryId) }
            verify { workspaceService.checkAccessible(username, productCategory.workspace.id) }
            verify(exactly = 0) { productService.checkProductCategoryDeletable(any(), any()) }
            verify(exactly = 0) { productService.deleteProductCategory(any()) }
        }

        it("should throw CustomException if product category is not deletable") {
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
            } throws CustomException(ErrorCode.CANNOT_DELETE_USING_PRODUCT_CATEGORY)

            val ex = assertThrows<CustomException> {
                sut.deleteProductCategory(username, workspaceId, productCategoryId)
            }
            assertEquals(ErrorCode.CANNOT_DELETE_USING_PRODUCT_CATEGORY, ex.errorCode)

            verify { productService.getProductCategory(productCategoryId) }
            verify { workspaceService.checkAccessible(username, productCategory.workspace.id) }
            verify { productService.checkProductCategoryDeletable(workspaceId, productCategoryId) }
            verify(exactly = 0) { productService.deleteProductCategory(any()) }
        }
    }

    describe("sortProductCategories") {
        it("should sort product categories") {
            val username = "username"
            val workspaceId = SampleEntity.productCategory.workspace.id
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
            val username = "username"
            val workspaceId = 1L
            val productCategoryIds = listOf(1L, 2L, 3L)
            every {
                workspaceService.checkAccessible(
                    username,
                    workspaceId
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.sortProductCategories(username, workspaceId, productCategoryIds)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.getProductCategories(any()) }
            verify(exactly = 0) { productService.saveProductCategories(any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) if a category is in another workspace") {
            val username = "username"
            val ownCategory = ProductCategory(
                name = "own",
                workspace = SampleEntity.workspace(SampleEntity.user).withId(1L)
            ).withId(5L)
            val otherCategory = ProductCategory(
                name = "other",
                workspace = SampleEntity.workspace(SampleEntity.user).withId(7L)
            ).withId(6L)
            every { workspaceService.checkAccessible(username, 1L) } just Runs
            every { productService.getProductCategories(listOf(5L, 6L)) } returns listOf(ownCategory, otherCategory)

            val ex = assertThrows<CustomException> {
                sut.sortProductCategories(username, 1L, listOf(5L, 6L))
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            assertEquals(null, otherCategory.index)
            verify(exactly = 0) { productService.saveProductCategories(any()) }
        }
    }

    describe("updateProductStatus") {
        it("should update product status") {
            val username = "username"
            val workspaceId = SampleEntity.product.workspace.id
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

        it("should throw CustomException(WORKSPACE_INACCESSIBLE)") {
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
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.updateProductStatus(username, workspaceId, productId, status)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { productService.getProduct(productId) }
            verify { workspaceService.checkAccessible(username, workspaceId) }
            verify(exactly = 0) { productService.saveProduct(any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) if the product is in another workspace") {
            val username = "username"
            val product = Product(
                name = "감자전",
                description = "description",
                price = 5000,
                workspace = SampleEntity.workspace(SampleEntity.user).withId(7L)
            ).withId(40L)
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible(username, 1L) } just Runs

            val ex = assertThrows<CustomException> {
                sut.updateProductStatus(username, 1L, 40L, ProductStatus.SOLD_OUT)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            assertEquals(ProductStatus.SELLING, product.status)
            verify(exactly = 0) { productService.saveProduct(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
        }
    }

    describe("change log events") {
        // 이벤트가 엔티티 쪽 workspace(7)를 쓰는지 본다. 요청 workspaceId가 다르면 그 전에 거절된다.
        fun productOfWorkspace7() = Product(
            name = "감자전",
            description = "description",
            price = 5000,
            status = ProductStatus.SELLING,
            workspace = SampleEntity.workspace(SampleEntity.user).withId(7L)
        ).withId(40L)

        it("updateProductStatus should publish the before and after snapshots") {
            val product = productOfWorkspace7()
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible("username", 7L) } just Runs
            every { productService.saveProduct(product) } returns product

            sut.updateProductStatus("username", 7L, 40L, ProductStatus.SOLD_OUT)

            verify(exactly = 1) {
                eventPublisher.publishEvent(
                    ProductChangedEvent(
                        7L,
                        40L,
                        ProductSnapshot("감자전", 5000, ProductStatus.SELLING, null),
                        ProductSnapshot("감자전", 5000, ProductStatus.SOLD_OUT, null)
                    )
                )
            }
        }

        it("updateProduct should publish the before and after snapshots") {
            val product = productOfWorkspace7()
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible("username", 7L) } just Runs
            every { productService.getImageUrl(7L, 40L, null) } returns null
            every { productService.saveProduct(product) } returns product

            sut.updateProduct("username", 7L, 40L, "감자전(마감)", null, 3000, null, null)

            verify(exactly = 1) {
                eventPublisher.publishEvent(
                    ProductChangedEvent(
                        7L,
                        40L,
                        ProductSnapshot("감자전", 5000, ProductStatus.SELLING, null),
                        ProductSnapshot("감자전(마감)", 3000, ProductStatus.SELLING, null)
                    )
                )
            }
        }

        it("deleteProduct should publish the snapshot after the delete succeeds") {
            val product = productOfWorkspace7()
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible("username", 7L) } just Runs
            every { productService.deleteProduct(product) } returns product

            sut.deleteProduct("username", 40L)

            verifyOrder {
                productService.deleteProduct(product)
                eventPublisher.publishEvent(
                    ProductDeletedEvent(7L, 40L, ProductSnapshot("감자전", 5000, ProductStatus.SELLING, null))
                )
            }
        }

        it("deleteProduct should not publish when the delete fails") {
            val product = productOfWorkspace7()
            every { productService.getProduct(40L) } returns product
            every { workspaceService.checkAccessible("username", 7L) } just Runs
            every { productService.deleteProduct(product) } throws RuntimeException("delete failed")

            assertThrows<RuntimeException> { sut.deleteProduct("username", 40L) }

            verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
        }
    }
})

private fun <T : BaseEntity> T.withId(id: Long): T = apply {
    BaseEntity::class.java.getDeclaredField("id").apply { isAccessible = true }.set(this, id)
}
