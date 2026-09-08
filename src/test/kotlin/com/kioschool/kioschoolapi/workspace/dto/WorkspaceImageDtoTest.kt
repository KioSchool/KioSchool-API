package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageDto
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WorkspaceImageDtoTest : DescribeSpec({
    describe("of") {
        it("should expose the stored focal point") {
            val image = SampleEntity.workspaceImage1.apply {
                focalX = 30
                focalY = 12
            }

            WorkspaceImageDto.of(image).focalPoint shouldBe FocalPointDto(30, 12)
        }

        it("should expose the center focal point for an image that was never adjusted") {
            val image = SampleEntity.workspaceImage2.apply {
                focalX = FocalPointDto.CENTER_VALUE
                focalY = FocalPointDto.CENTER_VALUE
            }

            WorkspaceImageDto.of(image).focalPoint shouldBe FocalPointDto.CENTER
        }
    }
})
