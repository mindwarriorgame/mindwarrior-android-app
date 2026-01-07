package com.mindwarrior.app.badges

import kotlin.random.Random

object LevelGenerator {
    private val FIRST_LEVELS_0_1 = listOf(
        listOf("f0", "s0", "c0"),
        listOf("s1", "t0", "c0", "c0"),
        listOf("s0", "s1", "c0"),
        listOf("s0", "s1", "t0", "c0", "c0"),
        listOf("c0", "c1", "f0"),
        listOf("f0", "s2", "c2", "c0", "c0")
    )

    private val FIRST_LEVELS_2_3 = listOf(
        listOf("f0", "s0", "s1", "c0"),
        listOf("s1", "t0", "c0", "c1", "c0"),
        listOf("s0", "s1", "s2", "f0", "f0", "c0"),
        listOf("s0", "s1", "t0", "t0", "c0", "c0", "c0", "c0"),
        listOf("c0", "c1", "f0", "c0", "t0"),
        listOf("f0", "s2", "t0", "c1", "c2", "c0", "c0")
    )

    private val FIRST_LEVEL_4 = listOf(
        listOf("f0", "s0", "s1", "s2", "c0"),
        listOf("s1", "s2", "t0", "c0", "c1", "c0"),
        listOf("s0", "s1", "s2", "c1", "f0", "f0", "c0"),
        listOf("s0", "s1", "t0", "c2", "c0", "c0", "c0", "c0"),
        listOf("c0", "c1", "c2", "s2", "f0", "c0", "t0"),
        listOf("f0", "s2", "t0", "c1", "c2", "c2", "c0", "c0")
    )

    private val NEXT_LEVELS_0_1 = listOf(
        listOf("f0", "s1", "c1", "c1", "s2", "t0", "s0", "s0", "t0"),
        listOf("f0", "t0", "t0", "c0", "c0", "s0", "s2", "s1", "s0"),
        listOf("s2", "c2", "c0", "s0", "f0", "t0", "t0", "s2", "t0", "c2"),
        listOf("t0", "c0", "s0", "c1", "s2", "s0"),
        listOf("s2", "c0", "c2", "t0", "c2", "s1", "s1"),
        listOf("s0", "c0", "s0", "s0", "s0", "s1"),
        listOf("s2", "c2", "f0", "c0", "s1", "t0", "t0", "c2", "s1"),
        listOf("f0", "s0", "t0", "f0", "c2", "s2", "s0", "s2"),
        listOf("c1", "s1", "c2", "c2", "t0", "c2", "c0", "f0"),
        listOf("c2", "t0", "c1", "c0", "f0", "s2", "s0", "c0", "f0"),
        listOf("s1", "s1", "s2", "f0", "c2", "s0"),
        listOf("t0", "c2", "t0", "c1", "c1", "s1", "c0", "c1", "c0", "c2"),
        listOf("s1", "s0", "s0", "c2", "s0", "s0"),
        listOf("f0", "s0", "s0", "f0", "c2", "c2", "c2", "s0", "c2", "c1"),
        listOf("c1", "s1", "s0", "s2", "s1", "s2", "c0", "s1", "s1", "s1"),
        listOf("t0", "t0", "c0", "s2", "c2"),
        listOf("s0", "c1", "c1", "c0", "s1", "f0", "s1", "c1"),
        listOf("c0", "s0", "c2", "s0", "t0"),
        listOf("s0", "c2", "s1", "s0", "c0", "c0"),
        listOf("c1", "s2", "s1", "c2", "c1", "s2", "f0"),
        listOf("c1", "s0", "s1", "s0", "t0", "c2", "t0"),
        listOf("c0", "s2", "c2", "c2", "c0", "f0", "c2", "f0", "t0", "c0"),
        listOf("c1", "c0", "s2", "c2", "f0"),
        listOf("f0", "s1", "s0", "s0", "c2", "c2", "f0", "s2", "c1"),
        listOf("s0", "s2", "s1", "s2", "c2", "c0", "c1", "s2", "s1", "s1"),
        listOf("s2", "c1", "s1", "c0", "s1", "s2", "s1", "t0", "f0"),
        listOf("c2", "s2", "c1", "c1", "t0", "s2", "c2", "s2"),
        listOf("s0", "s2", "s1", "c1", "f0", "s2", "t0"),
        listOf("f0", "s2", "c2", "c1", "s1", "f0", "s1", "c2"),
        listOf("c1", "s0", "c2", "c1", "s1", "c0", "c0"),
        listOf("s0", "c0", "s1", "c1", "s2", "f0"),
        listOf("c0", "c1", "s2", "s0", "f0", "s1", "s2"),
        listOf("f0", "t0", "c2", "c1", "t0", "c0", "s2", "f0", "c0", "s1"),
        listOf("s2", "c2", "t0", "c2", "c0", "c2", "s0", "c2", "s1", "s2"),
        listOf("s0", "s0", "c1", "f0", "c1", "s2", "s1", "s1"),
        listOf("c1", "t0", "t0", "f0", "c1"),
        listOf("c1", "s0", "s1", "s0", "s1"),
        listOf("t0", "s0", "s0", "s0", "f0", "c2"),
        listOf("s0", "s1", "c2", "f0", "c1", "c1", "c2", "c2", "c1", "s0"),
        listOf("s0", "f0", "s1", "s0", "s2", "s0"),
        listOf("s2", "s0", "c1", "t0", "f0", "s0", "s2", "t0", "s2"),
        listOf("c2", "s2", "t0", "t0", "s2", "s1", "c2"),
        listOf("c1", "f0", "s1", "s0", "s0", "s2", "c0", "s2", "s1", "c1"),
        listOf("f0", "s1", "s0", "s1", "s1", "c0", "c1", "c0", "s0"),
        listOf("s1", "c1", "t0", "c2", "c1", "s0", "t0", "f0", "c0", "t0")
    )

