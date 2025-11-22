package com.kioschool.kioschoolapi.domain.product.facade

import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class ProductFacade(
    private val productService: ProductService,
    private val workspaceService: WorkspaceService,
    private val s3Service: S3Service
) {
    fun getProduct(username: String, productId: Long): ProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        return ProductDto.of(
            product
        )
    }

    @Cacheable(cacheNames = [CacheNames.PRODUCTS], key = "#workspaceId")
    fun getProducts(username: String, workspaceId: Long): List<ProductDto> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getAllProductsByCondition(workspaceId).map { ProductDto.of(it) }
    }

    fun getProducts(workspaceId: Long, categoryId: Long?) =
        productService.getAllProductsByCondition(workspaceId, categoryId).map { ProductDto.of(it) }

    @Cacheable(cacheNames = [CacheNames.PRODUCT_CATEGORIES], key = "#workspaceId")
    fun getProductCategories(workspaceId: Long) =
        productService.getAllProductCategories(workspaceId).map { ProductCategoryDto.of(it) }

    @Cacheable(cacheNames = [CacheNames.PRODUCT_CATEGORIES], key = "#workspaceId")
    fun getProductCategories(username: String, workspaceId: Long): List<ProductCategoryDto> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getAllProductCategories(workspaceId).map { ProductCategoryDto.of(it) }
    }

    fun createProduct(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        price: Int,
        productCategoryId: Long?,
        file: MultipartFile?
    ): ProductDto {
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

        return ProductDto.of(productService.saveProduct(product))
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
    ): ProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)

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
            if (productCategory.workspace.id != workspaceId) throw WorkspaceInaccessibleException()
            product.productCategory = productCategory
        } else {
            product.productCategory = null
        }

        return ProductDto.of(productService.saveProduct(product))
    }

    fun updateProductSellable(
        username: String,
        workspaceId: Long,
        productId: Long,
        isSellable: Boolean
    ): ProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, workspaceId)

        product.isSellable = isSellable
        return ProductDto.of(productService.saveProduct(product))
    }

    fun deleteProduct(username: String, productId: Long): ProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        return ProductDto.of(productService.deleteProduct(product))
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
        val productCategoryMap = productCategories.associateBy { it.id }
        productCategoryIds.forEachIndexed { index, productCategoryId ->
            productCategoryMap[productCategoryId]!!.index = index
        }
        return productService.saveProductCategories(productCategories)
            .map { ProductCategoryDto.of(it) }
    }

    fun updateProductStatus(
        username: String,
        workspaceId: Long,
        productId: Long,
        status: ProductStatus
    ): ProductDto {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, workspaceId)

        product.status = status
        return ProductDto.of(productService.saveProduct(product))
    }
}