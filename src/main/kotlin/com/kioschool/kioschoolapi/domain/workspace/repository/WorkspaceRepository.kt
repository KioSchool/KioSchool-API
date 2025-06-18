package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, Long> {
    fun findByNameContains(name: String, pageable: Pageable): Page<Workspace>
}