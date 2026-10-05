package com.kioschool.kioschoolapi.domain.changelog.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

// 운영 데이터의 값 변경 이력. 분석·문의 대응용으로 SQL에서만 조회한다.
// 상품 하드 삭제·주점 강제 삭제를 막지 않도록 대상에 FK를 걸지 않는다.
@Entity
@Table(
    name = "change_log",
    indexes = [
        Index(name = "idx_change_log_workspace_created", columnList = "workspace_id, created_at"),
        Index(name = "idx_change_log_target", columnList = "target_type, target_id")
    ]
)
class ChangeLog(
    // @Index의 columnList는 논리 이름으로 찾으므로, 인덱스에 쓰는 컬럼은 이름을 명시한다.
    @Column(name = "workspace_id")
    val workspaceId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    val targetType: ChangeTargetType,
    @Column(name = "target_id")
    val targetId: Long,
    val field: String,
    @Column(columnDefinition = "TEXT")
    val oldValue: String?,
    @Column(columnDefinition = "TEXT")
    val newValue: String?
) : BaseEntity()
