package com.kioschool.kioschoolapi.domain.workspace.entity

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
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