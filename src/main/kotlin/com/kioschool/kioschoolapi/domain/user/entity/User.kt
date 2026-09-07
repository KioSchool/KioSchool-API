package com.kioschool.kioschoolapi.domain.user.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import jakarta.persistence.*

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
    var accountUrl: String? = null,
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var account: Account? = null,
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var members: MutableList<WorkspaceMember>,
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var invitations: MutableList<WorkspaceInvitation> = mutableListOf(),
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    var acquisitionChannel: AcquisitionChannel? = null,
    @Column(length = 100)
    var acquisitionChannelEtc: String? = null,
    @Column(length = 500)
    var acquisitionContext: String? = null,
) : BaseEntity() {
    @JsonIgnore
    fun getWorkspaces() = members.map { it.workspace }.sortedBy { it.id }
}