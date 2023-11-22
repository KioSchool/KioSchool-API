package com.kioschool.kioschoolapi.workspace.repository

import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceMemberRepository : JpaRepository<WorkspaceMember, Long>