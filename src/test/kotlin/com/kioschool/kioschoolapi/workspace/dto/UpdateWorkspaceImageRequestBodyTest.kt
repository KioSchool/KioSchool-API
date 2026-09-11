package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageSlot
import com.kioschool.kioschoolapi.domain.workspace.dto.request.UpdateWorkspaceImageRequestBody
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import jakarta.validation.Validation
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile

class UpdateWorkspaceImageRequestBodyTest : DescribeSpec({
    val fileA = mockk<MultipartFile>()
    val fileB = mockk<MultipartFile>()

    describe("field validation") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        it("should reject imageIds with a length other than 3") {
            val violations = validator.validate(UpdateWorkspaceImageRequestBody(1L, listOf(1L, null), null))

            violations.map { it.propertyPath.toString() } shouldBe listOf("imageIds")
        }

        it("should reject focalPoints with a length other than 3") {
            val violations = validator.validate(
                UpdateWorkspaceImageRequestBody(1L, listOf(1L, null, null), listOf(FocalPointDto(0, 0))),
            )

            violations.map { it.propertyPath.toString() } shouldBe listOf("focalPoints")
        }

        it("should accept a well-formed request") {
            validator.validate(UpdateWorkspaceImageRequestBody(1L, listOf(1L, null, null), null)).shouldBeEmpty()
        }
    }

    describe("toSlots") {
        it("should map existing images to Existing slots with their focal points") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(10L, 11L, null),
                focalPoints = listOf(FocalPointDto(20, 30), FocalPointDto(40, 50), null),
            )

            body.toSlots(emptyList()) shouldBe listOf(
                WorkspaceImageSlot.Existing(10L, FocalPointDto(20, 30)),
                WorkspaceImageSlot.Existing(11L, FocalPointDto(40, 50)),
            )
        }

        it("should map uploaded files to the empty slots in order") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(null, null, null),
                focalPoints = listOf(FocalPointDto(10, 10), FocalPointDto(20, 20), null),
            )

            body.toSlots(listOf(fileA, fileB)) shouldBe listOf(
                WorkspaceImageSlot.New(fileA, FocalPointDto(10, 10)),
                WorkspaceImageSlot.New(fileB, FocalPointDto(20, 20)),
            )
        }

        it("should map a mix of existing images and uploaded files by slot index") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(10L, null, null),
                focalPoints = listOf(FocalPointDto(0, 0), FocalPointDto(60, 70), null),
            )

            body.toSlots(listOf(fileA)) shouldBe listOf(
                WorkspaceImageSlot.Existing(10L, FocalPointDto(0, 0)),
                WorkspaceImageSlot.New(fileA, FocalPointDto(60, 70)),
            )
        }

        it("should keep focal points null when the client did not send any") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(10L, null, null),
                focalPoints = null,
            )

            body.toSlots(listOf(fileA)) shouldBe listOf(
                WorkspaceImageSlot.Existing(10L, null),
                WorkspaceImageSlot.New(fileA, null),
            )
        }

        it("should reject more files than empty slots") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(10L, 11L, null),
                focalPoints = null,
            )

            val exception = assertThrows<CustomException> { body.toSlots(listOf(fileA, fileB)) }
            exception.errorCode shouldBe ErrorCode.WORKSPACE_IMAGE_SLOT_MISMATCH
        }

        it("should reject a focal point outside 0..100") {
            val body = UpdateWorkspaceImageRequestBody(
                workspaceId = 1L,
                imageIds = listOf(10L, null, null),
                focalPoints = listOf(FocalPointDto(101, 50), null, null),
            )

            val exception = assertThrows<CustomException> { body.toSlots(emptyList()) }
            exception.errorCode shouldBe ErrorCode.INVALID_IMAGE_FOCAL_POINT
        }

        it("should produce no slots for an empty workspace") {
            UpdateWorkspaceImageRequestBody(1L, listOf(null, null, null), null)
                .toSlots(emptyList())
                .shouldBeEmpty()
        }
    }
})
