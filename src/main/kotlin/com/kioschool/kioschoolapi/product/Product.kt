package com.kioschool.kioschoolapi.product

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class Product(
    val name: String,
    val description: String,
    val price: Int,
    val imageUrl: String,
    @ManyToOne
    val workspace: Workspace,
) : BaseEntity()