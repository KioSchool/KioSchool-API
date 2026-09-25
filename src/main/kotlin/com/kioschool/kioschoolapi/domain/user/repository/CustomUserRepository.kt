package com.kioschool.kioschoolapi.domain.user.repository

import com.kioschool.kioschoolapi.domain.account.entity.QAccount
import com.kioschool.kioschoolapi.domain.user.entity.QUser
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.UserAccountFilter
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CustomUserRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        keyword: String?,
        accountFilter: UserAccountFilter?,
        pageable: Pageable
    ): Page<User> {
        val user = QUser.user
        val account = QAccount.account
        val conditions = listOfNotNull(
            keywordCondition(user, keyword),
            accountCondition(account, accountFilter)
        ).toTypedArray()

        val totalCount = queryFactory.select(user.count())
            .from(user)
            .leftJoin(user.account, account)
            .where(*conditions)
            .fetchOne() ?: 0L

        val users = queryFactory.selectFrom(user)
            .leftJoin(user.account, account).fetchJoin()
            .where(*conditions)
            .orderBy(user.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(users, pageable, totalCount)
    }

    private fun keywordCondition(user: QUser, keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) return null
        val trimmed = keyword.trim()
        return user.name.containsIgnoreCase(trimmed)
            .or(user.email.containsIgnoreCase(trimmed))
            .or(user.loginId.containsIgnoreCase(trimmed))
    }

    private fun accountCondition(account: QAccount, accountFilter: UserAccountFilter?): BooleanExpression? {
        return when (accountFilter) {
            null -> null
            UserAccountFilter.CONNECTED -> account.id.isNotNull
            UserAccountFilter.NOT_CONNECTED -> account.id.isNull
            UserAccountFilter.TOSS_NOT_CONNECTED -> account.id.isNotNull.and(account.tossAccountUrl.isNull)
        }
    }
}
