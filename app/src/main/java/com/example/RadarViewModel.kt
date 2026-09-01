package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RadarViewModel : ViewModel() {

    private val _isRadarActive = MutableStateFlow(false)
    val isRadarActive: StateFlow<Boolean> = _isRadarActive.asStateFlow()

    fun toggleRadar() {
        _isRadarActive.value = !_isRadarActive.value
    }
}
