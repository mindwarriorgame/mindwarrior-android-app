package com.mindwarrior.app.badges

data class BoardCell(
    val badge: String,
    val isActive: Boolean = false,
    val isLastModified: Boolean = false
)

data class BadgeAdvice(
    val badge: String?,
    val state: String?
)

data class BadgeProgress(
    val badge: String,
    val challenge: String,
    val progressPct: Int,
    val remainingTimeSecs: Long? = null,
    val remainingReviews: Int? = null
)
