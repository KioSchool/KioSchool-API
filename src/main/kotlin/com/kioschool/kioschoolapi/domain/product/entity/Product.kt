package com.kioschool.kioschoolapi.domain.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class Product(
    var name: String,
    var description: String,
    var price: Int,
    var imageUrl: String? = null,
    var status: ProductStatus = ProductStatus.SELLING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne(fetch = FetchType.LAZY)
    var productCategory: ProductCategory? = null
) : BaseEntity()