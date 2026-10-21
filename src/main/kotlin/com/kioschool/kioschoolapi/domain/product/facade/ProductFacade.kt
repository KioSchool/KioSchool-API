package com.kioschool.kioschoolapi.domain.product.facade

import com.kioschool.kioschoolapi.domain.product.dto.common.AdminProductDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.product.dto.request.CategoryProductSortInfo
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.event.ProductChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductDeletedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductSnapshot
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class ProductFacade(
    private val productService: ProductService,
    private val workspaceService: WorkspaceService,
    private val s3Service: S3Service,
    private val eventPublisher: ApplicationEventPublisher
) {
    fun getProduct(username: String, productId: Long): AdminProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        return toAdminDto(product)
    }

    // 캐시는 ProductService에 둔다. 여기에 두면 캐시가 맞을 때 접근 확인까지 건너뛴다.
    fun getProducts(username: String, workspaceId: Long): List<AdminProductDto> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getCachedProducts(workspaceId)
    }

    fun getProducts(workspaceId: Long, categoryId: Long?) =
        productService.getAllProductsByCondition(workspaceId, categoryId).map { ProductDto.of(it) }

    fun getProductCategories(workspaceId: Long) =
        productService.getCachedProductCategories(workspaceId)

    fun getProductCategories(username: String, workspaceId: Long): List<ProductCategoryDto> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getCachedProductCategories(workspaceId)
    }

    fun createProduct(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        price: Int,
        productCategoryId: Long?,
        file: MultipartFile?
    ): AdminProductDto {
        workspaceService.checkAccessible(username, workspaceId)
        val workspace = workspaceService.getWorkspace(workspaceId)

        val product = productService.saveProduct(
            name,
            price,
            description,
            workspace,
            productCategoryId
        )
        product.imageUrl = productService.getImageUrl(workspaceId, product.id, file)

        return toAdminDto(productService.saveProduct(product))
    }

    fun updateProduct(
        username: String,
        workspaceId: Long,
        productId: Long,
        name: String?,
        description: String?,
        price: Int?,
        productCategoryId: Long?,
        file: MultipartFile?
    ): AdminProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        // 아래 카테고리 확인이 요청 workspaceId 기준이라, 상품 소속과 다르면 남의 카테고리를 붙일 수 있다.
        if (product.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        val before = ProductSnapshot.of(product)

        name?.let { product.name = it }
        description?.let { product.description = it }
        price?.let { product.price = it }

        val imageUrl = productService.getImageUrl(product.workspace.id, product.id, file)
        imageUrl?.let {
            s3Service.deleteFile(product.imageUrl!!)
            product.imageUrl = it
        }

        if (productCategoryId != null) {
            val productCategory = productService.getProductCategory(productCategoryId)
            if (productCategory.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
            product.productCategory = productCategory
        } else {
            product.productCategory = null
        }

        val saved = productService.saveProduct(product)
        eventPublisher.publishEvent(
            ProductChangedEvent(product.workspace.id, product.id, before, ProductSnapshot.of(product))
        )
        return toAdminDto(saved)
    }

    fun deleteProduct(username: String, productId: Long): AdminProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        val snapshot = ProductSnapshot.of(product)
        // 지운 뒤에는 구성품·연결 상품을 지연 로딩할 수 없으므로 응답을 먼저 만든다.
        val response = toAdminDto(product)

        // 삭제가 실패했는데 '삭제됨' 이력만 남지 않도록 삭제가 끝난 뒤에 발행한다.
        productService.deleteProduct(product)
        eventPublisher.publishEvent(ProductDeletedEvent(product.workspace.id, product.id, snapshot))
        return response
    }

    fun createProductCategory(
        username: String,
        workspaceId: Long,
        name: String
    ): ProductCategoryDto {
        workspaceService.checkAccessible(username, workspaceId)
        val workspace = workspaceService.getWorkspace(workspaceId)

        return ProductCategoryDto.of(
            productService.saveProductCategory(
                ProductCategory(
                    name = name,
                    workspace = workspace
                )
            )
        )
    }

    fun deleteProductCategory(
        username: String,
        workspaceId: Long,
        productCategoryId: Long
    ): ProductCategoryDto {
        val productCategory = productService.getProductCategory(productCategoryId)
        workspaceService.checkAccessible(username, productCategory.workspace.id)
        productService.checkProductCategoryDeletable(workspaceId, productCategoryId)

        return ProductCategoryDto.of(productService.deleteProductCategory(productCategory))
    }

    fun sortProductCategories(
        username: String,
        workspaceId: Long,
        productCategoryIds: List<Long>
    ): List<ProductCategoryDto> {
        workspaceService.checkAccessible(username, workspaceId)

        val productCategories = productService.getProductCategories(productCategoryIds)
        productCategories.forEach {
            if (it.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        }
        val productCategoryMap = productCategories.associateBy { it.id }
        productCategoryIds.forEachIndexed { index, productCategoryId ->
            productCategoryMap[productCategoryId]!!.index = index
        }
        return productService.saveProductCategories(productCategories)
            .sortedBy { it.index ?: Int.MAX_VALUE }
            .map { ProductCategoryDto.of(it) }
    }

    fun sortProducts(
        username: String,
        workspaceId: Long,
        sorts: List<CategoryProductSortInfo>
    ): List<AdminProductDto> {
        workspaceService.checkAccessible(username, workspaceId)

        val uniqueCategoryIds = sorts.mapNotNull { it.categoryId }.distinct()
        val categories = productService.getProductCategories(uniqueCategoryIds)
        val categoryMap = categories.associateBy { it.id }

        categories.forEach {
            if (it.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        }

        val allProductIds = sorts.flatMap { it.productIds }.distinct()
        val products = productService.getProducts(allProductIds)
        products.forEach {
            if (it.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        }
        val productMap = products.associateBy { it.id }

        sorts.forEach { sortInfo ->
            val targetCategory = sortInfo.categoryId?.let { categoryMap[it] }
            sortInfo.productIds.forEachIndexed { index, productId ->
                val product = productMap[productId]
                if (product != null) {
                    product.productCategory = targetCategory
                    product.index = index
                }
            }
        }
        return toAdminDtos(
            productService.saveProducts(products).sortedBy { it.index ?: Int.MAX_VALUE }
        )
    }

    fun updateProductStatus(
        username: String,
        workspaceId: Long,
        productId: Long,
        status: ProductStatus
    ): AdminProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, workspaceId)
        if (product.workspace.id != workspaceId) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        val before = ProductSnapshot.of(product)

        product.status = status
        val saved = productService.saveProduct(product)
        eventPublisher.publishEvent(
            ProductChangedEvent(product.workspace.id, product.id, before, ProductSnapshot.of(product))
        )
        return toAdminDto(saved)
    }

    private fun toAdminDto(product: Product): AdminProductDto = toAdminDtos(listOf(product)).single()

    // 연결 세트는 일반 상품에만 있으므로 일반 상품 id로 한 번만 조회한다.
    private fun toAdminDtos(products: List<Product>): List<AdminProductDto> {
        val linkedSets = productService.getLinkedSets(
            products.filter { it.type == ProductType.SINGLE }.map { it.id }
        )
        return products.map { AdminProductDto.of(it, linkedSets[it.id].orEmpty()) }
    }
}