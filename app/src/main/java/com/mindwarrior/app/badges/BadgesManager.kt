package com.mindwarrior.app.badges

import com.mindwarrior.app.badges.counters.CatBadgeCounter
import com.mindwarrior.app.badges.counters.FeatherBadgeCounter
import com.mindwarrior.app.badges.counters.StarBadgeCounter
import com.mindwarrior.app.badges.counters.TimeBadgeCounter
import org.json.JSONArray
import org.json.JSONObject

class BadgesManager(
    private val difficulty: Int,
    badgesSerialized: String?
) {
    private val counters = listOf(
        CatBadgeCounter(),
        TimeBadgeCounter(),
        StarBadgeCounter(),
        FeatherBadgeCounter()
    )

    private val data: UserBadgesData = if (badgesSerialized.isNullOrBlank()) {
        UserBadgesData(
            badgesState = mutableMapOf(),
            board = levelBadgesToNewBoard(LevelGenerator.getLevel(difficulty, 0)).toMutableList(),
            level = 0,
            c0Hp = 0,
            c0HpNextDelta = 3,
            lastBadge = null,
            lastBadgeAt = null,
            c0ActiveTimePenalty = 0L,
            c0LockStartedAt = 0L
        )
    } else {
        fromJson(badgesSerialized)
    }.also { ensureDefaults(it) }

    private fun ensureDefaults(data: UserBadgesData) {
        if (data.badgesState.isEmpty()) {
            data.badgesState = mutableMapOf()
        }
        if (data.level < 0) {
            data.level = 0
        }
        if (data.board.isEmpty()) {
            val levelBadges = LevelGenerator.getLevel(difficulty, data.level)
            data.board = levelBadgesToNewBoard(levelBadges).toMutableList()
        }
        if (data.c0HpNextDelta <= 0) {
            data.c0HpNextDelta = 3
        }
        if (data.c0Hp < 0) {
            data.c0Hp = 0
        }
        if (data.c0Hp == 0) {
            for (cell in data.board) {
                if (cell.badge == "c0" && cell.isActive == true) {
                    data.c0Hp = maxGrumpyCatHealthpoints()
                }
            }
        }
        if (data.c0LockStartedAt < 0) {
            data.c0LockStartedAt = 0L
        }
        if (data.c0ActiveTimePenalty < 0) {
            data.c0ActiveTimePenalty = 0L
        }
    }

    private fun maxGrumpyCatHealthpoints(): Int {
        return 5 * (difficulty + 1)
    }

    fun onGameStarted(activePlayTimeSecs: Long): String? {
        data.c0HpNextDelta = 3
        data.c0Hp = 0
        data.c0LockStartedAt = 0
        data.c0ActiveTimePenalty = 0
        return chainBadgeCounters("onGameStarted", activePlayTimeSecs, false)
    }

    fun onFormulaUpdated(activePlayTimeSecs: Long): String? {
        return chainBadgeCounters("onFormulaUpdated", activePlayTimeSecs, true)
    }

    fun onPrompt(activePlayTimeSecs: Long): String? {
        data.c0HpNextDelta = 2
        return chainBadgeCounters("onPrompt", activePlayTimeSecs, false)
    }

    fun onShooCat(activePlayTimeSecs: Long): String {
        if (countActiveGrumpyCatsOnBoard() < 1) {
            throw IllegalStateException("Should never happen")
        }

        data.board = expelGrumpyCat(data.board).toMutableList()
        if (countActiveGrumpyCatsOnBoard() > 0) {
            data.c0Hp = maxGrumpyCatHealthpoints()
        } else {
            data.c0ActiveTimePenalty += (activePlayTimeSecs - data.c0LockStartedAt)
        }

        data.lastBadge = "c0_removed"
        return data.lastBadge ?: "c0_removed"
    }

    fun onForceBadgeOpen(activePlayTimeSecs: Long): String {
        if (countActiveGrumpyCatsOnBoard() > 0) {
            throw IllegalStateException("Cannot open badge while there's a c0 (grump cat)")
        }

        maybeStartNewLevel(activePlayTimeSecs)

        val inactiveBadges = getInactiveBadgesOnBoard(data.board)
        val inactiveBadgesNotC0 = inactiveBadges.filter { it != "c0" }
        if (inactiveBadgesNotC0.isEmpty()) {
            throw IllegalStateException("Should never happen")
        }

        val badgeToPutOnBoard = inactiveBadgesNotC0[0]
        return putBadgeToBoard(activePlayTimeSecs, badgeToPutOnBoard) ?: badgeToPutOnBoard
    }

    fun onPenalty(activePlayTimeSecs: Long): String? {
        data.c0HpNextDelta = 1
        return chainBadgeCounters("onPenalty", activePlayTimeSecs, false)
    }

    fun onReview(activePlayTimeSecs: Long): String? {
        val oldDelta = data.c0HpNextDelta
        data.c0HpNextDelta = 3

        if (countActiveGrumpyCatsOnBoard() > 0) {
            data.c0Hp = (data.c0Hp - oldDelta).coerceAtLeast(0)
            data.board = cloneBoardWithoutLastModified(data.board).toMutableList()

            if (data.c0Hp == 0) {
                return onShooCat(activePlayTimeSecs)
            }
        }

        return chainBadgeCounters("onReview", activePlayTimeSecs, true)
    }

    fun getGrumpyCatHealthpoints(): Int = data.c0Hp

    fun getLevel(): Int = data.level

    fun getBoard(): List<BoardCell> = data.board

    fun getLastBadge(): String? = data.lastBadge

    fun progress(activePlayTimeSecs: Long): Map<String, BadgeProgress> {
        val badges = listOf("f0", "s0", "s1", "s2", "t0", "c1", "c2", "c0")
        val allProgress = mutableMapOf<String, BadgeProgress>()

        for (counter in counters) {
            for (badge in badges) {
                val maybeProgress = if (badge == "c0") {
                    BadgeProgress(
                        badge = badge,
                        challenge = "review",
                        progressPct = 100 - (data.c0Hp * 100 / maxGrumpyCatHealthpoints()),
                        remainingReviews = data.c0Hp
                    )
                } else {
                    counter.progress(
                        badge,
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        data.badgesState[counter.javaClass.simpleName],
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                }
                if (maybeProgress != null) {
                    allProgress[badge] = maybeProgress
                }
            }
        }

        return allProgress
    }

    fun newLevelEmptyProgress(): Map<String, BadgeProgress> {
        val badges = listOf("f0", "s0", "s1", "s2", "t0", "c1", "c2")
        val allProgress = mutableMapOf<String, BadgeProgress>()
        val nextLevelBoard =
            getInactiveBadgesOnBoard(levelBadgesToNewBoard(LevelGenerator.getLevel(difficulty, data.level + 1)))
        for (counter in counters) {
            for (badge in badges) {
                val maybeProgress = counter.progress(
                    badge,
                    0,
                    null,
                    difficulty,
                    nextLevelBoard
                )
                if (maybeProgress != null) {
                    allProgress[badge] = maybeProgress
                }
            }
        }
        return allProgress
    }

    fun serialize(): String {
        val json = JSONObject()
        val badgesStateJson = JSONObject()
        data.badgesState.forEach { (key, value) ->
            if (value == null) {
                badgesStateJson.put(key, JSONObject.NULL)
            } else {
                badgesStateJson.put(key, value)
            }
        }
        json.put("badges_state", badgesStateJson)
        json.put("last_badge", data.lastBadge ?: JSONObject.NULL)
        json.put("last_badge_at", data.lastBadgeAt ?: JSONObject.NULL)
        json.put("level", data.level)
        json.put("c0_hp_next_delta", data.c0HpNextDelta)
        json.put("c0_hp", data.c0Hp)
        json.put("c0_lock_started_at", data.c0LockStartedAt)
        json.put("c0_active_time_penalty", data.c0ActiveTimePenalty)

        val boardJson = JSONArray()
        data.board.forEach { cell ->
            val cellJson = JSONObject()
            cellJson.put("badge", cell.badge)
            cellJson.put("is_active", cell.isActive ?: JSONObject.NULL)
            cellJson.put("is_last_modified", cell.isLastModified ?: JSONObject.NULL)
            boardJson.put(cellJson)
        }
        json.put("board", boardJson)
        return json.toString()
    }

    fun cloneBoardWithoutLastModified(board: List<BoardCell>): List<BoardCell> {
        return board.map { cell ->
            BoardCell(
                badge = cell.badge,
                isActive = cell.isActive
            )
        }
    }

    fun countActiveGrumpyCatsOnBoard(): Int {
        return data.board.count { it.badge == "c0" && it.isActive == true }
    }

    fun isLevelCompleted(): Boolean {
        var fineCells = 0
        for (cell in data.board) {
            if (cell.badge == "c0") {
                if (cell.isActive != true) {
                    fineCells += 1
                }
            } else if (cell.isActive == true) {
                fineCells += 1
            }
        }
        return fineCells == data.board.size
    }

    fun getNextLevelBoard(): List<BoardCell> {
        return levelBadgesToNewBoard(LevelGenerator.getLevel(difficulty, data.level + 1))
    }

    private fun chainBadgeCounters(
        methodName: String,
        activePlayTimeSecs: Long,
        terminateIfFound: Boolean
    ): String? {
        maybeStartNewLevel(activePlayTimeSecs)

        data.board = cloneBoardWithoutLastModified(data.board).toMutableList()
        data.lastBadge = null

        var badgeToPutOnBoard: String? = null
        val hasOldGrumpyCat = countActiveGrumpyCatsOnBoard() > 0
        if (hasOldGrumpyCat) {
            if (methodName == "onPenalty") {
                badgeToPutOnBoard = "c0"
            }
        } else {
            for (counter in counters) {
                val currentState = data.badgesState[counter.javaClass.simpleName]
                val advice = when (methodName) {
                    "onGameStarted" -> counter.onGameStarted(
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        currentState,
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                    "onFormulaUpdated" -> counter.onFormulaUpdated(
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        currentState,
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                    "onPrompt" -> counter.onPrompt(
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        currentState,
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                    "onPenalty" -> counter.onPenalty(
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        currentState,
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                    "onReview" -> counter.onReview(
                        adjustActivePlayTimeSecs(activePlayTimeSecs),
                        currentState,
                        difficulty,
                        getInactiveBadgesOnBoard(data.board)
                    )
                    else -> throw IllegalArgumentException("Unknown method: $methodName")
                }

                data.badgesState[counter.javaClass.simpleName] = advice.state
                if (advice.badge != null && badgeToPutOnBoard == null) {
                    badgeToPutOnBoard = advice.badge
                    if (terminateIfFound) {
                        break
                    }
                }
            }
        }

        return if (badgeToPutOnBoard != null) {
            putBadgeToBoard(activePlayTimeSecs, badgeToPutOnBoard)
        } else {
            null
        }
    }

    private fun maybeStartNewLevel(activePlayTimeSecs: Long) {
        if (!isLevelCompleted()) {
            return
        }

        val lastBadgeAt = data.lastBadgeAt
        data.level += 1
        data.board = levelBadgesToNewBoard(LevelGenerator.getLevel(difficulty, data.level)).toMutableList()
        data.lastBadge = null
        data.lastBadgeAt = null
        data.badgesState = mutableMapOf()
        data.c0Hp = 0
        data.c0HpNextDelta = 3
        data.c0LockStartedAt = 0
        data.c0ActiveTimePenalty = 0
        for (counter in counters) {
            val advice = counter.onGameStarted(
                lastBadgeAt ?: activePlayTimeSecs,
                null,
                difficulty,
                getInactiveBadgesOnBoard(data.board)
            )
            data.badgesState[counter.javaClass.simpleName] = advice.state
        }
    }

    private fun getInactiveBadgesOnBoard(board: List<BoardCell>): List<String> {
        return board.filter { it.isActive != true }.map { it.badge }
    }

    private fun adjustActivePlayTimeSecs(activePlayTimeSecs: Long): Long {
        var accum = activePlayTimeSecs - data.c0ActiveTimePenalty
        if (countActiveGrumpyCatsOnBoard() > 0) {
            val extraCurrCat = activePlayTimeSecs - data.c0LockStartedAt
            accum -= extraCurrCat
        }
        return accum
    }

    private fun expelGrumpyCat(board: List<BoardCell>): List<BoardCell> {
        val settledBoard = cloneBoardWithoutLastModified(board).toMutableList()
        for (cell in settledBoard) {
            if (cell.badge == "c0" && cell.isActive == true) {
                settledBoard[settledBoard.indexOf(cell)] = cell.copy(
                    isActive = false,
                    isLastModified = true
                )
                break
            }
        }
        return settledBoard
    }

    private fun hasInactiveBadgeOnBoard(board: List<BoardCell>, badge: String): Boolean {
        return board.any { it.badge == badge && it.isActive != true }
    }

    private fun activateBadgeOnBoard(board: List<BoardCell>, badge: String): List<BoardCell> {
        val settledBoard = cloneBoardWithoutLastModified(board).toMutableList()
        for (idx in settledBoard.indices) {
            val cell = settledBoard[idx]
            if (cell.badge == badge && cell.isActive != true) {
                settledBoard[idx] = cell.copy(isActive = true, isLastModified = true)
                break
            }
        }
        return settledBoard
    }

    private fun putBadgeToBoard(activePlayTimeSecs: Long, badge: String): String? {
        val oldBoard = data.board
        val oldCountGrumpyCats = countActiveGrumpyCatsOnBoard()

        if (!hasInactiveBadgeOnBoard(oldBoard, badge)) {
            return null
        }

        data.board = activateBadgeOnBoard(oldBoard, badge).toMutableList()

        if (badge == "c0" && oldCountGrumpyCats == 0) {
            data.c0LockStartedAt = activePlayTimeSecs
            data.c0Hp = maxGrumpyCatHealthpoints()
        }

        data.lastBadge = badge
        data.lastBadgeAt = activePlayTimeSecs

        return badge
    }

    private fun levelBadgesToNewBoard(level: List<String>): List<BoardCell> {
        return level.map { badge -> BoardCell(badge = badge) }
    }

    private fun fromJson(serialized: String): UserBadgesData {
        val json = JSONObject(serialized)
        val badgesState = mutableMapOf<String, String?>()
        val badgesStateJson = json.optJSONObject("badges_state")
        if (badgesStateJson != null) {
            badgesStateJson.keys().forEach { key ->
                badgesState[key] = if (badgesStateJson.isNull(key)) {
                    null
                } else {
                    badgesStateJson.optString(key, null)
                }
            }
        }

        val board = mutableListOf<BoardCell>()
        val boardJson = json.optJSONArray("board")
        if (boardJson != null) {
            for (idx in 0 until boardJson.length()) {
                val item = boardJson.optJSONObject(idx) ?: continue
                board.add(
                    BoardCell(
                        badge = item.optString("badge", ""),
                        isActive = if (item.has("is_active")) item.getBoolean("is_active") else false,
                        isLastModified = if (item.has("is_last_modified")) {
                            item.getBoolean("is_last_modified")
                        } else {
                            false
                        }
                    )
                )
            }
        }

        val lastBadge = if (json.has("last_badge") && !json.isNull("last_badge")) {
            json.getString("last_badge")
        } else {
            null
        }
        val lastBadgeAt = if (json.has("last_badge_at") && !json.isNull("last_badge_at")) {
            json.getLong("last_badge_at")
        } else {
            null
        }
        return UserBadgesData(
            badgesState = badgesState,
            lastBadge = lastBadge,
            lastBadgeAt = lastBadgeAt,
            level = json.optInt("level", 0),
            board = board,
            c0HpNextDelta = json.optInt("c0_hp_next_delta", 3),
            c0Hp = json.optInt("c0_hp", 0),
            c0LockStartedAt = json.optLong("c0_lock_started_at", 0L),
            c0ActiveTimePenalty = json.optLong("c0_active_time_penalty", 0L)
        )
    }

    private data class UserBadgesData(
        var badgesState: MutableMap<String, String?>,
        var lastBadge: String?,
        var lastBadgeAt: Long?,
        var level: Int,
        var board: MutableList<BoardCell>,
        var c0HpNextDelta: Int,
        var c0Hp: Int,
        var c0LockStartedAt: Long,
        var c0ActiveTimePenalty: Long
    )
}
