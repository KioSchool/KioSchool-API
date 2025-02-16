package com.kioschool.kioschoolapi.workspace.repository

import com.kioschool.kioschoolapi.workspace.entity.WorkspaceImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceImageRepository : JpaRepository<WorkspaceImage, Long> {
    fun deleteAllByIdIn(deletedImageIds: List<Long>)
}