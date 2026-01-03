package com.mindwarrior.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mindwarrior.app.UserStorage

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _timerForegroundEnabled = MutableLiveData<Boolean>()
    val timerForegroundEnabled: LiveData<Boolean> = _timerForegroundEnabled

    private val userListener = object : UserStorage.UserUpdateListener {
        override fun onUserUpdated(user: com.mindwarrior.app.engine.User) {
            _timerForegroundEnabled.value = user.timerForegroundEnabled
        }
    }

    init {
        UserStorage.observeUserChanges(getApplication(), userListener)
    }

    fun setTimerForegroundEnabled(enabled: Boolean) {
        val user = UserStorage.getUser(getApplication())
        UserStorage.upsertUser(getApplication(), user.copy(timerForegroundEnabled = enabled))
    }
}
