package com.kioschool.kioschoolapi.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.user.entity.User
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