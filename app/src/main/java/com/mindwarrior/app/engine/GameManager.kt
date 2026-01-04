package com.mindwarrior.app.engine

import java.util.Optional

object GameManager {

    fun onDifficultyChanged(user: User, newDifficulty: Difficulty): User {
        val newUser = UserFactory.createUser(newDifficulty)
        return if (user.pausedTimerSerialized.isPresent) {
            newUser.copy(
                pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
                activePlayTimerSerialized = Counter(newUser.activePlayTimerSerialized).pause().serialize(),
                reviewTimerSerialized = Counter(newUser.reviewTimerSerialized).pause().serialize()
            )
        } else {
            newUser
        }
    }

    fun onSleepScheduleChanged(
        user: User,
        draftEnabled: Boolean,
        draftStartMinutes: Int,
        draftEndMinutes: Int
    ): User {
        return user.copy(
            sleepEnabled = draftEnabled,
            sleepStartMinutes = draftStartMinutes,
            sleepEndMinutes = draftEndMinutes
        )

    }

    fun onPaused(user: User): User {
        if (user.pausedTimerSerialized.isPresent) {
            return user;
        }
        return user.copy(
            pausedTimerSerialized = Optional.of(Counter(null).resume().serialize()),
            reviewTimerSerialized = Counter(user.reviewTimerSerialized).pause().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).pause().serialize()
        )
    }

    fun onResume(user: User): User {
        if (user.pausedTimerSerialized.isEmpty) {
            return user
        }
        return user.copy(
            pausedTimerSerialized = Optional.empty(),
            reviewTimerSerialized = Counter(user.reviewTimerSerialized).resume().serialize(),
            activePlayTimerSerialized = Counter(user.activePlayTimerSerialized).resume().serialize()
        )
    }

    fun onLocalStorageUpdated(user: User, localStorate: Optional<String>): User {

        // TODO: is paused & activePlayTime is near 0 & localStorage contains non-empty "formula" JSON key then
        // start the game (remove paused, unpause other serialized timers)
        return user.copy(
            localStorate
        )
    }

}