package com.kioschool.kioschoolapi.factory

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.entity.User
import io.kotest.core.listeners.BeforeSpecListener
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

object AuthenticationSample : BeforeSpecListener {
    private var user = SampleEntity.user
    private var userDetails = CustomUserDetails(user)

    fun setUser(user: User) {
        this.user = user
        userDetails = CustomUserDetails(user)
    }

    override suspend fun beforeSpec(spec: io.kotest.core.spec.Spec) {
        val context = SecurityContextHolder.getContext()
        context.authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )
    }
}