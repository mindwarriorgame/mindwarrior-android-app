package com.mindwarrior.app.badges

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {
    @Test
    fun testGenerateLevel01() {
        val levels = LevelGenerator.generateLevels(4)
        assertTrue(levels.isNotEmpty())
    }

    @Test
    fun testGenerateOver50LevelPickIndices() {
        val indices = LevelGenerator.generateOver50LevelPickIndices()
        assertEquals(100, indices.size)
    }

    @Test
    fun testFirstLevelsPickPrecannedValues() {
        var level = 0
        while (level < 6) {
            assertEquals(LevelGeneratorAccess.FIRST_LEVELS_0_1[level], LevelGenerator.getLevel(0, level))
            assertEquals(LevelGeneratorAccess.FIRST_LEVELS_0_1[level], LevelGenerator.getLevel(1, level))
            assertEquals(LevelGeneratorAccess.FIRST_LEVELS_2_3[level], LevelGenerator.getLevel(2, level))
            assertEquals(LevelGeneratorAccess.FIRST_LEVELS_2_3[level], LevelGenerator.getLevel(3, level))
            assertEquals(LevelGeneratorAccess.FIRST_LEVEL_4[level], LevelGenerator.getLevel(4, level))
            level += 1
        }
    }

    @Test
    fun testNextLevelsPickGeneratedValues() {
        var level = 6
        while (level < 50) {
            for (difficulty in 0..4) {
                val generated = LevelGenerator.getLevel(difficulty, level)
                val expected = when (difficulty) {
                    0, 1 -> getExpectedPrefix(level, LevelGeneratorAccess.NEXT_LEVELS_0_1)
                    2, 3 -> getExpectedPrefix(level, LevelGeneratorAccess.NEXT_LEVELS_2_3)
                    else -> getExpectedPrefix(level, LevelGeneratorAccess.NEXT_LEVELS_4)
                }
                assertEquals(expected, generated.take(expected.size))
                for (idx in expected.size until generated.size) {
                    assertEquals("c0", generated[idx])
                }
            }
            level += 1
        }
    }

    @Test
    fun testLevelsOver50ArePickedDeterministically() {
        assertEquals(
            LevelGenerator.getLevel(0, 499),
            LevelGenerator.getLevel(0, 499)
        )
    }

    private fun getExpectedPrefix(level: Int, nextLevels: List<List<String>>): List<String> {
        return nextLevels[level - 6]
    }

    private object LevelGeneratorAccess {
        val FIRST_LEVELS_0_1: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("FIRST_LEVELS_0_1")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>

        val FIRST_LEVELS_2_3: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("FIRST_LEVELS_2_3")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>

        val FIRST_LEVEL_4: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("FIRST_LEVEL_4")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>

        val NEXT_LEVELS_0_1: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("NEXT_LEVELS_0_1")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>

        val NEXT_LEVELS_2_3: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("NEXT_LEVELS_2_3")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>

        val NEXT_LEVELS_4: List<List<String>> = LevelGenerator::class
            .java
            .getDeclaredField("NEXT_LEVELS_4")
            .apply { isAccessible = true }
            .get(LevelGenerator) as List<List<String>>
    }
}
