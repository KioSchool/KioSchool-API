package com.kioschool.kioschoolapi.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_member")
class WorkspaceMember(
    @ManyToOne(cascade = [CascadeType.ALL])
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JsonIgnore
    val user: User
) : BaseEntity()