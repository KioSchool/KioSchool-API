package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionInfo
import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.repository.AcquisitionSurveyRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val acquisitionSurveyRepository = mockk<AcquisitionSurveyRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val emailService = mockk<EmailService>()

    val sut = UserService(
        repository,
        acquisitionSurveyRepository,
        passwordEncoder,
        emailService
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(acquisitionSurveyRepository)
        mockkObject(passwordEncoder)
        mockkObject(emailService)
    }

    afterTest {
        clearAllMocks()
    }

    describe("checkPassword") {
        it("should throw CustomException(LOGIN_FAILED) when password is not matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "wrong password"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns false

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkPassword(user, loginPassword)
            }
            ex.errorCode shouldBe ErrorCode.LOGIN_FAILED
        }

        it("should not throw CustomException(LOGIN_FAILED) when password is matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "test"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns true

            // Act & Assert
            sut.checkPassword(user, loginPassword)
        }
    }

    describe("saveUser") {
        it("should save user") {
            val user = SampleEntity.user

            // Mock
            every { repository.save(user) } returns user

            // Act
            sut.saveUser(user)

            // Assert
            verify { repository.save(user) }
        }
    }

    describe("saveUser with parameters") {
        it("should save user") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"

            // Mock
            every { repository.save(any<User>()) } returns SampleEntity.user
            every { passwordEncoder.encode(loginPassword) } returns "encoded password"

            // Act
            sut.saveUser(loginId, loginPassword, name, email) shouldBe SampleEntity.user

            // Assert
            verify { repository.save(any<User>()) }
        }
    }

    describe("saveAcquisitionSurvey") {
        it("should persist acquisition info on the survey entity") {
            val user = SampleEntity.user
            val acquisition = AcquisitionInfo(
                AcquisitionChannel.SENIOR_HANDOVER,
                null,
                "source=instagram"
            )
            val savedSlot = slot<AcquisitionSurvey>()
            every { acquisitionSurveyRepository.findByUser(user) } returns null
            every { acquisitionSurveyRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            sut.saveAcquisitionSurvey(user, acquisition)

            savedSlot.captured.user shouldBe user
            savedSlot.captured.channel shouldBe AcquisitionChannel.SENIOR_HANDOVER
            savedSlot.captured.channelEtc shouldBe null
            savedSlot.captured.context shouldBe "source=instagram"
        }

        it("should drop channelEtc when channel is not ETC") {
            val user = SampleEntity.user
            val acquisition = AcquisitionInfo(
                AcquisitionChannel.INSTAGRAM,
                "버려져야 하는 값",
                null
            ).normalized()
            val savedSlot = slot<AcquisitionSurvey>()
            every { acquisitionSurveyRepository.findByUser(user) } returns null
            every { acquisitionSurveyRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            sut.saveAcquisitionSurvey(user, acquisition)

            savedSlot.captured.channel shouldBe AcquisitionChannel.INSTAGRAM
            savedSlot.captured.channelEtc shouldBe null
        }

        it("should overwrite the existing survey when called twice for the same user") {
            val user = SampleEntity.user
            val existingSurvey = AcquisitionSurvey(
                user = user,
                channel = AcquisitionChannel.SEARCH,
                channelEtc = null,
                context = "first call"
            )
            val newAcquisition = AcquisitionInfo(
                AcquisitionChannel.ETC,
                "친구 추천",
                "second call"
            )
            val savedSlot = slot<AcquisitionSurvey>()
            every { acquisitionSurveyRepository.findByUser(user) } returns existingSurvey
            every { acquisitionSurveyRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            sut.saveAcquisitionSurvey(user, newAcquisition)

            savedSlot.captured shouldBe existingSurvey
            savedSlot.captured.channel shouldBe AcquisitionChannel.ETC
            savedSlot.captured.channelEtc shouldBe "친구 추천"
            savedSlot.captured.context shouldBe "second call"
            verify(exactly = 1) { acquisitionSurveyRepository.save(any()) }
        }
    }

    describe("validateLoginId") {
        it("should throw CustomException(DUPLICATE_LOGIN_ID) when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateLoginId(loginId)
            }
            ex.errorCode shouldBe ErrorCode.DUPLICATE_LOGIN_ID
        }

        it("should not throw CustomException(DUPLICATE_LOGIN_ID) when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.validateLoginId(loginId)
        }
    }

    describe("validateEmail") {
        it("should throw CustomException(EMAIL_NOT_VERIFIED) when email is not verified") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns false

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.EMAIL_NOT_VERIFIED

            verify { emailService.isRegisterEmailVerified(email) }
            verify(exactly = 0) { repository.findByEmail(email) }
        }

        it("should throw CustomException(DUPLICATE_EMAIL) when email is duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.DUPLICATE_EMAIL

            verify { emailService.isRegisterEmailVerified(email) }
            verify { repository.findByEmail(email) }
        }

        it("should not throw CustomException when email is verified and not duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            sut.validateEmail(email)

            verify { emailService.isRegisterEmailVerified(email) }
            verify { repository.findByEmail(email) }
        }
    }

    describe("isDuplicateLoginId") {
        it("should return true when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe true
        }

        it("should return false when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe false
        }
    }

    describe("getUser") {
        it("should return user when user exists") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.getUser(loginId) shouldBe SampleEntity.user
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.getUser(loginId)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("getUserByEmail") {
        it("should return user when user exists") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            sut.getUserByEmail(email) shouldBe SampleEntity.user

            verify { repository.findByEmail(email) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.getUserByEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("getAllUsers") {
        it("should call findByNameContains if name is not null") {
            val name = "test"
            val page = 0
            val size = 10

            // Mock
            every { repository.findByNameContains(name, Pageable.ofSize(size)) } returns PageImpl(
                listOf(SampleEntity.user)
            )

            // Act
            sut.getAllUsers(name, page, size)

            // Assert
            verify { repository.findByNameContains(name, Pageable.ofSize(size)) }
            verify(exactly = 0) { repository.findAll(Pageable.ofSize(size)) }
        }

        it("should call findAll if name is null") {
            val name = null
            val page = 0
            val size = 10

            // Mock
            every { repository.findAll(Pageable.ofSize(size)) } returns PageImpl(listOf(SampleEntity.user))

            // Act
            sut.getAllUsers(name, page, size)

            // Assert
            verify { repository.findAll(Pageable.ofSize(size)) }
            verify(exactly = 0) { repository.findByNameContains(any(), Pageable.ofSize(size)) }
        }
    }

    describe("isSuperAdminUser") {
        it("should return true when user is super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe true
        }

        it("should return false when user is not super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe false
        }
    }

    describe("checkHasSuperAdminPermission") {
        it("should not throw exception when user is super admin") {
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Act & Assert
            sut.checkHasSuperAdminPermission(user)
        }

        it("should throw exception when user is not super admin") {
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkHasSuperAdminPermission(user)
            }
            ex.errorCode shouldBe ErrorCode.NO_PERMISSION
        }
    }

    describe("removeAmountQueryFromAccountUrl") {
        it("should remove amount query from account url") {
            val accountUrl = "test?amount=100&something=else"

            // Act & Assert
            sut.removeAmountQueryFromAccountUrl(accountUrl) shouldBe "test?something=else"
        }
    }

    describe("checkEmailAddress") {
        it("should not throw exception when email is matched") {
            val email = "test@test.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            sut.checkEmailAddress(user, email)
        }

        it("should throw CustomException(USER_NOT_FOUND) when email is not matched") {
            val email = "test@test.com"
            val wrongEmail = "wrong@wrong.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkEmailAddress(user, wrongEmail)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("deleteUser") {
        it("should delete user") {
            val user = SampleEntity.user

            // Mock r
            every { repository.delete(user) } returns Unit

            // Act & Assert
            sut.deleteUser(user) shouldBe user

            verify { repository.delete(user) }
        }
    }

    describe("savePassword") {
        it("should save password") {
            val user = SampleEntity.user
            val password = "test"
            val encodedPassword = "encoded password"

            // Mock
            every { passwordEncoder.encode(password) } returns encodedPassword
            every { repository.save(user) } returns user

            // Act & Assert
            sut.savePassword(user, password).loginPassword shouldBe encodedPassword

            verify { repository.save(user) }
        }
    }
})