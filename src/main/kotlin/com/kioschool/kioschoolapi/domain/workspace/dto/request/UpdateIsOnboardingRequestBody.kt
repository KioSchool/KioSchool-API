package com.kioschool.kioschoolapi.domain.workspace.dto.request

data class UpdateIsOnboardingRequestBody(
    val workspaceId: Long,
    val isOnboarding: Boolean
)
