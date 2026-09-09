package com.kioschool.kioschoolapi.domain.user.dto.admin

/**
 * 설문을 이미 물어봤는지만 알려준다. 응답 내용은 담지 않는다 —
 * 클라이언트는 설문 화면을 띄울지만 판단하면 되고, 저장된 값은 다시 보여주지 않는다.
 */
data class AcquisitionSurveyStatusResponse(
    val isAnswered: Boolean
)
