package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.product.entity.QProduct
import com.kioschool.kioschoolapi.domain.product.entity.QProductCategory
import com.kioschool.kioschoolapi.domain.workspace.entity.QWorkspace
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CustomWorkspaceRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        name: String?,
        pageable: Pageable,
        updatedAfter: LocalDateTime? = null
    ): Page<Workspace> {
        val workspace = QWorkspace.workspace
        val product = QProduct.product
        val productCategory = QProductCategory.productCategory

        val query = if (updatedAfter != null) {
            queryFactory.selectDistinct(workspace)
                .from(workspace)
                .leftJoin(workspace.products, product)
                .leftJoin(workspace.productCategories, productCategory)
        } else {
            queryFactory.selectFrom(workspace)
        }

        val countQuery = if (updatedAfter != null) {
            queryFactory.select(workspace.countDistinct())
                .from(workspace)
                .leftJoin(workspace.products, product)
                .leftJoin(workspace.productCategories, productCategory)
        } else {
            queryFactory.select(workspace.count()).from(workspace)
        }

        if (!name.isNullOrBlank()) {
            query.where(workspace.name.contains(name))
            countQuery.where(workspace.name.contains(name))
        }

        if (updatedAfter != null) {
            val dateCondition = workspace.updatedAt.goe(updatedAfter)
                .or(workspace.createdAt.goe(updatedAfter))
                .or(product.updatedAt.goe(updatedAfter))
                .or(product.createdAt.goe(updatedAfter))
                .or(productCategory.updatedAt.goe(updatedAfter))
                .or(productCategory.createdAt.goe(updatedAfter))
            query.where(dateCondition)
            countQuery.where(dateCondition)
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