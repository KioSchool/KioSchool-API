package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

data class UpdateIsOnboardingRequestBody(
    override val workspaceId: Long,
    val isOnboarding: Boolean
) : WorkspaceAware
