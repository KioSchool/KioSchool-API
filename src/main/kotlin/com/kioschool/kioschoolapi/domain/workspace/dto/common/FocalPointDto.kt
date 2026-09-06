package com.kioschool.kioschoolapi.domain.workspace.dto.common

/**
 * 사진에서 항상 화면에 남아야 할 지점을 원본 대비 퍼센트로 나타낸다.
 * 프론트는 이 값을 CSS object-position으로 적용한다.
 */
data class FocalPointDto(val x: Int, val y: Int) {
    fun isInRange() = x in MIN..MAX && y in MIN..MAX

    companion object {
        const val MIN = 0
        const val MAX = 100
        const val CENTER_VALUE = 50

        // 지정된 초점이 없을 때의 기본값. 초점 도입 이전의 정중앙 크롭과 같은 결과를 낸다.
        val CENTER = FocalPointDto(CENTER_VALUE, CENTER_VALUE)
    }
}
