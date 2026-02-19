package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "workspace")
class Workspace(
    var name: String,
    @ManyToOne
    val owner: User,
    @JsonIgnore
    @OneToMany(
        mappedBy = "workspace",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val members: MutableList<WorkspaceMember> = mutableListOf(),
    @OneToMany(
        mappedBy = "workspace",
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("id")
    val products: MutableList<Product> = mutableListOf(),
    @OneToMany(
        mappedBy = "workspace",
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val productCategories: MutableList<ProductCategory> = mutableListOf(),
    @JsonIgnore
    @OneToMany(
        mappedBy = "workspace",
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val invitations: MutableList<WorkspaceInvitation> = mutableListOf(),
    @OneToMany(
        mappedBy = "workspace",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val images: MutableList<WorkspaceImage> = mutableListOf(),
    var description: String = "",
    var notice: String = "",
    var memo: String = "",
    var tableCount: Int = 1,
    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var workspaceSetting: WorkspaceSetting,
) : BaseEntity()