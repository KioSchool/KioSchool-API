package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.cache.annotation.CacheEvict

class SuperAdminDashboardFacadeCacheEvictTest : DescribeSpec({

    describe("setFestivalCalendarExclusion") {
        val method = SuperAdminDashboardFacade::class.java.getDeclaredMethod(
            "setFestivalCalendarExclusion",
            Long::class.java,
            Boolean::class.java
        )
        val evict = method.getAnnotation(CacheEvict::class.java)

        it("축제 달력 캐시를 비운다") {
            evict.cacheNames.toList() shouldContain "${CacheNames.FESTIVAL_CALENDAR}#1h"
        }

        it("운영진 인사이트 카드 캐시도 함께 비운다") {
            evict.cacheNames.toList() shouldContain CacheNames.INSIGHT_CARD
        }

        it("키를 특정할 수 없으므로 전체 엔트리를 비운다") {
            evict.allEntries shouldBe true
        }
    }
})
