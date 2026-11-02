package com.kioschool.kioschoolapi.domain.user.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user", schema = "PUBLIC")
class User(
    @JsonIgnore
    var loginId: String,
    @JsonIgnore
    var loginPassword: String,
    @LogMasked
    var name: String,
    var email: String?,
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
) : BaseEntity() {
    // 탈퇴 시각. 탈퇴 계정은 지우지 않고 개인정보만 익명값으로 바꾼다(주점·주문 기록이 이 계정을 가리킨다).
    var withdrawnAt: LocalDateTime? = null

    // 여러 곳 발송의 수신 거부. 토큰이 응답에 실리면 다른 사람이 대신 거부할 수 있으므로 내보내지 않는다.
    @JsonIgnore
    var mailUnsubscribedAt: LocalDateTime? = null

    @JsonIgnore
    var mailUnsubscribeToken: String? = null

    @JsonIgnore
    fun getWorkspaces() = members.map { it.workspace }.sortedBy { it.id }
}