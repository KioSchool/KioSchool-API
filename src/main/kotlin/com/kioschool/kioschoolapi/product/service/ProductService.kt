package com.kioschool.kioschoolapi.product.service

import com.kioschool.kioschoolapi.aws.S3Service
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.repository.ProductRepository
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class ProductService(
    @Value("\${cloud.aws.s3.default-path}")
    private val productPath: String,
    private val productRepository: ProductRepository,
    private val workspaceService: WorkspaceService,
    private val s3Service: S3Service
) {
    fun getProducts(workspaceId: Long): List<Product> {
        return productRepository.findAllByWorkspaceId(workspaceId)
    }

    fun createProduct(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        price: Int,
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
            )
        )
        val path = "$productPath/workspace$workspaceId/product${product.id}"
        val imageUrl = if (file != null) s3Service.uploadFile(file, path) else null
        product.imageUrl = imageUrl

        return productRepository.save(product)
    }

    fun updateProduct(
        username: String,
        workspaceId: Long,
        productId: Long,
        name: String?,
        description: String?,
        price: Int?,
        file: MultipartFile?
    ): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (!workspaceService.isAccessible(
                username,
                workspace
            )
        ) throw WorkspaceInaccessibleException()

        val product = productRepository.findById(productId).orElseThrow()
        if (name != null) product.name = name
        if (description != null) product.description = description
        if (price != null) product.price = price
        val path = "$productPath/workspace$workspaceId/product${product.id}"
        val imageUrl = if (file != null) s3Service.uploadFile(file, path) else null
        if (imageUrl != null) product.imageUrl = imageUrl

        return productRepository.save(product)
    }
}