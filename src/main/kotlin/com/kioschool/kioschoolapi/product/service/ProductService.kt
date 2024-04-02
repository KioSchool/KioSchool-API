package com.kioschool.kioschoolapi.product.service

import com.kioschool.kioschoolapi.aws.S3Service
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.entity.ProductCategory
import com.kioschool.kioschoolapi.product.exception.CanNotDeleteUsingProductCategoryException
import com.kioschool.kioschoolapi.product.repository.CustomProductRepository
import com.kioschool.kioschoolapi.product.repository.ProductCategoryRepository
import com.kioschool.kioschoolapi.product.repository.ProductRepository
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
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
    private val workspaceService: WorkspaceService,
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

    fun createProduct(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        price: Int,
        productCategoryId: Long?,
        file: MultipartFile?
    ): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val product = productRepository.save(
            Product(
                name = name,
                price = price,
                description = description,
                workspace = workspace,
                productCategory = productCategoryId?.let {
                    val productCategory = productCategoryRepository.findById(it).orElseThrow()
                    if (productCategory.workspace.id != workspaceId) throw WorkspaceInaccessibleException()
                    productCategory
                }
            )
        )
        product.imageUrl = getImageUrl(workspaceId, product.id, file)

        return productRepository.save(product)
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
    ): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val product = productRepository.findById(productId).orElseThrow()

        name?.let { product.name = it }
        description?.let { product.description = it }
        price?.let { product.price = it }

        val imageUrl = getImageUrl(workspaceId, product.id, file)
        imageUrl?.let {
            s3Service.deleteFile(product.imageUrl!!)
            product.imageUrl = it
        }

        if (productCategoryId != null) {
            val productCategory =
                productCategoryRepository.findById(productCategoryId).orElseThrow()
            if (productCategory.workspace.id != workspaceId) throw WorkspaceInaccessibleException()
            product.productCategory = productCategory
        } else {
            product.productCategory = null
        }


        return productRepository.save(product)
    }

    fun getAllProductCategories(workspaceId: Long): List<ProductCategory> {
        return productCategoryRepository.findAllByWorkspaceIdOrderByIdAsc(workspaceId)
    }

    private fun getImageUrl(workspaceId: Long, productId: Long, file: MultipartFile?): String? {
        val date = System.currentTimeMillis()
        val path = "$productPath/workspace$workspaceId/product${productId}/${date.hashCode()}.jpg"
        return if (file != null) s3Service.uploadFile(file, path) else null
    }

    fun createProductCategory(username: String, workspaceId: Long, name: String): ProductCategory {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        return productCategoryRepository.save(
            ProductCategory(
                name = name,
                workspace = workspace
            )
        )
    }

    @Transactional
    fun deleteProductCategory(
        username: String,
        workspaceId: Long,
        productCategoryId: Long
    ): ProductCategory {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        if (!isDeletableProductCategory(
                workspaceId,
                productCategoryId
            )
        ) throw CanNotDeleteUsingProductCategoryException()

        val productCategory = productCategoryRepository.findById(productCategoryId).orElseThrow()
        productCategoryRepository.delete(productCategory)
        return productCategory
    }

    fun isDeletableProductCategory(workspaceId: Long, productCategoryId: Long): Boolean {
        return productRepository.countByWorkspaceIdAndProductCategoryId(
            workspaceId,
            productCategoryId
        ) == 0L
    }

    @Transactional
    fun deleteProduct(username: String, workspaceId: Long, productId: Long): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val product = productRepository.findById(productId).orElseThrow()
        productRepository.delete(product)
        return product
    }

    fun updateProductSellable(
        username: String,
        workspaceId: Long,
        productId: Long,
        sellable: Boolean
    ): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val product = productRepository.findById(productId).orElseThrow()
        product.isSellable = sellable
        return productRepository.save(product)
    }

    fun sortProductCategories(
        username: String,
        workspaceId: Long,
        productCategoryIds: List<Long>
    ): List<ProductCategory> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val productCategories = productCategoryRepository.findAllById(productCategoryIds)
        productCategories.forEachIndexed { index, productCategory ->
            productCategory.index = index
        }
        return productCategoryRepository.saveAll(productCategories)
    }
}