package com.mindwarrior.app.engine

import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SleepUtilsTest {
    private var originalTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        originalTimeZone?.let { TimeZone.setDefault(it) }
    }

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

    @Test
    fun calculateNextSleepEventMillisReturnsNextStartWhenBeforeWindow() {
        val nowMillis = utcMillis(2024, 0, 10, 9, 30, 0)
        val start = 22 * 60
        val end = 6 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 22, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNextEndWhenInsideWindow() {
        val nowMillis = utcMillis(2024, 0, 10, 23, 30, 0)
        val start = 22 * 60
        val end = 6 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 11, 6, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNextStartAfterWindow() {
        val nowMillis = utcMillis(2024, 0, 10, 7, 30, 0)
        val start = 22 * 60
        val end = 6 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 22, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNextEndForSameDayWindow() {
        val nowMillis = utcMillis(2024, 0, 10, 14, 0, 0)
        val start = 9 * 60
        val end = 17 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 17, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNextStartForSameDayWindow() {
        val nowMillis = utcMillis(2024, 0, 10, 8, 0, 0)
        val start = 9 * 60
        val end = 17 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 9, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNowWhenAtStartBoundary() {
        val nowMillis = utcMillis(2024, 0, 10, 9, 0, 0)
        val start = 9 * 60
        val end = 17 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(nowMillis, next)
    }

    @Test
    fun calculateNextSleepEventMillisReturnsNowWhenAtEndBoundary() {
        val nowMillis = utcMillis(2024, 0, 10, 17, 0, 0)
        val start = 9 * 60
        val end = 17 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(nowMillis, next)
    }

    @Test
    fun calculateNextSleepEventMillisHandlesStartEqualsEnd() {
        val nowMillis = utcMillis(2024, 0, 10, 9, 30, 0)
        val start = 9 * 60
        val end = 9 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 11, 9, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisNormalizesMinutesOutsideDay() {
        val nowMillis = utcMillis(2024, 0, 10, 10, 0, 0)
        val start = 25 * 60
        val end = -1 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 23, 0, 0), next)
    }

    @Test
    fun calculateNextSleepEventMillisWhenNowIsALittleBitLater() {
        val nowMillis = utcMillis(2024, 0, 10, 10, 0, 0) + 1
        val start = 10 * 60
        val end = 11 * 60

        val next = SleepUtils.calculateNextSleepEventMillisAt(nowMillis, start, end)

        assertEquals(utcMillis(2024, 0, 10, 11, 0, 0), next)
    }

    private fun utcMillis(
        year: Int,
        monthZeroBased: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthZeroBased)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, second)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
