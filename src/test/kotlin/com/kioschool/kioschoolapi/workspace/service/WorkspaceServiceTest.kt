package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.domain.user.service.UserService
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
            val workspace = SampleEntity.workspace
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
        it("should get all workspace tables") {
            val workspace = SampleEntity.workspace

            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns listOf(
                SampleEntity.workspaceTable
            )

            sut.getAllWorkspaceTables(workspace) shouldBe listOf(SampleEntity.workspaceTable)

            verify { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) }
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

        it("should remove tables when table count is decreased") {
            val workspace = SampleEntity.workspace.apply { tableCount = 1 }

            every { workspaceTableRepository.countAllByWorkspace(workspace) } returns 3
            every { workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace) } returns listOf(
                SampleEntity.workspaceTable,
                SampleEntity.workspaceTable,
                SampleEntity.workspaceTable
            )
            every { workspaceTableRepository.deleteAll(any<Iterable<WorkspaceTable>>()) } just Runs

            sut.updateWorkspaceTables(workspace)

            verify { workspaceTableRepository.deleteAll(any<Iterable<WorkspaceTable>>()) }
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

    describe("saveWorkspaceTable") {
        it("should save workspace table") {
            val table = SampleEntity.workspaceTable

            every { workspaceTableRepository.save(table) } returns table

            sut.saveWorkspaceTable(table) shouldBe table

            verify { workspaceTableRepository.save(table) }
        }
    }
})