package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = SmmRepository(ApiService.create())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus

    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance: StateFlow<BalanceResponse?> = _balance

    init {
        refreshServices()
    }

    fun refreshServices() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.getServices()
            _loading.value = false
            res.onSuccess { list -> _services.value = list }
                .onFailure { e -> _error.value = e.message ?: "Unknown error" }
        }
    }

    fun placeOrder(serviceId: Int, link: String, quantity: Int) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.placeOrder(serviceId, link, quantity)
            _loading.value = false
            res.onSuccess { id -> _lastOrderId.value = id }
                .onFailure { e -> _error.value = e.message ?: "Unknown error" }
        }
    }

    fun checkOrderStatus() {
        val id = _lastOrderId.value ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.orderStatus(id)
            _loading.value = false
            res.onSuccess { st -> _lastStatus.value = st }
                .onFailure { e -> _error.value = e.message ?: "Unknown error" }
        }
    }

    fun fetchBalance() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.balance()
            _loading.value = false
            res.onSuccess { b -> _balance.value = b }
                .onFailure { e -> _error.value = e.message ?: "Unknown error" }
        }
    }
}
