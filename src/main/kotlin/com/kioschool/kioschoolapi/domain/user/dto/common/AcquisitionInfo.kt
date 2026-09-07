package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel

data class AcquisitionInfo(
    val channel: AcquisitionChannel?,
    val channelEtc: String?,
    val context: String?
) {
    // 클라이언트가 보낸 조합을 그대로 믿지 않는다. ETC가 아닌 행에 자유입력이 섞이면 집계 해석이 흔들린다.
    fun normalized(): AcquisitionInfo {
        val trimmedEtc = channelEtc?.trim()?.takeIf { it.isNotEmpty() }
        if (channel != AcquisitionChannel.ETC) return copy(channelEtc = null)
        return copy(channelEtc = trimmedEtc)
    }

    companion object {
        val EMPTY = AcquisitionInfo(null, null, null)
    }
}
