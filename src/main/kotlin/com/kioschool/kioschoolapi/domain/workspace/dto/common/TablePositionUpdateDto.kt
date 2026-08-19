package com.kioschool.kioschoolapi.domain.workspace.dto.common

// 벌크 위치 저장 요청의 항목 하나. position이 null이면 해당 테이블의 배치를 취소한다.
data class TablePositionUpdateDto(
    val tableId: Long,
    val position: TablePositionDto?,
)
