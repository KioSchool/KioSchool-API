package com.kioschool.kioschoolapi.workspace.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "workspace")
class Workspace(
    val name: String,
    @ManyToOne
    val owner: User,
    @OneToMany
    @JoinTable(name = "user_workspace")
    @JoinColumn(name = "workspace_id")
    val users: MutableList<User>
) : BaseEntity()