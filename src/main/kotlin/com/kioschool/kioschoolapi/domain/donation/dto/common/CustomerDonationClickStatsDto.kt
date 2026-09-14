package com.kioschool.kioschoolapi.domain.donation.dto.common

data class CustomerDonationClickStatsDto(
    val startDate: String,
    val endDate: String,
    val summary: Summary,
    val daily: List<DailyPoint>,
    val byAmount: List<Bucket>,
    val byMethod: List<Bucket>,
    val byNoteIndex: List<Bucket>,
    val topWorkspaces: List<WorkspaceItem>
) {
    data class Summary(
        val totalClicks: Long,
        // 계좌 복사 후 토스로 다시 누르면 한 주문에 클릭이 여러 건 쌓인다.
        val uniqueOrders: Long,
        val clickedAmountSum: Long,
        val averageAmount: Long,
        val ordersInRange: Long,
        // 모달은 24시간 스누즈·후원 완료 시 다시 뜨지 않으므로 실제 노출 대비 비율보다 낮게 나온다.
        val clickRatePerOrder: Double,
        val depositedClicks: Long,
        val depositedOrders: Long,
        val depositAmountSum: Long,
        // 입금 확인된 주문 ÷ 클릭한 고유 주문. 슈퍼어드민이 체크하지 않은 입금은 빠진다.
        val depositRatePerOrder: Double
    )

    data class DailyPoint(
        val date: String,
        val clicks: Long,
        val uniqueOrders: Long,
        val amountSum: Long,
        val depositAmountSum: Long
    )

    data class Bucket(
        val key: String?,
        val clicks: Long,
        val ratio: Double
    )

    data class WorkspaceItem(
        val workspaceId: Long,
        val workspaceName: String?,
        val clicks: Long,
        val uniqueOrders: Long,
        val amountSum: Long,
        val depositAmountSum: Long
    )
}
