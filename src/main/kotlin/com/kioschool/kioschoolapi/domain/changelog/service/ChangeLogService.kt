package com.kioschool.kioschoolapi.domain.changelog.service

import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeLog
import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeTargetType
import com.kioschool.kioschoolapi.domain.changelog.repository.ChangeLogRepository
import org.springframework.stereotype.Service

@Service
class ChangeLogService(
    private val changeLogRepository: ChangeLogRepository
) {
    // 여러 기기의 중복 클릭이나 값을 그대로 둔 저장은 변경이 아니므로 남기지 않는다.
    fun record(
        workspaceId: Long,
        targetType: ChangeTargetType,
        targetId: Long,
        field: String,
        oldValue: String?,
        newValue: String?
    ) {
        if (oldValue == newValue) return

        changeLogRepository.save(
            ChangeLog(
                workspaceId = workspaceId,
                targetType = targetType,
                targetId = targetId,
                field = field,
                oldValue = oldValue,
                newValue = newValue
            )
        )
    }
}
