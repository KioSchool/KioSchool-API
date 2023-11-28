package com.kioschool.kioschoolapi.user.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "user", schema = "PUBLIC")
class User(
    @JsonIgnore
    var loginId: String,
    @JsonIgnore
    var loginPassword: String,
    var name: String,
    var email: String,
    var role: UserRole,
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    var members: MutableList<WorkspaceMember>
) : BaseEntity() {
    @JsonIgnore
    fun getWorkspaces() = members.map { it.workspace }
}