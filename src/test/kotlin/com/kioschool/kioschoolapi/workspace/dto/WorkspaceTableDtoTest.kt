package com.kioschool.kioschoolapi.workspace.dto

import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceTableDto
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WorkspaceTableDtoTest : DescribeSpec({
    describe("position mapping") {
        it("should map position to null when the table is not placed") {
            val table = WorkspaceTable(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                tableHash = "testHash"
            )

            WorkspaceTableDto.of(table).position shouldBe null
        }

        it("should map position to a TablePositionDto when the table is placed") {
            val table = WorkspaceTable(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                tableHash = "testHash",
                positionX = 3,
                positionY = 2
            )

            WorkspaceTableDto.of(table).position shouldBe TablePositionDto(3, 2)
        }

        it("should map position to null when only positionX is set") {
            val table = WorkspaceTable(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                tableHash = "testHash",
                positionX = 3
            )

            WorkspaceTableDto.of(table).position shouldBe null
        }

        it("should map position to null when only positionY is set") {
            val table = WorkspaceTable(
                workspace = SampleEntity.workspace,
                tableNumber = 1,
                tableHash = "testHash",
                positionY = 2
            )

            WorkspaceTableDto.of(table).position shouldBe null
        }
    }
})
