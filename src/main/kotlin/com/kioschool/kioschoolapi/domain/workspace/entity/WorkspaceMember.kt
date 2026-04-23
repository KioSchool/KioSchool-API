package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_member")
class WorkspaceMember(
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val user: User
) : BaseEntity()