package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FocalPointDtoTest : DescribeSpec({
    describe("isInRange") {
        it("should accept values inside 0..100") {
            FocalPointDto(0, 0).isInRange() shouldBe true
            FocalPointDto(50, 50).isInRange() shouldBe true
            FocalPointDto(100, 100).isInRange() shouldBe true
        }

        it("should reject values below 0") {
            FocalPointDto(-1, 50).isInRange() shouldBe false
            FocalPointDto(50, -1).isInRange() shouldBe false
        }

        it("should reject values above 100") {
            FocalPointDto(101, 50).isInRange() shouldBe false
            FocalPointDto(50, 101).isInRange() shouldBe false
        }
    }

    describe("CENTER") {
        it("should be the midpoint, matching the previous center-crop behaviour") {
            FocalPointDto.CENTER shouldBe FocalPointDto(50, 50)
        }
    }
})
