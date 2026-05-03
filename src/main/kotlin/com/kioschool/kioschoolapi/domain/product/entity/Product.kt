package com.kioschool.kioschoolapi.domain.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn

@Entity
@Table(
    name = "product",
    indexes = [
        Index(name = "idx_product_workspace_category_index", columnList = "workspace_id, product_category_id, index")
    ]
)
class Product(
    var name: String,
    var description: String,
    var price: Int,
    var imageUrl: String? = null,
    var status: ProductStatus = ProductStatus.SELLING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    var productCategory: ProductCategory? = null,
    var index: Int? = null
) : BaseEntity()