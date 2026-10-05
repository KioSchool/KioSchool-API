package com.kioschool.kioschoolapi.domain.changelog.repository

import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChangeLogRepository : JpaRepository<ChangeLog, Long> {
    // 파생 delete는 행을 전부 읽어 한 건씩 지우므로, 이력이 많이 쌓인 주점도 한 번에 지우도록 벌크로 둔다.
    @Modifying
    @Query("delete from ChangeLog c where c.workspaceId = :workspaceId")
    fun deleteByWorkspaceId(@Param("workspaceId") workspaceId: Long)
}
