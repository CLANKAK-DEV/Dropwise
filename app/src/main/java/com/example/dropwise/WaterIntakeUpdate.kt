package com.example.dropwise

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

object WaterIntakeUpdate {
    private val _updateTrigger = mutableStateOf(0L)
    val updateTrigger: State<Long> = _updateTrigger

    fun triggerUpdate() {
        _updateTrigger.value = System.currentTimeMillis()
    }
}