package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_member")
class WorkspaceMember(
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne
    @JsonIgnore
    val user: User
) : BaseEntity()