    private val NEXT_LEVELS_2_3 = listOf(
        listOf("s2", "f0", "c0", "c1", "s0", "s1", "s2", "c0", "s0", "f0", "s2", "s1", "c1"),
        listOf("s1", "c2", "t0", "c1", "f0", "c1", "f0", "s1", "c2", "f0", "c1"),
        listOf("c1", "c1", "f0", "c1", "t0", "s0", "c2", "s1", "t0", "c1"),
        listOf("t0", "s0", "c1", "s2", "s1", "c2", "c2", "c2", "s1", "c2", "t0"),
        listOf("s0", "s0", "s0", "s1", "s0", "t0", "c1", "s2", "s1", "t0", "s1", "c1", "s0"),
        listOf("c1", "s1", "c2", "c2", "s1", "s2", "t0", "t0"),
        listOf("c2", "s0", "t0", "s1", "s2", "f0", "t0", "c1"),
        listOf("t0", "s1", "t0", "c2", "s2", "c0", "s0", "t0", "c1", "s0", "s0", "c2", "c1"),
        listOf("t0", "f0", "t0", "t0", "c2", "t0", "c0", "s2", "t0", "c2", "s1", "s2"),
        listOf("s1", "s0", "s1", "s2", "c2", "c0", "s1", "f0", "c0", "t0", "s1"),
        listOf("s2", "c1", "c2", "t0", "s1", "f0", "t0"),
        listOf("s1", "t0", "c0", "s0", "c1", "s0", "c0", "s2", "t0"),
        listOf("t0", "t0", "s1", "s1", "c0", "c2", "s1", "c1"),
        listOf("c1", "t0", "c2", "f0", "c2", "s0", "s0", "s1"),
        listOf("s1", "s1", "f0", "c0", "c0", "s2", "t0", "f0", "f0", "s0", "c1", "c0", "s2"),
        listOf("t0", "c0", "s1", "c0", "c2", "c2", "s0", "t0", "s1"),
        listOf("c1", "c1", "f0", "s1", "s0", "f0", "c2", "s2"),
        listOf("s0", "c2", "c1", "s1", "f0", "s1", "s1", "c2", "s1", "s2", "s1", "t0"),
        listOf("c2", "s1", "s1", "s0", "c1", "s2", "f0", "s1", "f0"),
        listOf("s2", "s1", "s2", "s0", "s0", "s2", "c1", "c2", "c2", "s1"),
        listOf("s2", "c2", "c2", "c0", "c0", "s2", "f0"),
        listOf("c2", "f0", "c0", "s2", "f0", "c1", "f0", "c0", "c0", "c1", "c1"),
        listOf("t0", "c2", "c2", "f0", "f0", "c2", "t0"),
        listOf("c1", "s0", "s2", "s1", "s1", "t0", "c2", "s0", "c0", "s1", "s0", "t0"),
        listOf("c2", "f0", "c1", "s0", "s2", "f0", "s2", "s0", "f0", "c1", "f0", "c1"),
        listOf("c2", "s0", "c1", "c0", "s1", "t0", "f0", "s2", "c1", "c0", "s1", "t0"),
        listOf("c0", "c2", "t0", "t0", "s0", "s2", "c0", "c0"),
        listOf("t0", "c0", "t0", "c2", "s0", "s0", "s0", "c1", "c1", "s2"),
        listOf("s2", "c0", "f0", "c1", "s0", "c1", "c2", "s0", "c1", "c2"),
        listOf("c0", "s0", "s1", "f0", "c1", "t0", "s2", "s0"),
        listOf("c0", "c0", "c0", "c1", "c1", "f0", "f0", "c1", "c2", "s1", "c1", "c0", "c0"),
        listOf("c2", "t0", "c2", "c2", "s0", "t0", "f0"),
        listOf("s2", "c0", "s1", "t0", "s1", "f0", "c2"),
        listOf("s1", "t0", "c0", "c2", "f0", "c0", "c1"),
        listOf("s1", "t0", "s1", "c0", "c2", "c0", "f0", "c2", "c1", "s0", "c0", "f0"),
        listOf("c1", "f0", "c1", "t0", "s0", "c2", "f0", "s0", "s2", "c0", "s1", "s1", "f0"),
        listOf("c0", "s0", "c0", "f0", "c2", "s0", "c0", "s1", "s1"),
        listOf("f0", "c1", "s1", "t0", "s1", "f0", "s1", "t0", "s2", "c1"),
        listOf("c1", "s1", "t0", "s1", "t0", "c1", "s2", "s2", "c2", "c2", "c2", "c1"),
        listOf("c0", "c1", "t0", "s2", "s2", "s0", "s0", "f0", "s2", "c2", "c0"),
        listOf("c0", "c2", "c0", "s0", "t0", "c2", "s2", "c2"),
        listOf("s0", "s2", "s1", "s1", "c0", "f0", "f0", "c0", "t0", "f0", "c2", "s0"),
        listOf("t0", "s1", "c1", "s2", "c0", "s0", "c2", "s0", "c0", "c0", "c0", "s2", "t0"),
        listOf("c0", "s1", "t0", "c1", "c0", "s0", "s2"),
        listOf("s1", "c1", "c1", "c2", "s2", "s0", "c0", "f0", "c1", "s0", "t0")
    )

