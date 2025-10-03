package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.remote.ServiceDto
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainViewModel(
    private val repo: SmmRepository
) : ViewModel() {

    var services by mutableStateOf<List<ServiceDto>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadServices() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                services = repo.getServices()
            } catch (t: Throwable) {
                error = t.message ?: "Failed to load services"
            } finally {
                loading = false
            }
        }
    }
}
