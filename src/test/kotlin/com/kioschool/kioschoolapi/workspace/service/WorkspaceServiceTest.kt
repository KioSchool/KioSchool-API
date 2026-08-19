package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionUpdateDto
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.CustomWorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.multipart.MultipartFile
import java.util.*
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceMemberRepository

class WorkspaceServiceTest : DescribeSpec({
    val repository = mockk<WorkspaceRepository>()
    val customWorkspaceRepository = mockk<CustomWorkspaceRepository>()
    val workspaceTableRepository = mockk<WorkspaceTableRepository>()
    val workspaceMemberRepository = mockk<WorkspaceMemberRepository>()
    val userService = mockk<UserService>()
    val s3Service = mockk<S3Service>()

    val sut = WorkspaceService(
        "test-path",
        repository,
        customWorkspaceRepository,
        workspaceTableRepository,
        workspaceMemberRepository,
        userService,
        s3Service
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(workspaceTableRepository)
        mockkObject(workspaceMemberRepository)
        mockkObject(userService)
        SampleEntity.workspace.tableCount = 1
    }

    afterTest {
        clearAllMocks()
    }

    describe("getAllWorkspaces") {
        it("should call findByNameContains when name is not null") {
            val name = "name"
            val page = 0
            val size = 10

            // Mock
            every {
                customWorkspaceRepository.findAllByCondition(
                    name,
                    PageRequest.of(page, size)
                )
            } returns PageImpl(listOf())

            // Act
            sut.getAllWorkspaces(name, page, size)

            // Assert
            verify { customWorkspaceRepository.findAllByCondition(name, PageRequest.of(page, size)) }
            verify(exactly = 0) { repository.findAll(PageRequest.of(page, size)) }
        }

        it("should call findAllByCondition with null name when name is null or blank") {
            val name = null
            val page = 0
            val size = 10

            // Mock
            every {
                customWorkspaceRepository.findAllByCondition(
                    name,
                    PageRequest.of(page, size)
                )
            } returns PageImpl(listOf())

            // Act
            sut.getAllWorkspaces(name, page, size)

            // Assert
            verify { customWorkspaceRepository.findAllByCondition(name, PageRequest.of(page, size)) }
            verify(exactly = 0) { repository.findAll(PageRequest.of(page, size)) }
        }
    }

    describe("checkCanCreateWorkspace") {
        it("should throw CustomException(NO_PERMISSION_TO_CREATE_WORKSPACE) when user accountUrl is null") {
            val user = SampleEntity.user.apply { account = null }

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkCanCreateWorkspace(user)
            }
            ex.errorCode shouldBe ErrorCode.NO_PERMISSION_TO_CREATE_WORKSPACE
        }

        it("should not throw CustomException(NO_PERMISSION_TO_CREATE_WORKSPACE) when user accountUrl is not null") {
            val user = SampleEntity.user.apply { account = SampleEntity.account }

            // Act & Assert
            sut.checkCanCreateWorkspace(user)
        }
    }

    describe("saveNewWorkspace") {
        it("should save new workspace") {
            val user = SampleEntity.user
            val name = "name"
            val description = "description"

            // Mock
            every {
                repository.save(any())
            } returns SampleEntity.workspace

            // Act
            sut.saveNewWorkspace(user, name, description) shouldBe SampleEntity.workspace

            // Assert
            verify { repository.save(any()) }
        }
    }

    describe("checkCanJoinWorkspace") {
        it("should throw CustomException(NO_PERMISSION_TO_JOIN_WORKSPACE) when user is not invited") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            workspace.invitations.clear()

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkCanJoinWorkspace(user, workspace)
            }
            ex.errorCode shouldBe ErrorCode.NO_PERMISSION_TO_JOIN_WORKSPACE
        }

        it("should not throw CustomException(NO_PERMISSION_TO_JOIN_WORKSPACE) when user is invited") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            workspace.invitations.add(SampleEntity.workspaceInvitation)

            // Act & Assert
            sut.checkCanJoinWorkspace(user, workspace)
        }
    }

    describe("addUserToWorkspace") {
        it("should add user to workspace") {
            val workspace = SampleEntity.workspace
            val user = SampleEntity.user

            // Mock
            every {
                repository.save(workspace)
            } returns workspace

            // Act
            sut.addUserToWorkspace(workspace, user)

            // Assert
            verify { repository.save(workspace) }
        }
    }

    describe("getWorkspace") {
        it("should get workspace") {
            val workspaceId = 1L
            val workspace = SampleEntity.workspace

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            // Act
            sut.getWorkspace(workspaceId) shouldBe workspace

            // Assert
            verify { repository.findById(workspaceId) }
        }
    }

    describe("isAccessible") {
        it("should return true when user is a member of workspace") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user

            // Mock
            every {
                userService.getUser(username)
            } returns user

            every {
                workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username)
            } returns true

            // Act
            sut.isAccessible(username, workspaceId) shouldBe true

            // Assert
            verify { userService.getUser(username) }
            verify { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) }
        }

        it("should return true when user is a super admin") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user.apply { role = UserRole.SUPER_ADMIN }

            // Mock
            every {
                userService.getUser(username)
            } returns user

            // Act
            sut.isAccessible(username, workspaceId) shouldBe true

            // Assert
            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(any(), any()) }
        }

        it("should return false when user is not a member of workspace and not a super admin") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user.apply { role = UserRole.ADMIN }

            // Mock
            every {
                userService.getUser(username)
            } returns user

            every {
                workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username)
            } returns false

            // Act
            sut.isAccessible(username, workspaceId) shouldBe false

            // Assert
            verify { userService.getUser(username) }
            verify { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) }
        }
    }

    describe("checkAccessible") {
        it("should throw CustomException(WORKSPACE_INACCESSIBLE) when user is not a member of workspace and not a super admin") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user.apply { role = UserRole.ADMIN }

            every { userService.getUser(username) } returns user
            every { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) } returns false

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkAccessible(username, workspaceId)
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE

            // Assert
            verify { userService.getUser(username) }
            verify { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) }
        }

        it("should not throw CustomException(WORKSPACE_INACCESSIBLE) when user is a member of workspace") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user

            every { userService.getUser(username) } returns user
            every { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) } returns true

            // Act & Assert
            sut.checkAccessible(username, workspaceId)

            // Assert
            verify { userService.getUser(username) }
            verify { workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username) }
        }

        it("should not throw CustomException(WORKSPACE_INACCESSIBLE) when user is a super admin") {
            val username = "test"
            val workspaceId = 1L
            val user = SampleEntity.user.apply { role = UserRole.SUPER_ADMIN }

            every { userService.getUser(username) } returns user

            // Act & Assert
            sut.checkAccessible(username, workspaceId)

            // Assert
            verify { userService.getUser(username) }
        }
    }

    describe("checkCanInviteWorkspace") {
        it("should throw CustomException(NO_PERMISSION_TO_INVITE) when user is not owner of workspace") {
            val user = SampleEntity.user
            val otherUser = SampleEntity.otherUser
            val inaccessibleWorkspace = SampleEntity.workspace(otherUser)

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkCanInviteWorkspace(user, inaccessibleWorkspace)
            }
            ex.errorCode shouldBe ErrorCode.NO_PERMISSION_TO_INVITE
        }

        it("should not throw CustomException(NO_PERMISSION_TO_INVITE) when user is owner of workspace") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace(user)

            // Act & Assert
            sut.checkCanInviteWorkspace(user, workspace)
        }
    }

    describe("inviteUserToWorkspace") {
        it("should invite user to workspace") {
            val workspace = SampleEntity.workspace
            val user = SampleEntity.user

            // Mock
            every {
                repository.save(workspace)
            } returns workspace

            // Act
            sut.inviteUserToWorkspace(workspace, user)

            // Assert
            verify { repository.save(workspace) }
        }
    }

    describe("removeUserFromWorkspace") {
        it("should remove user from workspace") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            workspace.members.add(SampleEntity.workspaceMember(user, workspace))

            // Mock
            every {
                repository.save(workspace)
            } returns workspace

            // Act
            val result = sut.removeUserFromWorkspace(workspace, user)
            result.members.size shouldBe 0

            // Assert
            verify { repository.save(workspace) }
        }

        it("should not remove user from workspace when user is not a member") {
            val user = SampleEntity.user
            val otherUser = SampleEntity.otherUser
            val workspace = SampleEntity.workspace.apply { members.clear() }
            workspace.members.add(SampleEntity.workspaceMember(otherUser, workspace))

            // Mock
            every {
                repository.save(workspace)
            } returns workspace

            // Act
            val result = sut.removeUserFromWorkspace(workspace, user)
            result.members.size shouldBe 1

            // Assert
            verify { repository.save(workspace) }
        }
    }

    describe("updateTableCount") {
        it("should update table count") {
            val workspace = SampleEntity.workspace
            val tableCount = 10

            // Mock
            every {
                repository.save(workspace)
            } returns workspace

            // Act
            sut.updateTableCount(workspace, tableCount)

            workspace.tableCount shouldBe tableCount

            // Assert
            verify { repository.save(workspace) }
        }
    }

    describe("saveWorkspace") {
        it("should save workspace") {
            val workspace = SampleEntity.workspace

            every {
                repository.save(workspace)
            } returns workspace

            val result = sut.saveWorkspace(workspace)

            assert(result == workspace)

            verify { repository.save(workspace) }
        }
    }

    describe("deleteWorkspaceImages") {
        it("should delete workspace images") {
            val workspace = SampleEntity.workspace.apply {
                images.addAll(SampleEntity.workspaceImages)
            }
            val deletedImages = SampleEntity.workspaceImages

            every {
                s3Service.deleteFile(any())
            } just Runs

            sut.deleteWorkspaceImages(workspace, deletedImages)
            assert(workspace.images.isEmpty())

            verify(exactly = 3) { s3Service.deleteFile(any()) }
        }
    }

    describe("saveWorkspaceImages") {
        it("should save workspace images") {
            val workspace = SampleEntity.workspace
            val newImageFiles = listOf(mockk<MultipartFile>(), mockk<MultipartFile>())
            newImageFiles.forEach { every { it.inputStream } returns java.io.ByteArrayInputStream(ByteArray(0)) }

            every {
                s3Service.uploadResizedWebpImage(any(), any<String>())
            } returns "imageUrl"
            every {
                repository.save(workspace)
            } returns workspace

            val result = sut.saveWorkspaceImages(workspace, newImageFiles)

            assert(result == workspace)

            verify(exactly = 2) { s3Service.uploadResizedWebpImage(any(), any<String>()) }
            verify { repository.save(workspace) }
        }
    }

    describe("checkCanAccessWorkspace") {
        it("should not throw exception when user can access workspace") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            workspace.members.clear()
            workspace.members.add(SampleEntity.workspaceMember(user, workspace))

            every { repository.findById(workspace.id) } returns Optional.of(workspace)
            every { userService.getUser(user.loginId) } returns user

            sut.checkCanAccessWorkspace(user, workspace)
        }
    }

    describe("getWorkspaceTable") {
        it("should get workspace table") {
            val workspace = SampleEntity.workspace.apply { tableCount = 1 }
            val tableNumber = 1

            every {
                workspaceTableRepository.findByTableNumberAndWorkspace(
                    tableNumber,
                    workspace
                )
            } returns SampleEntity.workspaceTable

            sut.getWorkspaceTable(workspace, tableNumber) shouldBe SampleEntity.workspaceTable

            verify {
                workspaceTableRepository.findByTableNumberAndWorkspace(
                    tableNumber,
                    workspace
                )
            }
        }
    }

    describe("getAllWorkspaceTables") {
        it("should return only tables within tableCount") {
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2)
            )

            every {
                workspaceTableRepository
                    .findAllByWorkspaceAndTableNumberLessThanEqualOrderByTableNumber(workspace, 2)
            } returns tables

            sut.getAllWorkspaceTables(workspace) shouldBe tables

            verify {
                workspaceTableRepository
                    .findAllByWorkspaceAndTableNumberLessThanEqualOrderByTableNumber(workspace, 2)
            }
        }
    }

    describe("out-of-range table access") {
        it("should reject a tableHash whose table number exceeds tableCount") {
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }
            val table = SampleEntity.workspaceTableWithId(5L, tableNumber = 5)

            every {
                workspaceTableRepository.findByTableHashAndWorkspace("testHash5", workspace)
            } returns Optional.of(table)

            val ex = shouldThrow<CustomException> {
                sut.getWorkspaceTableByHash(workspace, "testHash5")
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_TABLE_NOT_FOUND
        }

        it("should return a tableHash whose table number is within tableCount") {
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }
            val table = SampleEntity.workspaceTableWithId(1L, tableNumber = 1)

            every {
                workspaceTableRepository.findByTableHashAndWorkspace("testHash1", workspace)
            } returns Optional.of(table)

            sut.getWorkspaceTableByHash(workspace, "testHash1") shouldBe table
        }

        it("should reject a tableNumber that exceeds tableCount without hitting the repository") {
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }

            val ex = shouldThrow<CustomException> { sut.getWorkspaceTable(workspace, 5) }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_TABLE_NOT_FOUND

            verify(exactly = 0) {
                workspaceTableRepository.findByTableNumberAndWorkspace(any(), any())
            }
        }

        it("should reject a tableNumber below 1") {
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }

            val ex = shouldThrow<CustomException> { sut.getWorkspaceTable(workspace, 0) }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_TABLE_NOT_FOUND
        }
    }

    describe("updateWorkspaceTables") {
        it("should add tables when table count is increased") {
            val workspace = SampleEntity.workspace.apply { tableCount = 5 }

            every { workspaceTableRepository.countAllByWorkspace(workspace) } returns 3
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns listOf()

            sut.updateWorkspaceTables(workspace)

            verify { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should clear positions of out-of-range tables instead of deleting them when table count is decreased") {
            val workspace = SampleEntity.workspace.apply { tableCount = 1 }
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 1, positionY = 0),
                SampleEntity.workspaceTableWithId(3L, tableNumber = 3, positionX = 2, positionY = 0)
            )

            every { workspaceTableRepository.countAllByWorkspace(workspace) } returns 3
            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns listOf()

            sut.updateWorkspaceTables(workspace)

            // 범위 안 테이블(1번)은 그대로
            tables[0].positionX shouldBe 0
            tables[0].positionY shouldBe 0

            // 범위 밖 테이블(2, 3번)은 position만 비워짐
            tables[1].positionX shouldBe null
            tables[1].positionY shouldBe null
            tables[2].positionX shouldBe null
            tables[2].positionY shouldBe null

            verify {
                workspaceTableRepository.saveAll(
                    match<Iterable<WorkspaceTable>> {
                        it.map(WorkspaceTable::tableNumber).toSet() == setOf(2, 3)
                    }
                )
            }
            verify(exactly = 0) { workspaceTableRepository.deleteAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should preserve tableHash across a decrease and a matching increase") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2),
                SampleEntity.workspaceTableWithId(3L, tableNumber = 3)
            )
            val hashesBefore = tables.map { it.tableHash }

            every { workspaceTableRepository.countAllByWorkspace(workspace) } returns 3
            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns listOf()

            // 3 -> 1 (감소): row는 남고 좌표만 비워진다
            workspace.tableCount = 1
            sut.updateWorkspaceTables(workspace)

            // 1 -> 3 (복귀): 신규 생성이 없어야 한다
            workspace.tableCount = 3
            sut.updateWorkspaceTables(workspace)

            // 인쇄된 QR이 살아있다는 것이 이 기능의 존재 이유다
            tables.map { it.tableHash } shouldBe hashesBefore
            verify(exactly = 0) { workspaceTableRepository.deleteAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should do nothing when table count returns to the row high-water mark") {
            val workspace = SampleEntity.workspace.apply { tableCount = 3 }

            every { workspaceTableRepository.countAllByWorkspace(workspace) } returns 3

            sut.updateWorkspaceTables(workspace)

            // hash 재발급이 없어야 인쇄된 QR이 살아있다
            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
            verify(exactly = 0) { workspaceTableRepository.deleteAll(any<Iterable<WorkspaceTable>>()) }
        }
    }

    describe("updateTablePosition") {
        it("should save the given position") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)
            every {
                workspaceTableRepository.existsByWorkspaceAndPositionXAndPositionYAndIdNot(workspace, 3, 2, 1L)
            } returns false
            every { workspaceTableRepository.save(table) } returns table

            val result = sut.updateTablePosition(workspace, 1L, 3, 2)

            result.positionX shouldBe 3
            result.positionY shouldBe 2

            verify { workspaceTableRepository.save(table) }
        }

        it("should clear the position when position is null") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L, positionX = 3, positionY = 2)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)
            every { workspaceTableRepository.save(table) } returns table

            val result = sut.updateTablePosition(workspace, 1L, null, null)

            result.positionX shouldBe null
            result.positionY shouldBe null

            verify { workspaceTableRepository.save(table) }
            verify(exactly = 0) {
                workspaceTableRepository.existsByWorkspaceAndPositionXAndPositionYAndIdNot(
                    any(), any(), any(), any()
                )
            }
        }

        it("should throw TABLE_POSITION_CONFLICT when another table occupies the cell") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)
            every {
                workspaceTableRepository.existsByWorkspaceAndPositionXAndPositionYAndIdNot(workspace, 3, 2, 1L)
            } returns true

            val ex = shouldThrow<CustomException> { sut.updateTablePosition(workspace, 1L, 3, 2) }
            ex.errorCode shouldBe ErrorCode.TABLE_POSITION_CONFLICT

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }

        it("should allow re-saving the same table to the cell it already occupies") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L, positionX = 3, positionY = 2)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)
            every {
                workspaceTableRepository.existsByWorkspaceAndPositionXAndPositionYAndIdNot(workspace, 3, 2, 1L)
            } returns false
            every { workspaceTableRepository.save(table) } returns table

            sut.updateTablePosition(workspace, 1L, 3, 2)

            verify { workspaceTableRepository.save(table) }
        }

        it("should throw INVALID_TABLE_POSITION when a coordinate is negative") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)

            val ex = shouldThrow<CustomException> { sut.updateTablePosition(workspace, 1L, -1, 2) }
            ex.errorCode shouldBe ErrorCode.INVALID_TABLE_POSITION

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }

        it("should throw INVALID_TABLE_POSITION when a coordinate is at or beyond the grid limit") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)

            val ex = shouldThrow<CustomException> {
                sut.updateTablePosition(workspace, 1L, WorkspaceService.MAX_GRID_SIZE, 2)
            }
            ex.errorCode shouldBe ErrorCode.INVALID_TABLE_POSITION

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }

        it("should throw INVALID_TABLE_POSITION when y is negative") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)

            val ex = shouldThrow<CustomException> { sut.updateTablePosition(workspace, 1L, 3, -1) }
            ex.errorCode shouldBe ErrorCode.INVALID_TABLE_POSITION

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }

        it("should throw INVALID_TABLE_POSITION when y is at or beyond the grid limit") {
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { workspaceTableRepository.findByIdAndWorkspace(1L, workspace) } returns Optional.of(table)

            val ex = shouldThrow<CustomException> {
                sut.updateTablePosition(workspace, 1L, 3, WorkspaceService.MAX_GRID_SIZE)
            }
            ex.errorCode shouldBe ErrorCode.INVALID_TABLE_POSITION

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }

        it("should throw WORKSPACE_TABLE_NOT_FOUND when the table does not belong to the workspace") {
            val workspace = SampleEntity.workspace

            every { workspaceTableRepository.findByIdAndWorkspace(99L, workspace) } returns Optional.empty()

            val ex = shouldThrow<CustomException> { sut.updateTablePosition(workspace, 99L, 3, 2) }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_TABLE_NOT_FOUND

            verify(exactly = 0) { workspaceTableRepository.save(any()) }
        }
    }

    describe("updateTablePositions") {
        fun update(tableId: Long, x: Int?, y: Int?) = TablePositionUpdateDto(
            tableId,
            if (x != null && y != null) TablePositionDto(x, y) else null
        )

        it("should apply every position in a single request") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            sut.updateTablePositions(workspace, listOf(update(1L, 0, 0), update(2L, 1, 0)))

            tables[0].positionX shouldBe 0
            tables[0].positionY shouldBe 0
            tables[1].positionX shouldBe 1
            tables[1].positionY shouldBe 0

            verify { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should allow a swap that sequential per-table checks would reject") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 1, positionY = 0)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            // 1번을 (0,0) -> (1,0), 2번을 (1,0) -> (0,0). 순서대로 검사하면 첫 수정에서 409지만
            // 요청을 다 반영한 최종 상태에는 충돌이 없다.
            sut.updateTablePositions(workspace, listOf(update(1L, 1, 0), update(2L, 0, 0)))

            tables[0].positionX shouldBe 1
            tables[1].positionX shouldBe 0
        }

        it("should allow moving into a cell that the same request vacates") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 1, positionY = 0)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            // 1번은 배치를 취소하고, 2번이 그 자리로 들어온다.
            sut.updateTablePositions(workspace, listOf(update(1L, null, null), update(2L, 0, 0)))

            tables[0].positionX shouldBe null
            tables[0].positionY shouldBe null
            tables[1].positionX shouldBe 0
            tables[1].positionY shouldBe 0
        }

        it("should throw TABLE_POSITION_CONFLICT when two updates target the same cell") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 2, 3), update(2L, 2, 3)))
            }
            ex.errorCode shouldBe ErrorCode.TABLE_POSITION_CONFLICT

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should throw TABLE_POSITION_CONFLICT when the cell is held by a table outside the request") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 2, positionY = 3)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 2, 3)))
            }
            ex.errorCode shouldBe ErrorCode.TABLE_POSITION_CONFLICT

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should report the conflicting cell and its request index in the error details") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 4, 5), update(2L, 4, 5)))
            }

            ex.errors.size shouldBe 1
            ex.errors[0].field shouldBe "positions[1].position"
            ex.errors[0].value shouldBe "(4, 5)"
        }

        it("should clear the position when position is null") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 3, positionY = 2)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            sut.updateTablePositions(workspace, listOf(update(1L, null, null)))

            tables[0].positionX shouldBe null
            tables[0].positionY shouldBe null
        }

        it("should throw WORKSPACE_TABLE_NOT_FOUND when a table does not belong to the workspace") {
            val workspace = SampleEntity.workspace
            val tables = listOf(SampleEntity.workspaceTableWithId(1L, tableNumber = 1))

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 0, 0), update(99L, 1, 0)))
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_TABLE_NOT_FOUND

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should throw INVALID_TABLE_POSITION when a coordinate is out of the grid") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(
                    workspace,
                    listOf(update(1L, 0, 0), update(2L, WorkspaceService.MAX_GRID_SIZE, 0))
                )
            }
            ex.errorCode shouldBe ErrorCode.INVALID_TABLE_POSITION

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should throw INVALID_INPUT when the same table appears twice") {
            val workspace = SampleEntity.workspace
            val tables = listOf(SampleEntity.workspaceTableWithId(1L, tableNumber = 1))

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 0, 0), update(1L, 1, 0)))
            }
            ex.errorCode shouldBe ErrorCode.INVALID_INPUT

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should leave every position untouched when the request fails validation") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 1, positionY = 0)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            // 앞의 수정은 유효하지만 뒤가 격자를 벗어난다. 부분 반영이 남으면 안 된다.
            shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 5, 5), update(2L, -1, 0)))
            }

            tables[0].positionX shouldBe 0
            tables[0].positionY shouldBe 0
            tables[1].positionX shouldBe 1
            tables[1].positionY shouldBe 0
        }

        it("should ignore a pre-existing collision between tables the request does not touch") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                // 동시 저장 경합 등으로 이미 같은 칸에 겹쳐 있는 두 테이블. 이번 요청은 건드리지 않는다.
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 9, positionY = 9),
                SampleEntity.workspaceTableWithId(3L, tableNumber = 3, positionX = 9, positionY = 9)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            // 남의 겹침 때문에 관리자가 저장 자체를 못 하게 되면 안 된다.
            sut.updateTablePositions(workspace, listOf(update(1L, 0, 0)))

            tables[0].positionX shouldBe 0
            tables[0].positionY shouldBe 0
        }

        it("should still reject moving onto a cell that a pre-existing collision sits on") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 9, positionY = 9),
                SampleEntity.workspaceTableWithId(3L, tableNumber = 3, positionX = 9, positionY = 9)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables

            val ex = shouldThrow<CustomException> {
                sut.updateTablePositions(workspace, listOf(update(1L, 9, 9)))
            }
            ex.errorCode shouldBe ErrorCode.TABLE_POSITION_CONFLICT

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should not write anything when the request has no positions") {
            val workspace = SampleEntity.workspace

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns emptyList()

            sut.updateTablePositions(workspace, emptyList())

            verify(exactly = 0) { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }
    }

    describe("resetTablePositions") {
        it("should clear positions of all tables in the workspace") {
            val workspace = SampleEntity.workspace
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(2L, tableNumber = 2, positionX = 1, positionY = 0)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            sut.resetTablePositions(workspace)

            tables.forEach {
                it.positionX shouldBe null
                it.positionY shouldBe null
            }

            verify { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) }
        }

        it("should clear positions of tables beyond tableCount too") {
            val workspace = SampleEntity.workspace.apply { tableCount = 1 }
            val tables = listOf(
                SampleEntity.workspaceTableWithId(1L, tableNumber = 1, positionX = 0, positionY = 0),
                SampleEntity.workspaceTableWithId(5L, tableNumber = 5, positionX = 4, positionY = 0)
            )

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns tables
            every { workspaceTableRepository.saveAll(any<Iterable<WorkspaceTable>>()) } returns tables

            sut.resetTablePositions(workspace)

            // tableCount = 1이지만 5번 테이블 좌표도 비워져야 한다 -- 화면 밖에 갇힌 테이블의 복구 경로
            tables[1].positionX shouldBe null
            tables[1].positionY shouldBe null
        }
    }

    describe("saveWorkspaceTable") {
        it("should save workspace table") {
            val table = SampleEntity.workspaceTable

            every { workspaceTableRepository.save(table) } returns table

            sut.saveWorkspaceTable(table) shouldBe table

            verify { workspaceTableRepository.save(table) }
        }
    }
})