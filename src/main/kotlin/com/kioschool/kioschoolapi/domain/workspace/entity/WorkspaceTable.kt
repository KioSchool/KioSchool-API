package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "workspace_table")
class WorkspaceTable(
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val workspace: Workspace,
    val tableNumber: Int,
    @Column(unique = true)
    val tableHash: String,
    @OneToOne(fetch = FetchType.LAZY)
    var orderSession: OrderSession? = null,
    // 명시적 컬럼명 필수: Hibernate의 CamelCaseToUnderscoresNamingStrategy는 이름 끝의
    // 대문자 한 글자에 언더스코어를 넣지 않아, 암시적 매핑은 positionx/positiony가 된다.
    @Column(name = "position_x")
    var positionX: Int? = null,
    @Column(name = "position_y")
    var positionY: Int? = null,
) : BaseEntity()