    private val NEXT_LEVELS_4 = listOf(
        listOf("t0", "c1", "t0", "f0", "f0", "s2", "t0", "c1", "s1", "c0", "c0", "t0", "s0", "c2", "s2"),
        listOf("s0", "c2", "s2", "c0", "s2", "s0", "t0", "c2", "t0", "c0", "c1", "s2"),
        listOf("s0", "t0", "s1", "c0", "c1", "c1", "s2", "f0", "t0", "t0", "s1", "c1", "c1"),
        listOf("c1", "t0", "c2", "c2", "s1", "s0", "s0", "c0", "t0", "t0", "s1", "s1"),
        listOf("f0", "t0", "c2", "s1", "s0", "c0", "c0", "s2", "t0", "f0", "t0"),
        listOf("c1", "s2", "s1", "c1", "c1", "s0", "s2", "s1", "s2"),
        listOf("t0", "s1", "s0", "s1", "s0", "s1", "s0", "s1", "s0", "c1"),
        listOf("c2", "f0", "c0", "f0", "t0", "c1", "s2", "f0", "f0", "t0"),
        listOf("t0", "s2", "c0", "s2", "c2", "c2", "c2", "s0", "s1", "t0", "s2", "c2", "s0", "c2", "c2"),
        listOf("s0", "s2", "c0", "c0", "s2", "c1", "s2", "s2", "t0"),
        listOf("s1", "s0", "f0", "t0", "f0", "c1", "c2", "s0", "f0", "s1"),
        listOf("s1", "c0", "t0", "c2", "c1", "s0", "c0", "s1", "s1", "c1", "c1", "s0", "s0"),
        listOf("s2", "s0", "c2", "t0", "s2", "s1", "f0", "c1", "s2", "c1"),
        listOf("s2", "s2", "s2", "s1", "t0", "t0", "f0", "s1", "s2"),
        listOf("c2", "c2", "c1", "c0", "c2", "s2", "c1", "c2", "s2"),
        listOf("c0", "s0", "s1", "f0", "c1", "c1", "c0", "c1", "c2", "c0", "s1", "c2", "c1"),
        listOf("c0", "t0", "s1", "c2", "c0", "c0", "c0", "c0", "c0", "c0", "s1", "f0", "s1"),
        listOf("f0", "c1", "c1", "s1", "s2", "s0", "c2", "f0", "c2", "s2", "s0", "c0", "t0", "c1", "c2"),
        listOf("c0", "c2", "c0", "f0", "t0", "s0", "s0", "s0", "t0", "c2", "s1"),
        listOf("t0", "c2", "c2", "f0", "t0", "c1", "t0", "c0", "c1", "s2", "c2", "s2"),
        listOf("c0", "c2", "c0", "t0", "c0", "s1", "s0", "c0", "s1"),
        listOf("s2", "c2", "s2", "t0", "c1", "c2", "s0", "f0", "s0", "c0", "t0", "s2", "f0", "f0", "s1"),
        listOf("f0", "c1", "f0", "s0", "s2", "c1", "s2", "c1", "s0", "c2", "s2", "s2"),
        listOf("s0", "c0", "c0", "s0", "s1", "s1", "c2", "s0", "c1", "f0", "s0", "s0", "s2", "c1", "c0"),
        listOf("c0", "f0", "c2", "s1", "f0", "s0", "c1", "c0", "c0", "t0", "c1", "t0"),
        listOf("c2", "c1", "c0", "c0", "s0", "s1", "c1", "f0", "c1", "t0"),
        listOf("c1", "s0", "c1", "s0", "c2", "s2", "c0", "s1", "s1", "f0", "s0", "c1", "s1", "c0"),
        listOf("c2", "f0", "s1", "c2", "c1", "f0", "c0", "s0", "s1", "c2"),
        listOf("t0", "c2", "c0", "c1", "f0", "s1", "c1", "f0", "s0", "t0", "s2", "f0", "c0", "s1", "s0"),
        listOf("s2", "t0", "s2", "c1", "s1", "c1", "c2", "s2", "s0", "c0", "s1", "c1", "c0", "c0", "c0"),
        listOf("t0", "s2", "c2", "s0", "c1", "s1", "f0", "s0", "s0", "s0"),
        listOf("c1", "c1", "t0", "f0", "c1", "s1", "s2", "c1", "f0"),
        listOf("c0", "s2", "s1", "t0", "s2", "s0", "t0", "f0", "c2", "c2"),
        listOf("c2", "t0", "c2", "f0", "s1", "s2", "t0", "f0", "s1", "c2", "c1", "s0"),
        listOf("c0", "t0", "c1", "f0", "s0", "c2", "t0", "c1", "c0", "s2", "c2", "s0", "c0", "t0", "t0"),
        listOf("s1", "s0", "c2", "s2", "c0", "c0", "s1", "c0", "c1", "s2", "c0", "t0", "s1", "c1"),
        listOf("c2", "c0", "c2", "t0", "c2", "f0", "s0", "f0", "s2", "s0", "s0"),
        listOf("c2", "s2", "s0", "c2", "t0", "c2", "s0", "s0", "s1", "f0", "s1", "c2", "c2", "s2", "s1"),
        listOf("c1", "s2", "c1", "s2", "s0", "s2", "s2", "s1", "s1", "c0", "c1", "c0", "c1", "s1", "c0"),
        listOf("t0", "s0", "c0", "s0", "c2", "s2", "f0", "c2", "c2", "f0", "s1", "c1", "s2"),
        listOf("s0", "s1", "s1", "c1", "f0", "f0", "c0", "s1", "s2", "s1", "t0", "c1"),
        listOf("t0", "s1", "s1", "t0", "s1", "f0", "c0", "c1", "s0", "s0", "t0"),
        listOf("s2", "s2", "c0", "f0", "c1", "c0", "s2", "c2", "t0", "s2", "f0"),
        listOf("c2", "c1", "s2", "c1", "s1", "t0", "c1", "s1", "c1", "t0", "c0"),
        listOf("t0", "c2", "t0", "f0", "c0", "s0", "f0", "s0", "s1")
    )

