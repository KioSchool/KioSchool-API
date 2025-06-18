package com.kioschool.kioschoolapi.domain.product.repository

import com.kioschool.kioschoolapi.domain.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun countByWorkspaceIdAndProductCategoryId(workspaceId: Long, productCategoryId: Long): Long
    fun findAllByIdInAndWorkspaceId(productIds: List<Long>, workspaceId: Long): List<Product>
}