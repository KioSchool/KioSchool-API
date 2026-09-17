package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProductItem
import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MenuDiversityMetricTest : DescribeSpec({
    val sut = MenuDiversityMetric()

    fun mockStat(soldProducts: Int, registeredProducts: Int): DailyOrderStatistic {
        val items = (1..soldProducts).map { PopularProductItem(it.toLong(), "Product$it", 1.0) }
        val workspace = mockk<Workspace>()
        every { workspace.products } returns MutableList(registeredProducts) { mockk<Product>() }
        val stat = mockk<DailyOrderStatistic>()
        every { stat.popularProducts } returns PopularProducts(items, items, items)
        every { stat.workspace } returns workspace
        return stat
    }

    describe("evaluate") {
        it("판매된 상품 수를 한 번만 세서 등록 메뉴 대비 비율을 구한다") {
            val self = mockStat(soldProducts = 12, registeredProducts = 20)
            val peers = listOf(self, mockStat(soldProducts = 5, registeredProducts = 20))
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            val result = sut.evaluate(self, cohort)

            result!!.absoluteValue shouldBe 0.6
            result.percentile shouldBe 50.0
        }

        it("판매 상품 수가 등록 메뉴 수보다 많으면 100%로 제한한다") {
            val self = mockStat(soldProducts = 8, registeredProducts = 6)
            val peers = listOf(self, mockStat(soldProducts = 1, registeredProducts = 6))
            val cohort = CohortContext(bucket = TableCountBucket.S, peers = peers)

            sut.evaluate(self, cohort)!!.absoluteValue shouldBe 1.0
        }
    }
})
