package com.kioschool.kioschoolapi.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class Product(
    var name: String,
    var description: String,
    var price: Int,
    var imageUrl: String? = null,
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne
    var productCategory: ProductCategory? = null
) : BaseEntity()