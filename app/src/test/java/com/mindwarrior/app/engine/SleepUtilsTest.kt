package com.mindwarrior.app.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepUtilsTest {

    @Test
    fun isNowInsideSleepIntervalReturnsTrueForWindowAroundNow() {
        val nowMinutes = 9 * 60 + 30
        val start = 9 * 60 + 29
        val end = 9 * 60 + 31

        assertTrue(SleepUtils.isInsideSleepInterval(nowMinutes, start, end))
    }

    @Test
    fun isNowInsideSleepIntervalReturnsFalseForFutureWindow() {
        val nowMinutes = 9 * 60 + 30
        val start = 9 * 60 + 31
        val end = 9 * 60 + 32

        assertFalse(SleepUtils.isInsideSleepInterval(nowMinutes, start, end))
    }

    @Test
    fun isNowInsideSleepIntervalHandlesWrapAroundTrue() {
        val nowMinutes = 3 * 60 + 30
        val start = 8 * 60 + 20
        val end = 6 * 60 + 40

        assertTrue(SleepUtils.isInsideSleepInterval(nowMinutes, start, end))
    }

    @Test
    fun isNowInsideSleepIntervalHandlesWrapAroundFalse() {
        val nowMinutes = 9 * 60 + 30
        val start = 10 * 60
        val end = 8 * 60 + 20

        assertFalse(SleepUtils.isInsideSleepInterval(nowMinutes, start, end))
    }

    @Test
    fun isNowInsideSleepIntervalReturnsFalseWhenStartEqualsEnd() {
        val nowMinutes = 9 * 60 + 30

        assertFalse(SleepUtils.isInsideSleepInterval(nowMinutes, nowMinutes, nowMinutes))
    }

}
