package com.kioschool.kioschoolapi.product.facade

import com.kioschool.kioschoolapi.domain.product.facade.ProductFacade
import com.kioschool.kioschoolapi.domain.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.SetComponentRepository
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Supplier

// 캐시 프록시를 실제로 거쳐야 캐시 적중 시 동작을 볼 수 있어서 최소한의 Spring 컨텍스트를 띄운다.
class ProductFacadeCacheTest : DescribeSpec({
    val productRepository = mockk<ProductRepository>()
    val customProductRepository = mockk<CustomProductRepository>()
    val productCategoryRepository = mockk<ProductCategoryRepository>()
    val setComponentRepository = mockk<SetComponentRepository>()
    val workspaceService = mockk<WorkspaceService>()
    val s3Service = mockk<S3Service>()

    val owner = "owner"
    val other = "other"
    val workspaceId = 1L

    lateinit var context: AnnotationConfigApplicationContext
    lateinit var sut: ProductFacade

    beforeTest {
        every { workspaceService.checkAccessible(owner, workspaceId) } just Runs
        every {
            workspaceService.checkAccessible(other, workspaceId)
        } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        every { customProductRepository.findAllByCondition(workspaceId, null) } returns listOf(SampleEntity.product)
        every {
            productCategoryRepository.findAllByWorkspaceIdOrderByIndexAsc(workspaceId)
        } returns listOf(SampleEntity.productCategory)
        every { setComponentRepository.findAllByConditionProductIdIn(any()) } returns emptyList()

        context = AnnotationConfigApplicationContext().apply {
            register(ProductFacadeCacheTestConfiguration::class.java)
            registerBean(ProductService::class.java, Supplier {
                ProductService(
                    "test",
                    productRepository,
                    customProductRepository,
                    productCategoryRepository,
                    s3Service,
                    setComponentRepository
                )
            })
            registerBean(WorkspaceService::class.java, Supplier { workspaceService })
            registerBean(S3Service::class.java, Supplier { s3Service })
            register(ProductFacade::class.java)
            refresh()
        }
        sut = context.getBean(ProductFacade::class.java)
    }

    afterTest {
        context.close()
        clearAllMocks()
    }

    describe("getProducts by username and workspaceId") {
        it("should check access even when products are cached") {
            sut.getProducts(owner, workspaceId)

            val ex = shouldThrow<CustomException> {
                sut.getProducts(other, workspaceId)
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE
            verify { workspaceService.checkAccessible(other, workspaceId) }
        }

        it("should keep caching products by workspaceId") {
            sut.getProducts(owner, workspaceId)
            sut.getProducts(owner, workspaceId)

            verify(exactly = 1) { customProductRepository.findAllByCondition(workspaceId, null) }
            verify(exactly = 2) { workspaceService.checkAccessible(owner, workspaceId) }
            context.getBean(CacheManager::class.java).getCache(CacheNames.PRODUCTS)
                ?.get(workspaceId).shouldNotBeNull()
        }
    }

    describe("getProductCategories by username and workspaceId") {
        it("should check access even when categories are cached by the public endpoint") {
            sut.getProductCategories(workspaceId)

            val ex = shouldThrow<CustomException> {
                sut.getProductCategories(other, workspaceId)
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE
            verify { workspaceService.checkAccessible(other, workspaceId) }
        }

        it("should keep caching categories by workspaceId") {
            sut.getProductCategories(workspaceId)
            sut.getProductCategories(owner, workspaceId)

            verify(exactly = 1) { productCategoryRepository.findAllByWorkspaceIdOrderByIndexAsc(workspaceId) }
            verify(exactly = 1) { workspaceService.checkAccessible(owner, workspaceId) }
            context.getBean(CacheManager::class.java).getCache(CacheNames.PRODUCT_CATEGORIES)
                ?.get(workspaceId).shouldNotBeNull()
        }
    }
})

@Configuration(proxyBeanMethods = false)
@EnableCaching
class ProductFacadeCacheTestConfiguration {
    @Bean
    fun cacheManager(): CacheManager = ConcurrentMapCacheManager()
}
