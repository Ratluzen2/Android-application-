package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.remote.ServiceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val services: List<ServiceItem> = emptyList(),
    val message: String? = null
)

class MainViewModel(
    private val repo: SmmRepository = SmmRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun loadServices(key: String) {
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val list = repo.fetchServices(key)
            _state.value = UiState(
                loading = false,
                services = list,
                message = if (list.isEmpty()) "لم يتم العثور على خدمات/ تأكد من الـ API Key" else null
            )
        }
    }

    fun order(key: String, serviceId: Long, link: String, qty: Int, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val result = repo.placeOrder(key, serviceId, link, qty)
            _state.value = _state.value.copy(loading = false)
            onDone(
                result.fold(
                    onSuccess = { "تم إنشاء الطلب #$it" },
                    onFailure = { "فشل الطلب: ${it.message}" }
                )
            )
        }
    }
}
