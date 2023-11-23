package com.kioschool.kioschoolapi.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findAllByWorkspaceId(workspaceId: Long): List<Product>
}