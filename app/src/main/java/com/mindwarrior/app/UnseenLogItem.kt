package com.mindwarrior.app

data class UnseenLogItem(
    val rawMessage: String,
    val translatedMessage: String,
    val timestampMillis: Long
)
