package com.kioschool.kioschoolapi.domain.product.facade

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.aws.S3Service
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class ProductFacade(
    private val productService: ProductService,
    private val workspaceService: WorkspaceService,
    private val s3Service: S3Service
) {
    fun getProduct(username: String, productId: Long): Product {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        return product
    }

    fun getProducts(username: String, workspaceId: Long): List<Product> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getAllProductsByCondition(workspaceId)
    }

    fun getProducts(workspaceId: Long, categoryId: Long?) =
        productService.getAllProductsByCondition(workspaceId, categoryId)

    fun getProductCategories(workspaceId: Long) =
        productService.getAllProductCategories(workspaceId)

    fun getProductCategories(username: String, workspaceId: Long): List<ProductCategory> {
        workspaceService.checkAccessible(username, workspaceId)
        return productService.getAllProductCategories(workspaceId)
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

        return productService.saveProduct(product)
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

        return productService.saveProduct(product)
    }

    fun updateProductSellable(
        username: String,
        workspaceId: Long,
        productId: Long,
        isSellable: Boolean
    ): Product {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, workspaceId)

        product.isSellable = isSellable
        return productService.saveProduct(product)
    }

    fun deleteProduct(username: String, productId: Long): Product {
        val product = productService.getProduct(productId)
        workspaceService.checkAccessible(username, product.workspace.id)
        return productService.deleteProduct(product)
    }

    fun createProductCategory(username: String, workspaceId: Long, name: String): ProductCategory {
        workspaceService.checkAccessible(username, workspaceId)
        val workspace = workspaceService.getWorkspace(workspaceId)

        return productService.saveProductCategory(
            ProductCategory(
                name = name,
                workspace = workspace
            )
        )
    }

    fun deleteProductCategory(
        username: String,
        workspaceId: Long,
        productCategoryId: Long
    ): ProductCategory {
        val productCategory = productService.getProductCategory(productCategoryId)
        workspaceService.checkAccessible(username, productCategory.workspace.id)
        productService.checkProductCategoryDeletable(workspaceId, productCategoryId)

        return productService.deleteProductCategory(productCategory)
    }

    fun sortProductCategories(
        username: String,
        workspaceId: Long,
        productCategoryIds: List<Long>
    ): List<ProductCategory> {
        workspaceService.checkAccessible(username, workspaceId)

        val productCategories = productService.getProductCategories(productCategoryIds)
        val productCategoryMap = productCategories.associateBy { it.id }
        productCategoryIds.forEachIndexed { index, productCategoryId ->
            productCategoryMap[productCategoryId]!!.index = index
        }
        return productService.saveProductCategories(productCategories)
    }
}