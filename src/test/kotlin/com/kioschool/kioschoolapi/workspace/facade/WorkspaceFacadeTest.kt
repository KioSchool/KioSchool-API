package com.kioschool.kioschoolapi.workspace.facade

import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionUpdateDto
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile

class WorkspaceFacadeTest : DescribeSpec({
    val userService = mockk<UserService>()
    val discordService = mockk<DiscordService>()
    val workspaceService = mockk<WorkspaceService>()
    val orderRepository = mockk<OrderRepository>()
    val orderSessionRepository = mockk<OrderSessionRepository>()
    val dailyOrderStatisticRepository = mockk<DailyOrderStatisticRepository>()

    val sut = WorkspaceFacade(userService, discordService, workspaceService, orderRepository, orderSessionRepository, dailyOrderStatisticRepository)

    beforeTest {
        mockkObject(userService)
        mockkObject(discordService)
        mockkObject(workspaceService)
        mockkObject(orderRepository)
        mockkObject(orderSessionRepository)
        mockkObject(dailyOrderStatisticRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("getWorkspace") {
        it("should call workspaceService.getWorkspace") {
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            every { workspaceService.getWorkspace(workspaceId) } returns workspace

            val result = sut.getWorkspace(workspaceId)

            assert(result.id == workspace.id)

            verify { workspaceService.getWorkspace(workspaceId) }
        }
    }

    describe("getWorkspaces") {
        it("should call userService.getUser and return user.getWorkspaces") {
            val username = "username"
            val user = SampleEntity.user.apply {
                members.add(SampleEntity.workspaceMember)
            }
            val workspaces = user.getWorkspaces()
            every { userService.getUser(username) } returns user

            val result = sut.getWorkspaces(username)

            assert(result.first().id == workspaces.first().id)

            verify { userService.getUser(username) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.getWorkspaces(username)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
        }
    }

    describe("getWorkspaceAccount") {
        it("should call workspaceService.getWorkspace and return decodedBank and accountNo") {
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply {
                owner.account = SampleEntity.account
            }

            every { workspaceService.getWorkspace(workspaceId) } returns workspace

            val result = sut.getWorkspaceAccount(workspaceId)

            assert(result?.id == SampleEntity.account.id)

            verify { workspaceService.getWorkspace(workspaceId) }
        }
    }

    describe("createWorkspace") {
        it("should call userService.getUser, workspaceService.checkCanCreateWorkspace, workspaceService.saveNewWorkspace and discordService.sendWorkspaceCreate") {
            val username = "username"
            val user = SampleEntity.user
            val name = "name"
            val description = "description"
            val workspace = SampleEntity.workspace
            every { userService.getUser(username) } returns user
            every { workspaceService.checkCanCreateWorkspace(user) } just Runs
            every { workspaceService.saveNewWorkspace(user, name, description) } returns workspace
            every { workspaceService.updateWorkspaceTables(workspace) } just Runs
            every { discordService.sendWorkspaceCreate(workspace) } just Runs

            val result = sut.createWorkspace(username, name, description)

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.checkCanCreateWorkspace(user) }
            verify { workspaceService.saveNewWorkspace(user, name, description) }
            verify { workspaceService.updateWorkspaceTables(workspace) }
            verify { discordService.sendWorkspaceCreate(workspace) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val name = "name"
            val description = "description"

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.createWorkspace(username, name, description)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.checkCanCreateWorkspace(any()) }
            verify(exactly = 0) { workspaceService.saveNewWorkspace(any(), any(), any()) }
            verify(exactly = 0) { workspaceService.updateWorkspaceTables(any()) }
            verify(exactly = 0) { discordService.sendWorkspaceCreate(any()) }
        }

        it("should throw CustomException(NO_PERMISSION_TO_CREATE_WORKSPACE) when user has no permission to create workspace") {
            val username = "username"
            val user = SampleEntity.user
            val name = "name"
            val description = "description"

            every { userService.getUser(username) } returns user
            every { workspaceService.checkCanCreateWorkspace(user) } throws CustomException(ErrorCode.NO_PERMISSION_TO_CREATE_WORKSPACE)

            val ex = assertThrows<CustomException> {
                sut.createWorkspace(username, name, description)
            }
            assertEquals(ErrorCode.NO_PERMISSION_TO_CREATE_WORKSPACE, ex.errorCode)

            verify { userService.getUser(username) }
            verify { workspaceService.checkCanCreateWorkspace(user) }
            verify(exactly = 0) { workspaceService.saveNewWorkspace(any(), any(), any()) }
            verify(exactly = 0) { workspaceService.updateWorkspaceTables(any()) }
            verify(exactly = 0) { discordService.sendWorkspaceCreate(any()) }
        }
    }

    describe("inviteWorkspace") {
        it("should call userService.getUser, workspaceService.getWorkspace, workspaceService.checkCanInviteWorkspace and workspaceService.inviteUserToWorkspace") {
            val hostUserName = "hostUserName"
            val hostUser = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val userLoginId = "userLoginId"
            val user = SampleEntity.user

            every { userService.getUser(hostUserName) } returns hostUser
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanInviteWorkspace(hostUser, workspace) } just Runs
            every { userService.getUser(userLoginId) } returns user
            every { workspaceService.inviteUserToWorkspace(workspace, user) } returns workspace

            val result = sut.inviteWorkspace(hostUserName, workspaceId, userLoginId)

            assert(result.id == workspace.id)

            verify { userService.getUser(hostUserName) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanInviteWorkspace(hostUser, workspace) }
            verify { userService.getUser(userLoginId) }
            verify { workspaceService.inviteUserToWorkspace(workspace, user) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when host user not found") {
            val hostUserName = "hostUserName"
            val workspaceId = 1L
            val userLoginId = "userLoginId"

            every { userService.getUser(hostUserName) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.inviteWorkspace(hostUserName, workspaceId, userLoginId)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify(exactly = 1) { userService.getUser(hostUserName) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanInviteWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.inviteUserToWorkspace(any(), any()) }
        }

        it("should throw CustomException(NO_PERMISSION_TO_INVITE) when host user has no permission to invite") {
            val hostUserName = "hostUserName"
            val hostUser = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val userLoginId = "userLoginId"

            every { userService.getUser(hostUserName) } returns hostUser
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                workspaceService.checkCanInviteWorkspace(
                    hostUser,
                    workspace
                )
            } throws CustomException(ErrorCode.NO_PERMISSION_TO_INVITE)

            val ex = assertThrows<CustomException> {
                sut.inviteWorkspace(hostUserName, workspaceId, userLoginId)
            }
            assertEquals(ErrorCode.NO_PERMISSION_TO_INVITE, ex.errorCode)

            verify(exactly = 1) { userService.getUser(hostUserName) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanInviteWorkspace(hostUser, workspace) }
            verify(exactly = 0) { workspaceService.inviteUserToWorkspace(any(), any()) }
        }
    }

    describe("joinWorkspace") {
        it("should call userService.getUser, workspaceService.getWorkspace, workspaceService.checkCanJoinWorkspace and workspaceService.addUserToWorkspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanJoinWorkspace(user, workspace) } just Runs
            every { workspaceService.addUserToWorkspace(workspace, user) } just Runs

            val result = sut.joinWorkspace(username, workspaceId)

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanJoinWorkspace(user, workspace) }
            verify { workspaceService.addUserToWorkspace(workspace, user) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val workspaceId = 1L

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.joinWorkspace(username, workspaceId)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanJoinWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.addUserToWorkspace(any(), any()) }
        }

        it("should throw CustomException(NO_PERMISSION_TO_JOIN_WORKSPACE) when user has no permission to join workspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                workspaceService.checkCanJoinWorkspace(
                    user,
                    workspace
                )
            } throws CustomException(ErrorCode.NO_PERMISSION_TO_JOIN_WORKSPACE)

            val ex = assertThrows<CustomException> {
                sut.joinWorkspace(username, workspaceId)
            }
            assertEquals(ErrorCode.NO_PERMISSION_TO_JOIN_WORKSPACE, ex.errorCode)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanJoinWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.addUserToWorkspace(any(), any()) }
        }
    }

    describe("leaveWorkspace") {
        it("should call userService.getUser, workspaceService.getWorkspace and workspaceService.removeUserFromWorkspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.removeUserFromWorkspace(workspace, user) } returns workspace

            val result = sut.leaveWorkspace(username, workspaceId)

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.removeUserFromWorkspace(workspace, user) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val workspaceId = 1L

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.leaveWorkspace(username, workspaceId)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.removeUserFromWorkspace(any(), any()) }
        }
    }

    describe("updateTableCount") {
        it("should call userService.getUser, workspaceService.getWorkspace and workspaceService.updateTableCount") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val tableCount = 10

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.updateTableCount(workspace, tableCount) } returns workspace
            every { workspaceService.updateWorkspaceTables(workspace) } just Runs

            val result = sut.updateTableCount(username, workspaceId, tableCount)

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.updateTableCount(workspace, tableCount) }
            verify { workspaceService.updateWorkspaceTables(workspace) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val workspaceId = 1L
            val tableCount = 10

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.updateTableCount(username, workspaceId, tableCount)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.updateTableCount(any(), any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) when user has no permission to update table count") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val tableCount = 10

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(
                    user,
                    workspace
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.updateTableCount(username, workspaceId, tableCount)
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.updateTableCount(any(), any()) }
        }
    }

    describe("updateWorkspaceInfo") {
        it("should call userService.getUser, workspaceService.getWorkspace and workspaceService.saveWorkspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val name = "name"
            val description = "description"
            val notice = "notice"

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.saveWorkspace(workspace) } returns workspace

            val result = sut.updateWorkspaceInfo(
                username,
                workspaceId,
                name,
                description,
                notice,
            )

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.saveWorkspace(workspace) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            val description = "description"
            val notice = "notice"

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.updateWorkspaceInfo(
                    username,
                    workspaceId,
                    name,
                    description,
                    notice,
                )
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspace(any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) when user has no permission to update workspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val name = "name"
            val description = "description"
            val notice = "notice"

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(
                    user,
                    workspace
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.updateWorkspaceInfo(
                    username,
                    workspaceId,
                    name,
                    description,
                    notice,
                )
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.saveWorkspace(any()) }
        }
    }

    describe("updateWorkspaceImage") {
        beforeTest {
            SampleEntity.workspace.images.clear()
        }

        it("should call userService.getUser, workspaceService.getWorkspace, workspaceService.deleteWorkspaceImages, workspaceService.saveWorkspaceImages and workspaceService.saveWorkspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply {
                images.addAll(SampleEntity.workspaceImages)
            }
            val imageIds = listOf(1L, 2L, 3L)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), mockk<MultipartFile>())


            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.deleteWorkspaceImages(workspace, any()) } just Runs
            every {
                workspaceService.saveWorkspaceImages(
                    workspace,
                    any<List<MultipartFile>>(),
                )
            } returns workspace

            val result = sut.updateWorkspaceImage(
                username,
                workspaceId,
                imageIds,
                imageFiles,
            )

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.deleteWorkspaceImages(workspace, any()) }
            verify { workspaceService.saveWorkspaceImages(workspace, any<List<MultipartFile>>()) }
        }

        it("should delete workspace images when imageIds is not exist and save workspace images") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply {
                images.addAll(SampleEntity.workspaceImages)
            }
            val imageIds = listOf(workspace.images[2].id, null, null)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>())

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every {
                workspaceService.deleteWorkspaceImages(
                    workspace,
                    arrayListOf(workspace.images[0], workspace.images[1])
                )
            } just Runs
            every {
                workspaceService.saveWorkspaceImages(
                    workspace,
                    imageFiles
                )
            } returns workspace

            val result = sut.updateWorkspaceImage(
                username,
                workspaceId,
                imageIds,
                imageFiles
            )

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify {
                workspaceService.deleteWorkspaceImages(
                    workspace,
                    arrayListOf(workspace.images[0], workspace.images[1])
                )
            }
            verify { workspaceService.saveWorkspaceImages(workspace, imageFiles) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val username = "username"
            val workspaceId = 1L
            val imageIds = listOf(1L, 2L, 3L)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), mockk<MultipartFile>())

            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.updateWorkspaceImage(
                    username,
                    workspaceId,
                    imageIds,
                    imageFiles
                )
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.deleteWorkspaceImages(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspaceImages(any(), any()) }
        }

        it("should throw CustomException(WORKSPACE_INACCESSIBLE) when user has no permission to update workspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val imageIds = listOf(1L, 2L, 3L)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), mockk<MultipartFile>())

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(
                    user,
                    workspace
                )
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.updateWorkspaceImage(
                    username,
                    workspaceId,
                    imageIds,
                    imageFiles
                )
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.deleteWorkspaceImages(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspaceImages(any(), any()) }
        }
    }

    describe("getAllWorkspaceTables") {
        it("should get all workspace tables") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.getAllWorkspaceTables(workspace) } returns listOf(SampleEntity.workspaceTable)

            val result = sut.getAllWorkspaceTables(username, workspaceId)

            assert(result.first().id == SampleEntity.workspaceTable.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.getAllWorkspaceTables(workspace) }
        }
    }

    describe("updateTablePosition") {
        it("should check access then delegate to workspaceService") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L, positionX = 3, positionY = 2)

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.updateTablePosition(workspace, 1L, 3, 2) } returns table

            val result = sut.updateTablePosition(username, 1L, 1L, TablePositionDto(3, 2))

            assertEquals(TablePositionDto(3, 2), result.position)

            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.updateTablePosition(workspace, 1L, 3, 2) }
        }

        it("should pass null coordinates when position is null") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            val table = SampleEntity.workspaceTableWithId(1L)

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.updateTablePosition(workspace, 1L, null, null) } returns table

            val result = sut.updateTablePosition(username, 1L, 1L, null)

            assertEquals(null, result.position)

            verify { workspaceService.updateTablePosition(workspace, 1L, null, null) }
        }

        it("should not call updateTablePosition when the workspace is inaccessible") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(user, workspace)
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            val ex = assertThrows<CustomException> {
                sut.updateTablePosition(username, 1L, 1L, TablePositionDto(3, 2))
            }
            assertEquals(ErrorCode.WORKSPACE_INACCESSIBLE, ex.errorCode)

            verify(exactly = 0) { workspaceService.updateTablePosition(any(), any(), any(), any()) }
        }
    }

    describe("updateTablePositions") {
        val positions = listOf(TablePositionUpdateDto(1L, TablePositionDto(2, 0)))

        it("should check access, delegate to workspaceService, then return the GET view") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            val tables = listOf(SampleEntity.workspaceTableWithId(1L, positionX = 2, positionY = 0))

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.updateTablePositions(workspace, positions) } just Runs
            every { workspaceService.getAllWorkspaceTables(workspace) } returns tables

            val result = sut.updateTablePositions(username, 1L, positions)

            assertEquals(1, result.size)
            assertEquals(TablePositionDto(2, 0), result.first().position)

            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.updateTablePositions(workspace, positions) }
            verify { workspaceService.getAllWorkspaceTables(workspace) }
        }

        it("should not call updateTablePositions when the workspace is inaccessible") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(user, workspace)
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            assertThrows<CustomException> { sut.updateTablePositions(username, 1L, positions) }

            verify(exactly = 0) { workspaceService.updateTablePositions(any(), any()) }
        }
    }

    describe("resetTablePositions") {
        it("should check access then delegate to workspaceService") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            val tables = listOf(SampleEntity.workspaceTableWithId(1L))

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.resetTablePositions(workspace) } just Runs
            every { workspaceService.getAllWorkspaceTables(workspace) } returns tables

            val result = sut.resetTablePositions(username, 1L)

            assertEquals(1, result.size)
            assertEquals(null, result.first().position)

            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.resetTablePositions(workspace) }
            verify { workspaceService.getAllWorkspaceTables(workspace) }
        }

        it("should not call resetTablePositions when the workspace is inaccessible") {
            val username = "username"
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(1L) } returns workspace
            every {
                workspaceService.checkCanAccessWorkspace(user, workspace)
            } throws CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)

            assertThrows<CustomException> { sut.resetTablePositions(username, 1L) }

            verify(exactly = 0) { workspaceService.resetTablePositions(any()) }
        }
    }

    describe("forceDeleteWorkspace") {
        it("should use the unsliced table accessor so out-of-range tables' orderSession refs are cleared before the FK-dependent deletes") {
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply { tableCount = 2 }

            // 범위 안 테이블
            val inRangeTable = SampleEntity.workspaceTableWithId(1L, tableNumber = 1)
            // 범위 밖 테이블이지만 살아있는 orderSession을 참조 -- 슬라이스된 뷰였다면 누락되어
            // OrderSession 삭제 시 FK 제약 위반이 났을 행
            val outOfRangeTableWithSession = SampleEntity.workspaceTableWithId(2L, tableNumber = 5).apply {
                orderSession = SampleEntity.testSession1
            }
            val allTables = listOf(inRangeTable, outOfRangeTableWithSession)

            val orders = listOf(SampleEntity.order1)
            val sessions = listOf(SampleEntity.testSession1)

            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { dailyOrderStatisticRepository.deleteByWorkspaceId(workspaceId) } just Runs
            every { orderRepository.findAllByWorkspaceId(workspaceId) } returns orders
            every { orderRepository.deleteAll(orders) } just Runs
            every { workspaceService.getAllWorkspaceTablesIncludingOutOfRange(workspace) } returns allTables
            every { workspaceService.saveWorkspaceTable(outOfRangeTableWithSession) } returns outOfRangeTableWithSession
            every { orderSessionRepository.findAllByWorkspaceId(workspaceId) } returns sessions
            every { orderSessionRepository.deleteAll(sessions) } just Runs
            every { workspaceService.deleteAllWorkspaceTables(workspace) } just Runs
            every { workspaceService.deleteWorkspace(workspace) } just Runs

            val result = sut.forceDeleteWorkspace(workspaceId)

            assert(result.id == workspace.id)
            assert(outOfRangeTableWithSession.orderSession == null)

            // 1. DailyOrderStatistic 삭제
            verify { dailyOrderStatisticRepository.deleteByWorkspaceId(workspaceId) }
            // 2. Order 삭제
            verify { orderRepository.findAllByWorkspaceId(workspaceId) }
            verify { orderRepository.deleteAll(orders) }
            // 3. WorkspaceTable의 orderSession 참조 해제 -- 반드시 범위 밖 테이블까지 포함하는
            //    접근자를 써야 하고, 관리자 화면용 슬라이스된 접근자는 절대 쓰지 않아야 한다
            verify { workspaceService.getAllWorkspaceTablesIncludingOutOfRange(workspace) }
            verify(exactly = 0) { workspaceService.getAllWorkspaceTables(any()) }
            verify { workspaceService.saveWorkspaceTable(outOfRangeTableWithSession) }
            verify(exactly = 0) { workspaceService.saveWorkspaceTable(inRangeTable) }
            // 4. OrderSession 삭제
            verify { orderSessionRepository.findAllByWorkspaceId(workspaceId) }
            verify { orderSessionRepository.deleteAll(sessions) }
            // 5. WorkspaceTable 삭제
            verify { workspaceService.deleteAllWorkspaceTables(workspace) }
            // 6. Workspace 삭제
            verify { workspaceService.deleteWorkspace(workspace) }
        }
    }

    describe("updateOrderSetting") {
        it("should update order setting") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val useOrderSessionTimeLimit = true
            val orderSessionTimeLimitMinutes = 60L

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.saveWorkspace(workspace) } returns workspace

            val result = sut.updateOrderSetting(
                username,
                workspaceId,
                useOrderSessionTimeLimit,
                orderSessionTimeLimitMinutes
            )

            assert(result.id == workspace.id)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.saveWorkspace(workspace) }
        }
    }
})