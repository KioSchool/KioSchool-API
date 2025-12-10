package com.kioschool.kioschoolapi.domain.product.service

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.exception.CanNotDeleteUsingProductCategoryException
import com.kioschool.kioschoolapi.domain.product.exception.NotFoundProductException
import com.kioschool.kioschoolapi.domain.product.exception.NotSellableProductException
import com.kioschool.kioschoolapi.domain.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.cache.annotation.ProductCategoriesUpdateEvent
import com.kioschool.kioschoolapi.global.cache.annotation.ProductCategoryUpdateEvent
import com.kioschool.kioschoolapi.global.cache.annotation.ProductUpdateEvent
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class ProductService(
    @Value("\${cloud.aws.s3.default-path}")
    private val productPath: String,
    private val productRepository: ProductRepository,
    private val customProductRepository: CustomProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val s3Service: S3Service
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

    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId).orElseThrow()
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
                    if (productCategory.workspace.id != workspace.id) throw WorkspaceInaccessibleException()
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

    fun getImageUrl(workspaceId: Long, productId: Long, file: MultipartFile?): String? {
        val date = System.currentTimeMillis()
        val path =
            "$productPath/workspace$workspaceId/product/product${productId}/${date.hashCode()}.jpg"
        return if (file != null) s3Service.uploadFile(file, path) else null
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
        ) throw CanNotDeleteUsingProductCategoryException()

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
            throw NotFoundProductException()
        }
        if (products.any { it.status != ProductStatus.SELLING }) {
            throw NotSellableProductException()
        }
    }
}
