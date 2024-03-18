package com.kioschool.kioschoolapi.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.entity.ProductCategory
import com.kioschool.kioschoolapi.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "workspace")
class Workspace(
    val name: String,
    @ManyToOne
    val owner: User,
    @JsonIgnore
    @OneToMany(
        mappedBy = "workspace",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    val members: MutableList<WorkspaceMember> = mutableListOf(),
    @OneToMany(
        mappedBy = "workspace",
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    val products: MutableList<Product> = mutableListOf(),
    @OneToMany(
        mappedBy = "workspace",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    val productCategories: MutableList<ProductCategory> = mutableListOf(),
    @JsonIgnore
    @OneToMany(
        mappedBy = "workspace",
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    val invitations: MutableList<WorkspaceInvitation> = mutableListOf(),
) : BaseEntity()