package com.kioschool.kioschoolapi.product

import com.kioschool.kioschoolapi.aws.S3Service
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
        workspaceId: Long,
        name: String,
        description: String,
        price: Int,
        file: MultipartFile?
    ): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val path = "$productPath/$workspaceId"
        val imageUrl = if (file != null) s3Service.uploadFile(file, path) else null

        return productRepository.save(
            Product(
                name = name,
                price = price,
                description = description,
                workspace = workspace,
                imageUrl = imageUrl
            )
        )
    }
}