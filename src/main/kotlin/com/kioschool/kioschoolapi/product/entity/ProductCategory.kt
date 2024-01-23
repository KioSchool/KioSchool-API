package com.kioschool.kioschoolapi.product.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "product_category")
class ProductCategory(
    var name: String
) : BaseEntity()