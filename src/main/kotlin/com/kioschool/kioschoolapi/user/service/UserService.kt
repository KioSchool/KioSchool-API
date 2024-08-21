package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val discordService: DiscordService
) {
    fun login(loginId: String, loginPassword: String): String {
        val user = getUser(loginId)
        checkPassword(user, loginPassword)

        return jwtProvider.createToken(user)
    }

    fun checkPassword(user: User, loginPassword: String) {
        if (!passwordEncoder.matches(
                loginPassword,
                user.loginPassword
            )
        ) throw LoginFailedException()
    }

    fun register(loginId: String, loginPassword: String, name: String, email: String): String {
        validateLoginId(loginId)
        validateEmail(email)
        emailService.deleteRegisterCode(email)

        val user = userRepository.save(
            User(
                loginId = loginId,
                loginPassword = passwordEncoder.encode(loginPassword),
                name = name,
                email = email,
                role = UserRole.ADMIN,
                members = mutableListOf()
            )
        )

        discordService.sendUserRegister(user)
        return jwtProvider.createToken(user)
    }

    fun validateLoginId(loginId: String) {
        if (isDuplicateLoginId(loginId)) throw RegisterException()
    }

    fun validateEmail(email: String) {
        checkIsEmailVerified(email)
        checkIsEmailDuplicate(email)
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userRepository.findByLoginId(loginId) != null
    }

    fun checkIsEmailVerified(email: String) {
        if (!emailService.isEmailVerified(email)) throw RegisterException()
    }

    fun checkIsEmailDuplicate(email: String) {
        if (isDuplicateEmail(email)) throw RegisterException()
    }

    fun isDuplicateEmail(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    fun getUser(loginId: String): User {
        return userRepository.findByLoginId(loginId) ?: throw UserNotFoundException()
    }

    fun getAllUsers(page: Int, size: Int): Page<User> {
        return userRepository.findAll(PageRequest.of(page, size))
    }

    fun isSuperAdminUser(username: String): Boolean {
        return getUser(username).role == UserRole.SUPER_ADMIN
    }

    fun createSuperAdminUser(username: String, id: String): User {
        val superAdminUser = getUser(username)
        checkHasSuperAdminPermission(superAdminUser)

        val user = getUser(id)
        user.role = UserRole.SUPER_ADMIN
        return userRepository.save(user)
    }

    fun checkHasSuperAdminPermission(user: User) {
        if (user.role != UserRole.SUPER_ADMIN) throw NoPermissionException()
    }

    fun registerAccountUrl(username: String, accountUrl: String): User {
        val user = getUser(username)
        user.accountUrl = removeAmountQueryFromAccountUrl(accountUrl)

        return userRepository.save(user)
    }

    fun removeAmountQueryFromAccountUrl(accountUrl: String): String {
        return accountUrl.replace(Regex("amount=\\d+&"), "")
    }

    fun sendResetPasswordEmail(loginId: String, email: String) {
        val user = getUser(loginId)
        checkEmailAddress(user, email)

        emailService.sendResetPasswordEmail(email)
    }

    fun checkEmailAddress(user: User, email: String) {
        if (user.email != email) throw UserNotFoundException()
    }

    fun deleteUser(username: String): User {
        val user = getUser(username)
        userRepository.delete(user)
        return user
    }

    fun resetPassword(code: String, password: String) {
        val email = emailService.getEmailByCode(code) ?: throw UserNotFoundException()
        val user = userRepository.findByEmail(email) ?: throw UserNotFoundException()
        user.loginPassword = passwordEncoder.encode(password)
        userRepository.save(user)
        emailService.deleteResetPasswordCode(code)
    }
}