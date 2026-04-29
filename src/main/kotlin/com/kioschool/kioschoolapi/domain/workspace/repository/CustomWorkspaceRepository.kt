package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.workspace.entity.QWorkspace
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CustomWorkspaceRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        name: String?,
        pageable: Pageable
    ): Page<Workspace> {
        val workspace = QWorkspace.workspace
        val query = queryFactory.selectFrom(workspace)

        val countQuery = queryFactory.select(workspace.count()).from(workspace)

        if (!name.isNullOrBlank()) {
            query.where(workspace.name.contains(name))
            countQuery.where(workspace.name.contains(name))
        }

        val totalCount = countQuery.fetchOne() ?: 0L

        val workspaces = query
            .orderBy(workspace.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(workspaces, pageable, totalCount)
    }
}