    private val NEXT_LEVEL_OVER_50_PICK_INDICES = listOf(
        371, 167, 279, 723, 166, 799, 215, 14, 81, 865, 363, 59, 9, 786, 443, 650, 69, 30, 1004,
        107, 207, 653, 134, 360, 401, 884, 85, 317, 303, 126, 320, 263, 442, 165, 5, 492, 829,
        788, 127, 225, 324, 783, 560, 41, 741, 317, 541, 65, 984, 176, 930, 55, 444, 251, 276,
        61, 943, 128, 591, 957, 589, 102, 745, 721, 609, 535, 938, 573, 670, 699, 946, 62, 19,
        820, 432, 584, 111, 249, 707, 669, 90, 706, 67, 393, 419, 1019, 786, 448, 256, 1, 234,
        535, 178, 647, 389, 790, 50, 0, 620, 637
    )

    private fun countC0s(level: List<String>): Int {
        return level.count { it == "c0" }
    }

    fun getLevel(difficulty: Int, levelStartingZero: Int): List<String> {
        if (levelStartingZero < FIRST_LEVELS_0_1.size) {
            return if (difficulty < 2) {
                FIRST_LEVELS_0_1[levelStartingZero]
            } else if (difficulty < 4) {
                FIRST_LEVELS_2_3[levelStartingZero]
            } else {
                FIRST_LEVEL_4[levelStartingZero]
            }
        }

        val shiftedLevel = levelStartingZero - FIRST_LEVELS_0_1.size
        val nextLevels = if (difficulty < 2) {
            NEXT_LEVELS_0_1
        } else if (difficulty < 4) {
            NEXT_LEVELS_2_3
        } else {
            NEXT_LEVELS_4
        }

        val levelTemplate = if (shiftedLevel < nextLevels.size) {
            nextLevels[shiftedLevel]
        } else {
            val pickIndex = NEXT_LEVEL_OVER_50_PICK_INDICES[
                shiftedLevel % NEXT_LEVEL_OVER_50_PICK_INDICES.size
            ]
            nextLevels[pickIndex % nextLevels.size]
        }

        val level = levelTemplate.toMutableList()
        val minC0 = listOf(0, 1, 2, 3, 4)
        var countC0sInLevel = countC0s(level)
        val requiredC0s = minC0[difficulty] + Random.nextInt(0, 2)
        while (countC0sInLevel < requiredC0s) {
            level.add("c0")
            countC0sInLevel += 1
        }
        return level
    }
}
