package com.mindwarrior.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HelloViewModel : ViewModel() {
    private val _message = MutableLiveData("Hello, MindWarrior!")
    val message: LiveData<String> = _message
}
