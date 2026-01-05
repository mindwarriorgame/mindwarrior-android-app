package com.mindwarrior.app

object TimeHelperObject {
    private val overrideMillis = ThreadLocal<Long?>()

    fun currentTimeMillis(): Long {
        return overrideMillis.get() ?: System.currentTimeMillis()
    }

    fun <T> withCurrentMillis(customMillis: Long, block: () -> T): T {
        val previous = overrideMillis.get()
        overrideMillis.set(customMillis)
        return try {
            block()
        } finally {
            overrideMillis.set(previous)
        }
    }
}
