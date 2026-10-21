package com.kioschool.kioschoolapi.domain.product.service

import com.kioschool.kioschoolapi.domain.product.dto.common.AdminProductDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.SetComponentRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.cache.annotation.ProductCategoriesUpdateEvent
import com.kioschool.kioschoolapi.global.cache.annotation.ProductCategoryUpdateEvent
import com.kioschool.kioschoolapi.global.cache.annotation.ProductUpdateEvent
import com.kioschool.kioschoolapi.global.cache.annotation.ProductsUpdateEvent
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class ProductService(
    @Value("\${cloud.aws.s3.default-path}")
    private val productPath: String,
    private val productRepository: ProductRepository,
    private val customProductRepository: CustomProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val s3Service: S3Service,
    private val setComponentRepository: SetComponentRepository
) {
    fun getAllProductsByCondition(
        workspaceId: Long,
        productCategoryId: Long? = null
    ): List<Product> {
        return customProductRepository.findAllByCondition(
            workspaceId,
            productCategoryId
        )
    }

    // 키는 연결 상품 id, 값은 그 상품을 구성품의 연결 상품으로 쓰는 세트(중복 없이 id 순)
    fun getLinkedSets(productIds: Collection<Long>): Map<Long, List<Product>> {
        if (productIds.isEmpty()) return emptyMap()
        return setComponentRepository.findAllByConditionProductIdIn(productIds)
            .groupBy({ it.conditionProduct!!.id }, { it.setProduct })
            .mapValues { (_, sets) -> sets.distinctBy { it.id }.sortedBy { it.id } }
    }

    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId).orElseThrow()
    }

    fun getProducts(productIds: List<Long>): List<Product> {
        return productRepository.findAllById(productIds)
    }

    fun getProductCategory(productCategoryId: Long): ProductCategory {
        return productCategoryRepository.findById(productCategoryId).orElseThrow()
    }

    fun getProductCategories(productCategoryIds: List<Long>): List<ProductCategory> {
        return productCategoryRepository.findAllById(productCategoryIds)
    }

    @ProductUpdateEvent
    fun saveProduct(product: Product): Product {
        return productRepository.save(product)
    }

    @ProductsUpdateEvent
    fun saveProducts(products: List<Product>): List<Product> {
        return productRepository.saveAll(products)
    }

    fun saveProduct(
        name: String,
        price: Int,
        description: String,
        workspace: Workspace,
        productCategoryId: Long?
    ): Product {
        return productRepository.save(
            Product(
                name = name,
                price = price,
                description = description,
                workspace = workspace,
                productCategory = productCategoryId?.let {
                    val productCategory = productCategoryRepository.findById(it).orElseThrow()
                    if (productCategory.workspace.id != workspace.id) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
                    productCategory
                }
            )
        )
    }

    @ProductCategoriesUpdateEvent
    fun saveProductCategories(productCategories: List<ProductCategory>): List<ProductCategory> {
        return productCategoryRepository.saveAll(productCategories)
    }

    fun getAllProductCategories(workspaceId: Long): List<ProductCategory> {
        return productCategoryRepository.findAllByWorkspaceIdOrderByIndexAsc(workspaceId)
    }

    // 관리자 상품 목록. 캐시가 맞으면 본문을 건너뛰므로 접근 확인은 부르는 쪽에서 먼저 한다.
    // 연결 세트는 일반 상품에만 있으므로 일반 상품 id로 한 번만 조회한다.
    @Cacheable(cacheNames = [CacheNames.PRODUCTS], key = "#workspaceId")
    fun getCachedProducts(workspaceId: Long): List<AdminProductDto> {
        val products = getAllProductsByCondition(workspaceId)
        val linkedSets = getLinkedSets(products.filter { it.type == ProductType.SINGLE }.map { it.id })
        return products.map { AdminProductDto.of(it, linkedSets[it.id].orEmpty()) }
    }

    @Cacheable(cacheNames = [CacheNames.PRODUCT_CATEGORIES], key = "#workspaceId")
    fun getCachedProductCategories(workspaceId: Long): List<ProductCategoryDto> {
        return getAllProductCategories(workspaceId).map { ProductCategoryDto.of(it) }
    }

    fun getImageUrl(workspaceId: Long, productId: Long, file: MultipartFile?): String? {
        val date = System.currentTimeMillis()
        val path =
            "$productPath/workspace$workspaceId/product/product-${productId}/${date.hashCode()}.webp"
        return if (file != null) s3Service.uploadResizedWebpImage(file.inputStream, path) else null
    }

    @ProductCategoryUpdateEvent
    fun saveProductCategory(productCategory: ProductCategory): ProductCategory {
        return productCategoryRepository.save(productCategory)
    }

    @ProductCategoryUpdateEvent
    @Transactional
    fun deleteProductCategory(productCategory: ProductCategory): ProductCategory {
        productCategoryRepository.delete(productCategory)
        return productCategory
    }

    fun checkProductCategoryDeletable(workspaceId: Long, productCategoryId: Long) {
        if (!isDeletableProductCategory(
                workspaceId,
                productCategoryId
            )
        ) throw CustomException(ErrorCode.CANNOT_DELETE_USING_PRODUCT_CATEGORY)

    }

    private fun isDeletableProductCategory(workspaceId: Long, productCategoryId: Long): Boolean {
        return productRepository.countByWorkspaceIdAndProductCategoryId(
            workspaceId,
            productCategoryId
        ) == 0L
    }

    @ProductUpdateEvent
    @Transactional
    fun deleteProduct(product: Product): Product {
        productRepository.delete(product)
        return product
    }

    fun validateProducts(
        workspaceId: Long,
        productIds: List<Long>,
    ) {
        val products = productRepository.findAllByIdInAndWorkspaceId(productIds, workspaceId)
        if (products.size != productIds.size) {
            throw CustomException(ErrorCode.NOT_FOUND_PRODUCT)
        }
        if (products.any { it.status != ProductStatus.SELLING }) {
            throw CustomException(ErrorCode.NOT_SELLABLE_PRODUCT)
        }
    }
}
