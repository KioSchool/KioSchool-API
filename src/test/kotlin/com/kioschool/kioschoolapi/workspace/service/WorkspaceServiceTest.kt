package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.aws.S3Service
import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToCreateWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToInviteException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToJoinWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.multipart.MultipartFile
import java.util.*

class WorkspaceServiceTest : DescribeSpec({
    val repository = mockk<WorkspaceRepository>()
    val userService = mockk<UserService>()
    val s3Service = mockk<S3Service>()

    val sut = WorkspaceService("test", repository, userService, s3Service)

    beforeTest {
        mockkObject(repository)
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
                repository.findByNameContains(
                    name,
                    PageRequest.of(page, size)
                )
            } returns PageImpl(listOf())

            // Act
            sut.getAllWorkspaces(name, page, size)

            // Assert
            verify { repository.findByNameContains(name, PageRequest.of(page, size)) }
            verify(exactly = 0) { repository.findAll(PageRequest.of(page, size)) }
        }

        it("should call findAll when name is null") {
            val name = null
            val page = 0
            val size = 10

            // Mock
            every {
                repository.findAll(PageRequest.of(page, size))
            } returns PageImpl(listOf())

            // Act
            sut.getAllWorkspaces(name, page, size)

            // Assert
            verify { repository.findAll(PageRequest.of(page, size)) }
            verify(exactly = 0) { repository.findByNameContains(any(), any()) }
        }
    }

    describe("checkCanCreateWorkspace") {
        it("should throw NoPermissionToCreateWorkspaceException when user accountUrl is null") {
            val user = SampleEntity.user.apply { accountUrl = null }

            // Act & Assert
            shouldThrow<NoPermissionToCreateWorkspaceException> {
                sut.checkCanCreateWorkspace(user)
            }
        }

        it("should not throw NoPermissionToCreateWorkspaceException when user accountUrl is not null") {
            val user = SampleEntity.user.apply { accountUrl = "accountUrl" }

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
        it("should throw NoPermissionToJoinWorkspaceException when user is not invited") {
            val user = SampleEntity.user
            val workspace = SampleEntity.workspace
            workspace.invitations.clear()

            // Act & Assert
            shouldThrow<NoPermissionToJoinWorkspaceException> {
                sut.checkCanJoinWorkspace(user, workspace)
            }
        }

        it("should not throw NoPermissionToJoinWorkspaceException when user is invited") {
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
            val workspace = SampleEntity.workspace
            val user = SampleEntity.user
            workspace.members.add(SampleEntity.workspaceMember(user, workspace))

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act
            sut.isAccessible(username, workspaceId) shouldBe true

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }

        it("should return true when user is a super admin") {
            val username = "test"
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply { members.clear() }
            val user = SampleEntity.user.apply { role = UserRole.SUPER_ADMIN }

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act
            sut.isAccessible(username, workspaceId) shouldBe true

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }

        it("should return false when user is not a member of workspace and not a super admin") {
            val username = "test"
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply { members.clear() }
            val user = SampleEntity.user.apply { role = UserRole.ADMIN }

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act
            sut.isAccessible(username, workspaceId) shouldBe false

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }
    }

    describe("checkAccessible") {
        it("should throw WorkspaceInaccessibleException when user is not a member of workspace and not a super admin") {
            val username = "test"
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply { members.clear() }
            val user = SampleEntity.user.apply { role = UserRole.ADMIN }

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act & Assert
            shouldThrow<WorkspaceInaccessibleException> {
                sut.checkAccessible(username, workspaceId)
            }

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }

        it("should not throw WorkspaceInaccessibleException when user is a member of workspace") {
            val username = "test"
            val workspaceId = 1L
            val workspace = SampleEntity.workspace
            val user = SampleEntity.user
            workspace.members.add(SampleEntity.workspaceMember(user, workspace))

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act & Assert
            sut.checkAccessible(username, workspaceId)

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }

        it("should not throw WorkspaceInaccessibleException when user is a super admin") {
            val username = "test"
            val workspaceId = 1L
            val workspace = SampleEntity.workspace.apply { members.clear() }
            val user = SampleEntity.user.apply { role = UserRole.SUPER_ADMIN }

            // Mock
            every {
                repository.findById(workspaceId)
            } returns Optional.of(workspace)

            every {
                userService.getUser(username)
            } returns user

            // Act & Assert
            sut.checkAccessible(username, workspaceId)

            // Assert
            verify { repository.findById(workspaceId) }
            verify { userService.getUser(username) }
        }
    }

    describe("checkCanInviteWorkspace") {
        it("should throw NoPermissionToInviteException when user is not owner of workspace") {
            val user = SampleEntity.user
            val otherUser = SampleEntity.otherUser
            val inaccessibleWorkspace = SampleEntity.workspace(otherUser)

            // Act & Assert
            shouldThrow<NoPermissionToInviteException> {
                sut.checkCanInviteWorkspace(user, inaccessibleWorkspace)
            }
        }

        it("should not throw NoPermissionToInviteException when user is owner of workspace") {
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

    describe("extractDecodedBank") {
        it("should extract decoded bank") {
            val accountUrl = "https://test.com?bank=bank"

            // Act
            val result = sut.extractDecodedBank(accountUrl)

            // Assert
            result shouldBe "bank"
        }
    }

    describe("extractAccountNo") {
        it("should extract account no") {
            val accountUrl = "https://test.com?accountNo=accountNo"

            // Act
            val result = sut.extractAccountNo(accountUrl)

            // Assert
            result shouldBe "accountNo"
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

    describe("getImageUrl") {
        it("should call uploadFileToS3 when file is not null") {
            val workspaceId = 1L
            val file = mockk<MultipartFile>()

            every { s3Service.uploadFile(file, any<String>()) } returns "url"

            val result = sut.getImageUrl(workspaceId, file)

            assert(result == "url")

            verify { s3Service.uploadFile(file, any<String>()) }
        }

        it("should return null when file is null") {
            val workspaceId = 1L
            val file = null

            val result = sut.getImageUrl(workspaceId, file)

            assert(result == null)

            verify(exactly = 0) { s3Service.uploadFile(any(), any<String>()) }
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
})