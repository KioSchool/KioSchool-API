package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceImageRepository : JpaRepository<WorkspaceImage, Long> {
    fun deleteAllByIdIn(deletedImageIds: List<Long>)
}