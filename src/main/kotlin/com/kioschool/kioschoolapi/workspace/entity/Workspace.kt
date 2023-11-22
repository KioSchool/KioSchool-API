package com.kioschool.kioschoolapi.workspace.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
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
    @OneToMany
    val member: List<WorkspaceMember>,
) : BaseEntity()