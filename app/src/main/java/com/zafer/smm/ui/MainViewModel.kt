package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: SmmRepository = SmmRepository()
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _services = MutableStateFlow<List<LocalMappedService>>(emptyList())
    val services: StateFlow<List<LocalMappedService>> = _services

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
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // نعرض الكتالوج المحلي المطابق للبوت (مع IDs المدعومة)
                val local = repo.buildLocalCatalog()
                _services.value = local
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun placeOrder(serviceId: Int, link: String, quantity: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val res = repo.placeOrder(serviceId, link, quantity)
                if (res.error != null) {
                    _error.value = res.error
                } else {
                    _lastOrderId.value = res.order
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkOrderStatus() {
        val id = _lastOrderId.value ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _lastStatus.value = repo.getOrderStatus(id)
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchBalance() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _balance.value = repo.getBalance()
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }
}
