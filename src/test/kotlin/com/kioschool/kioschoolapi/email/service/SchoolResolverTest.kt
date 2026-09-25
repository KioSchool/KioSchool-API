package com.kioschool.kioschoolapi.email.service

import com.kioschool.kioschoolapi.domain.email.service.SchoolResolver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SchoolResolverTest : DescribeSpec({
    val sut = SchoolResolver(
        mapOf(
            "konkuk.ac.kr" to "건국대학교",
            "kku.ac.kr" to "건국대학교",
            "khu.ac.kr" to "경희대학교"
        )
    )

    describe("schoolOf") {
        it("등록된 도메인은 학교 이름을, 아니면 도메인을, 이메일이 없으면 묶음 이름을 돌려준다") {
            sut.schoolOf("a@konkuk.ac.kr") shouldBe "건국대학교"
            sut.schoolOf("a@unknown.ac.kr") shouldBe "unknown.ac.kr"
            sut.schoolOf(null) shouldBe SchoolResolver.NO_EMAIL_SCHOOL_NAME
        }
    }

    describe("domainsMatching") {
        it("학교 이름 일부로 해당 학교의 모든 도메인을 찾는다") {
            sut.domainsMatching(" 건국 ") shouldBe setOf("konkuk.ac.kr", "kku.ac.kr")
        }

        it("검색어가 비어 있거나 맞는 학교가 없으면 빈 집합을 돌려준다") {
            sut.domainsMatching(null) shouldBe emptySet()
            sut.domainsMatching("  ") shouldBe emptySet()
            sut.domainsMatching("서울대") shouldBe emptySet()
        }
    }
})
