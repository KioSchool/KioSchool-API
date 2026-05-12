package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, Long> {
    fun findByNameContains(name: String, pageable: Pageable): Page<Workspace>

    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long

    @Query("SELECT COUNT(DISTINCT w.id) FROM Workspace w WHERE w.isOnboarding = false AND SIZE(w.members) > 0")
    fun countOnboardingCompletedWithMembers(): Long

    @Query("SELECT w FROM Workspace w ORDER BY w.createdAt DESC")
    fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Workspace>

    @Query("SELECT COUNT(DISTINCT w.id) FROM Workspace w WHERE SIZE(w.members) > 0")
    fun countByMembersNotEmpty(): Long

    @Query("SELECT u.createdAt, MIN(w.createdAt) FROM Workspace w JOIN w.owner u GROUP BY u.id, u.createdAt")
    fun findUserRegistrationAndFirstWorkspaceCreatedAt(): List<Array<Any>>
}