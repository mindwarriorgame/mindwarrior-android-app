package com.mindwarrior.app.viewmodel

data class LogEntry(
    val timestamp: Long,
    val relativeTime: String,
    val absoluteTime: String,
    val message: String
)
