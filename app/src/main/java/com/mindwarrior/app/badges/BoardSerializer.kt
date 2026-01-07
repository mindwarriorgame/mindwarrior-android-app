package com.mindwarrior.app.badges

object BoardSerializer {
    private val badgeProgressKey = mapOf(
        "c1" to "remaining_time_secs",
        "c2" to "remaining_time_secs",
        "f0" to "remaining_time_secs",
        "t0" to "remaining_time_secs",
        "s0" to "remaining_reviews",
        "s1" to "remaining_reviews",
        "s2" to "remaining_reviews",
        "c0" to "remaining_reviews"
    )

    fun serializeCell(cell: BoardCell): String {
        val sb = StringBuilder(cell.badge)
        if (cell.isActive == true) {
            sb.append("a")
        }
        if (cell.isLastModified == true) {
            sb.append("m")
        }
        return sb.toString()
    }

    fun serializeBoard(board: List<BoardCell>): String {
        return board.joinToString("_") { serializeCell(it) }
    }

    fun serializeProgress(progress: Map<String, BadgeProgress>): String {
        val serializedItems = mutableListOf<String>()
        for ((badge, item) in progress) {
            val key = badgeProgressKey[badge]
                ?: throw IllegalArgumentException("Unknown badge: $badge")
            val value = when (key) {
                "remaining_time_secs" -> item.remainingTimeSecs
                "remaining_reviews" -> item.remainingReviews?.toLong()
                else -> null
            } ?: 0L
            val chunk = "$badge" +
                "_$value" +
                "_${item.progressPct}"
            serializedItems.add(chunk)
        }
        return serializedItems.joinToString("--")
    }
}
