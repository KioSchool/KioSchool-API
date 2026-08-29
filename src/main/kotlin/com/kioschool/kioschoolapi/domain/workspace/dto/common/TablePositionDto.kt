package com.kioschool.kioschoolapi.domain.workspace.dto.common

data class TablePositionDto(val x: Int, val y: Int) {
    companion object {
        // x와 y는 항상 같이 null이거나 같이 값이다. 한쪽만 있는 상태는 미배치로 취급한다.
        fun of(x: Int?, y: Int?) = if (x != null && y != null) TablePositionDto(x, y) else null
    }
}
