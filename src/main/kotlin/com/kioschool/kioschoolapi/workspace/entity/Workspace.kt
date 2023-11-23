package com.kioschool.kioschoolapi.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.product.Product
import com.kioschool.kioschoolapi.user.entity.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "workspace")
class Workspace(
    val name: String,
    @ManyToOne
    val owner: User,
    @OneToMany(mappedBy = "workspace")
    @JsonIgnore
    val members: MutableList<WorkspaceMember> = mutableListOf(),
    @OneToMany(mappedBy = "workspace")
    val products: MutableList<Product> = mutableListOf()
) : BaseEntity()