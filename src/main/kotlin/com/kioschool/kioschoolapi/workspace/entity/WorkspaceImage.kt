package com.kioschool.kioschoolapi.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_image")
class WorkspaceImage(
    @JsonIgnore
    @ManyToOne
    val workspace: Workspace,
    val url: String
) : BaseEntity()