package com.kioschool.kioschoolapi.product

import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val workspaceService: WorkspaceService
) {
    fun getProducts(workspaceId: Long): List<Product> {
        return productRepository.findAllByWorkspaceId(workspaceId)
    }

    fun createProduct(workspaceId: Long, name: String, description: String, price: Int): Product {
        val workspace = workspaceService.getWorkspace(workspaceId)

        return productRepository.save(
            Product(
                name = name,
                price = price,
                description = description,
                workspace = workspace
            )
        )
    }
}