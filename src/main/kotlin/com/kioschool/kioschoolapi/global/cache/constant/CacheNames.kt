package com.kioschool.kioschoolapi.global.cache.constant

object CacheNames {
    const val WORKSPACES = "workspaces"
    const val PRODUCT_CATEGORIES = "product-categories"
    const val ORDERS = "orders"
    const val PRODUCTS = "products"
    const val REAL_TIME_STATISTICS = "realTimeStatistics"
    const val HISTORY_STATISTICS = "historyStatistics"
    const val INSIGHT_CARD = "insightCard"
    const val FESTIVAL_CALENDAR = "festivalCalendar"

    val ALL = listOf(WORKSPACES, PRODUCT_CATEGORIES, ORDERS, PRODUCTS, REAL_TIME_STATISTICS, HISTORY_STATISTICS, INSIGHT_CARD, FESTIVAL_CALENDAR)
}
