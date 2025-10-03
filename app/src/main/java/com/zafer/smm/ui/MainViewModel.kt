package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repo = SmmRepository()

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

    fun refreshServices() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _services.value = repo.getServices()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchBalance(deviceId: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _balance.value = repo.getUserBalance(deviceId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val res = repo.placeOrder(deviceId, serviceId, link, quantity)
                _lastOrderId.value = res.order?.toLongOrNull() ?: res.order_id?.toLongOrNull()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkOrderStatus() {
        val id = _lastOrderId.value ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _lastStatus.value = repo.getOrderStatus(id)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
}
