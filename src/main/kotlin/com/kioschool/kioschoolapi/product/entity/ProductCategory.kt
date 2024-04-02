package com.kioschool.kioschoolapi.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_category")
class ProductCategory(
    var name: String,
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    var index: Int? = null
) : BaseEntity()