package com.kioschool.kioschoolapi.domain.workspace.entity

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_invitation")
class WorkspaceInvitation(
    @ManyToOne(fetch = FetchType.LAZY)
    val workspace: Workspace,
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,
) : BaseEntity()