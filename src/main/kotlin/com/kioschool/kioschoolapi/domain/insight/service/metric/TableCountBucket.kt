package com.kioschool.kioschoolapi.domain.insight.service.metric

enum class TableCountBucket(val label: String) {
    XS("XS"), S("S"), M("M"), L("L");

    companion object {
        fun resolve(tableCount: Int, edges: List<Int>): TableCountBucket {
            // edges = [3, 6, 10] → 1-3=XS, 4-6=S, 7-10=M, 11+=L
            val sorted = edges.sorted()
            return when {
                tableCount <= sorted[0] -> XS
                tableCount <= sorted[1] -> S
                tableCount <= sorted[2] -> M
                else -> L
            }
        }
    }
}
