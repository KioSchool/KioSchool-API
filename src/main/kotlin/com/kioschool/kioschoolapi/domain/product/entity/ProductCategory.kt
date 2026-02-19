package com.kioschool.kioschoolapi.domain.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_category")
class ProductCategory(
    var name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val workspace: Workspace,
    var index: Int? = null
) : BaseEntity()