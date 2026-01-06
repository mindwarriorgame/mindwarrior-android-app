package com.mindwarrior.app.engine

import com.mindwarrior.app.NowProvider
import java.util.regex.Pattern

class Counter(dataSerialized: String?) {

    // Internal state (UTC by convention: epoch seconds)
    private var isActive: Boolean = false
    private var totalSecondsIntermediate: Long = 0L
    private var lastUpdatedEpochSeconds: Long = nowSeconds()

    init {
        if (!dataSerialized.isNullOrBlank()) {
            // Parse only the fields we care about; missing fields fall back to defaults.
            isActive = parseBoolean(dataSerialized, "is_active") ?: false
            totalSecondsIntermediate = parseLong(dataSerialized, "total_seconds_intermediate") ?: 0L
            lastUpdatedEpochSeconds = parseLong(dataSerialized, "last_updated_epoch_seconds") ?: nowSeconds()
        }
        refresh()
    }

    fun serialize(): String {
        refresh()
        // Simple, stable JSON you control.
        return """{"is_active":$isActive,"total_seconds_intermediate":$totalSecondsIntermediate,"last_updated_epoch_seconds":$lastUpdatedEpochSeconds}"""
    }

    fun reset(): Counter {
        isActive = false
        totalSecondsIntermediate = 0L
        lastUpdatedEpochSeconds = nowSeconds()
        refresh()
        return this
    }

    fun getTotalSeconds(): Long {
        refresh()
        return totalSecondsIntermediate
    }

    fun isActive(): Boolean {
        refresh()
        return isActive
    }

    fun resume(): Counter {
        refresh()
        isActive = true
        return this
    }

    fun pause(): Counter {
        refresh()
        isActive = false
        return this
    }

    fun refresh() {
        val now = nowSeconds()
        if (isActive) {
            val delta = (now - lastUpdatedEpochSeconds).coerceAtLeast(0L)
            totalSecondsIntermediate += delta
        }
        lastUpdatedEpochSeconds = now
    }

    fun moveTimeBack(nMinutes: Long) {
        lastUpdatedEpochSeconds -= nMinutes * 60L
    }

    private fun nowSeconds(): Long = NowProvider.nowMillis() / 1000L

    // --- Minimal JSON field extractors (flat object only) ---

    private fun parseBoolean(json: String, key: String): Boolean? {
        val p = Pattern.compile("\"$key\"\\s*:\\s*(true|false)")
        val m = p.matcher(json)
        return if (m.find()) m.group(1).toBoolean() else null
    }

    private fun parseLong(json: String, key: String): Long? {
        val p = Pattern.compile("\"$key\"\\s*:\\s*(-?\\d+)")
        val m = p.matcher(json)
        return if (m.find()) m.group(1).toLongOrNull() else null
    }
}
