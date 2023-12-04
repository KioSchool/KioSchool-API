package com.kioschool.kioschoolapi.workspace.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.user.entity.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_invitation")
class WorkspaceInvitation(
    @ManyToOne
    val workspace: Workspace,
    @ManyToOne
    val user: User,
) : BaseEntity()