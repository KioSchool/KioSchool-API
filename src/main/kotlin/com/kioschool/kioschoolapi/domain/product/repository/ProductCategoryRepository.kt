package com.kioschool.kioschoolapi.domain.product.repository

import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import org.springframework.data.jpa.repository.JpaRepository

interface ProductCategoryRepository : JpaRepository<ProductCategory, Long> {
    fun findAllByWorkspaceIdOrderByIndexAsc(workspaceId: Long): List<ProductCategory>
}