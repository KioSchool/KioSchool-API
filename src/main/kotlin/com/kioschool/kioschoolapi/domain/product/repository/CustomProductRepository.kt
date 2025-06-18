package com.kioschool.kioschoolapi.domain.product.repository

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.QProduct
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class CustomProductRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        workspaceId: Long,
        productCategoryId: Long?
    ): List<Product> {
        val product = QProduct.product
        val query = queryFactory.selectFrom(product)
            .where(product.workspace.id.eq(workspaceId))
            .orderBy(product.id.asc())

        if (productCategoryId != null) query.where(product.productCategory.id.eq(productCategoryId))

        return query.fetch()
    }
}