package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.springframework.stereotype.Component

@Component
class InitProductStatus(
    private val productRepository: ProductRepository
) : Runnable {
    override fun run() {
        val products = productRepository.findAll()
        products.forEach {
            it.status = ProductStatus.SELLING
        }
        productRepository.saveAll(products)
    }
}