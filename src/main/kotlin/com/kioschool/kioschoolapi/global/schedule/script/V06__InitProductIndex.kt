package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.springframework.stereotype.Component

@Component
class V06__InitProductIndex(
    private val productRepository: ProductRepository
) : Runnable {
    override fun run() {
        val products = productRepository.findAll()

        val productCategoryGroups = products.groupBy { it.productCategory?.id }

        productCategoryGroups.forEach { (_, groupProducts) ->
            // 동일 카테고리 내에서 id 순으로 정렬하여 index 부여
            val sortedProducts = groupProducts.sortedBy { it.id }
            sortedProducts.forEachIndexed { index, product ->
                product.index = index
            }
        }

        productRepository.saveAll(products)
    }
}
