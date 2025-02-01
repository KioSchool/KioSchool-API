package com.kioschool.kioschoolapi.workspace.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_image")
class WorkspaceImage(
    @ManyToOne
    val workspace: Workspace,
    val url: String
) : BaseEntity()