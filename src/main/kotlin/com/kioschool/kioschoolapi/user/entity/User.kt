package com.kioschool.kioschoolapi.user.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import jakarta.persistence.*

@Entity
@Table(name = "user", schema = "PUBLIC")
class User(
    var loginId: String,
    var loginPassword: String,
    var name: String,
    var email: String,
    var role: UserRole,
    @OneToMany
    @JoinTable(name = "user_workspace")
    @JoinColumn(name = "user_id")
    var workspaces: MutableList<Workspace>
) : BaseEntity()