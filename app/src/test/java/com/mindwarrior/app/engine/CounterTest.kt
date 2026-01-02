package com.mindwarrior.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterTest {

    @Test
    fun defaultsStartPausedWithZeroSeconds() {
        val counter = Counter(null)

        assertFalse(counter.isActive())
        assertEquals(0L, counter.getTotalSeconds())
    }

    @Test
    fun resumeAccumulatesTimeWhenRefreshed() {
        val counter = Counter(null).resume()

        counter.moveTimeBack(2)
        val totalSeconds = counter.getTotalSeconds()

        assertTrue("Expected at least 120s, got $totalSeconds", totalSeconds >= 120L)
        assertTrue("Expected under 125s, got $totalSeconds", totalSeconds < 125L)
    }

    @Test
    fun pauseStopsAccumulation() {
        val counter = Counter(null).resume()

        counter.moveTimeBack(1)
        counter.pause()
        val pausedTotal = counter.getTotalSeconds()

        counter.moveTimeBack(1)
        val afterPauseTotal = counter.getTotalSeconds()

        assertEquals(pausedTotal, afterPauseTotal)
    }

    @Test
    fun serializeRestoresStateWhenPaused() {
        val counter = Counter(null).resume()

        counter.moveTimeBack(1)
        counter.pause()
        val serialized = counter.serialize()

        val restored = Counter(serialized)

        assertFalse(restored.isActive())
        assertEquals(counter.getTotalSeconds(), restored.getTotalSeconds())
    }
}
