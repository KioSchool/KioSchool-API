package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_image")
class WorkspaceImage(
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    val workspace: Workspace,
    var url: String,
    // 두 필드 모두 명시적 컬럼명 필수: Hibernate의 CamelCaseToUnderscoresNamingStrategy는 이름 끝의
    // 대문자 한 글자에 언더스코어를 넣지 않아, 암시적 매핑은 focalx/focaly가 된다.
    @Column(name = "focal_x")
    var focalX: Int = FocalPointDto.CENTER_VALUE,
    @Column(name = "focal_y")
    var focalY: Int = FocalPointDto.CENTER_VALUE,
) : BaseEntity()
