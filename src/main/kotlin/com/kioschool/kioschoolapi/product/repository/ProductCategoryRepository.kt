package com.kioschool.kioschoolapi.product.repository

import com.kioschool.kioschoolapi.product.entity.ProductCategory
import org.springframework.data.jpa.repository.JpaRepository

interface ProductCategoryRepository : JpaRepository<ProductCategory, Long>