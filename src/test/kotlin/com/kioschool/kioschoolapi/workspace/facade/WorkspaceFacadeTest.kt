package com.kioschool.kioschoolapi.workspace.facade

import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToCreateWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToInviteException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToJoinWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile

class WorkspaceFacadeTest : DescribeSpec({
    val userService = mockk<UserService>()
    val discordService = mockk<DiscordService>()
    val workspaceService = mockk<WorkspaceService>()

    val sut = WorkspaceFacade(userService, discordService, workspaceService)

    beforeTest {
        mockkObject(userService)
        mockkObject(discordService)
        mockkObject(workspaceService)
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

            assert(result == workspace)

            verify { workspaceService.getWorkspace(workspaceId) }
        }
    }

    describe("getWorkspaces") {
        it("should call userService.getUser and return user.getWorkspaces") {
            val username = "username"
            val user = SampleEntity.user
            val workspaces = user.getWorkspaces()
            every { userService.getUser(username) } returns user

            val result = sut.getWorkspaces(username)

            assert(result == workspaces)

            verify { userService.getUser(username) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.getWorkspaces(username)
            }

            verify { userService.getUser(username) }
        }
    }

    describe("getWorkspaceAccount") {
        it("should call workspaceService.getWorkspace and return decodedBank and accountNo") {
            val workspaceId = 1L
            val accountUrl = "accountUrl"
            val workspace = SampleEntity.workspace.apply {
                owner.accountUrl = accountUrl
            }
            val decodedBank = "decodedBank"
            val accountNo = "accountNo"
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.extractDecodedBank(accountUrl) } returns decodedBank
            every { workspaceService.extractAccountNo(accountUrl) } returns accountNo

            val result = sut.getWorkspaceAccount(workspaceId)

            assert(result == "$decodedBank $accountNo")

            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.extractDecodedBank(accountUrl) }
            verify { workspaceService.extractAccountNo(accountUrl) }
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
            every { discordService.sendWorkspaceCreate(workspace) } just Runs

            val result = sut.createWorkspace(username, name, description)

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.checkCanCreateWorkspace(user) }
            verify { workspaceService.saveNewWorkspace(user, name, description) }
            verify { discordService.sendWorkspaceCreate(workspace) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val name = "name"
            val description = "description"

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.createWorkspace(username, name, description)
            }

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.checkCanCreateWorkspace(any()) }
            verify(exactly = 0) { workspaceService.saveNewWorkspace(any(), any(), any()) }
            verify(exactly = 0) { discordService.sendWorkspaceCreate(any()) }
        }

        it("should throw NoPermissionToCreateWorkspaceException when user has no permission to create workspace") {
            val username = "username"
            val user = SampleEntity.user
            val name = "name"
            val description = "description"

            every { userService.getUser(username) } returns user
            every { workspaceService.checkCanCreateWorkspace(user) } throws NoPermissionToCreateWorkspaceException()

            assertThrows<NoPermissionToCreateWorkspaceException> {
                sut.createWorkspace(username, name, description)
            }

            verify { userService.getUser(username) }
            verify { workspaceService.checkCanCreateWorkspace(user) }
            verify(exactly = 0) { workspaceService.saveNewWorkspace(any(), any(), any()) }
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

            assert(result == workspace)

            verify { userService.getUser(hostUserName) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanInviteWorkspace(hostUser, workspace) }
            verify { userService.getUser(userLoginId) }
            verify { workspaceService.inviteUserToWorkspace(workspace, user) }
        }

        it("should throw UserNotFoundException when host user not found") {
            val hostUserName = "hostUserName"
            val workspaceId = 1L
            val userLoginId = "userLoginId"

            every { userService.getUser(hostUserName) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.inviteWorkspace(hostUserName, workspaceId, userLoginId)
            }

            verify(exactly = 1) { userService.getUser(hostUserName) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanInviteWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.inviteUserToWorkspace(any(), any()) }
        }

        it("should throw NoPermissionToInviteException when host user has no permission to invite") {
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
            } throws NoPermissionToInviteException()

            assertThrows<NoPermissionToInviteException> {
                sut.inviteWorkspace(hostUserName, workspaceId, userLoginId)
            }

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

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanJoinWorkspace(user, workspace) }
            verify { workspaceService.addUserToWorkspace(workspace, user) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val workspaceId = 1L

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.joinWorkspace(username, workspaceId)
            }

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanJoinWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.addUserToWorkspace(any(), any()) }
        }

        it("should throw NoPermissionToJoinWorkspaceException when user has no permission to join workspace") {
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
            } throws NoPermissionToJoinWorkspaceException()

            assertThrows<NoPermissionToJoinWorkspaceException> {
                sut.joinWorkspace(username, workspaceId)
            }

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

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.removeUserFromWorkspace(workspace, user) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val workspaceId = 1L

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.leaveWorkspace(username, workspaceId)
            }

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
            every { workspaceService.updateTableCount(workspace, tableCount) } just Runs

            val result = sut.updateTableCount(username, workspaceId, tableCount)

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.updateTableCount(workspace, tableCount) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val workspaceId = 1L
            val tableCount = 10

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.updateTableCount(username, workspaceId, tableCount)
            }

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.updateTableCount(any(), any()) }
        }

        it("should throw WorkspaceInaccessibleException when user has no permission to update table count") {
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
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateTableCount(username, workspaceId, tableCount)
            }

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

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.saveWorkspace(workspace) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val workspaceId = 1L
            val name = "name"
            val description = "description"
            val notice = "notice"

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.updateWorkspaceInfo(
                    username,
                    workspaceId,
                    name,
                    description,
                    notice,
                )
            }

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspace(any()) }
        }

        it("should throw WorkspaceInaccessibleException when user has no permission to update workspace") {
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
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateWorkspaceInfo(
                    username,
                    workspaceId,
                    name,
                    description,
                    notice,
                )
            }

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.saveWorkspace(any()) }
        }
    }

    describe("updateWorkspaceImage") {
        it("should call userService.getUser, workspaceService.getWorkspace, workspaceService.deleteWorkspaceImages, workspaceService.saveWorkspaceImages and workspaceService.saveWorkspace") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val imageIds = listOf(1L, 2L, 3L)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), mockk<MultipartFile>())


            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.deleteWorkspaceImages(workspace, any<List<Long>>()) } just Runs
            every {
                workspaceService.saveWorkspaceImages(
                    workspace,
                    any<List<MultipartFile>>()
                )
            } returns workspace

            val result = sut.updateWorkspaceImage(
                username,
                workspaceId,
                imageIds,
                imageFiles,
            )

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.deleteWorkspaceImages(workspace, any<List<Long>>()) }
            verify { workspaceService.saveWorkspaceImages(workspace, any<List<MultipartFile>>()) }
        }

        it("should delete workspace images when imageIds is not exist and save workspace images") {
            val username = "username"
            val user = SampleEntity.user
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply {
                images.addAll(SampleEntity.workspaceImages)
            }
            val imageIds = listOf(3L, null, null)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), null)

            every { userService.getUser(username) } returns user
            every { workspaceService.getWorkspace(workspaceId) } returns workspace
            every { workspaceService.checkCanAccessWorkspace(user, workspace) } just Runs
            every { workspaceService.deleteWorkspaceImages(workspace, listOf(1L, 2L)) } just Runs
            every {
                workspaceService.saveWorkspaceImages(
                    workspace,
                    listOf(imageFiles[1]!!)
                )
            } returns workspace

            val result = sut.updateWorkspaceImage(
                username,
                workspaceId,
                imageIds,
                imageFiles
            )

            assert(result == workspace)

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify { workspaceService.deleteWorkspaceImages(workspace, listOf(1L, 2L)) }
            verify { workspaceService.saveWorkspaceImages(workspace, listOf(imageFiles[1]!!)) }
        }

        it("should throw UserNotFoundException when user not found") {
            val username = "username"
            val workspaceId = 1L
            val imageIds = listOf(1L, 2L, 3L)
            val imageFiles =
                listOf(mockk<MultipartFile>(), mockk<MultipartFile>(), mockk<MultipartFile>())

            every { userService.getUser(username) } throws UserNotFoundException()

            assertThrows<UserNotFoundException> {
                sut.updateWorkspaceImage(
                    username,
                    workspaceId,
                    imageIds,
                    imageFiles
                )
            }

            verify { userService.getUser(username) }
            verify(exactly = 0) { workspaceService.getWorkspace(any()) }
            verify(exactly = 0) { workspaceService.checkCanAccessWorkspace(any(), any()) }
            verify(exactly = 0) { workspaceService.deleteWorkspaceImages(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspaceImages(any(), any()) }
        }

        it("should throw WorkspaceInaccessibleException when user has no permission to update workspace") {
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
            } throws WorkspaceInaccessibleException()

            assertThrows<WorkspaceInaccessibleException> {
                sut.updateWorkspaceImage(
                    username,
                    workspaceId,
                    imageIds,
                    imageFiles
                )
            }

            verify { userService.getUser(username) }
            verify { workspaceService.getWorkspace(workspaceId) }
            verify { workspaceService.checkCanAccessWorkspace(user, workspace) }
            verify(exactly = 0) { workspaceService.deleteWorkspaceImages(any(), any()) }
            verify(exactly = 0) { workspaceService.saveWorkspaceImages(any(), any()) }
        }
    }
})