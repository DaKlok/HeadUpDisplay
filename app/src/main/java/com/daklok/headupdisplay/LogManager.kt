package com.daklok.headupdisplay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun log(message: String) {
        _logs.update { (it + message).takeLast(100) }
    }

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }
}