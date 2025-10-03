package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.*
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

    private val _balance = MutableStateFlow<BalanceDto?>(null)
    val balance: StateFlow<BalanceDto?> = _balance

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus

    fun register(deviceId: String, fullName: String? = null, username: String? = null) {
        viewModelScope.launch {
            try {
                _loading.value = true
                repo.registerIfNeeded(deviceId, fullName, username)
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshServices() {
        viewModelScope.launch {
            runWithLoading {
                _services.value = repo.getServices()
            }
        }
    }

    fun getUserBalance(deviceId: String) {
        viewModelScope.launch {
            runWithLoading {
                _balance.value = repo.getUserBalance(deviceId)
            }
        }
    }

    fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int) {
        viewModelScope.launch {
            runWithLoading {
                val resp = repo.placeOrder(deviceId, serviceId, link, quantity)
                _lastOrderId.value = resp.provider_order_id
            }
        }
    }

    fun getOrderStatus(providerOrderId: Long) {
        viewModelScope.launch {
            runWithLoading {
                _lastStatus.value = repo.getOrderStatus(providerOrderId)
            }
        }
    }

    fun loadOrders(deviceId: String) {
        viewModelScope.launch {
            runWithLoading {
                _orders.value = repo.getOrders(deviceId)
            }
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            runWithLoading {
                _leaderboard.value = repo.getLeaderboard()
            }
        }
    }

    fun walletDeposit(deviceId: String, amount: Double, method: String? = null, note: String? = null) {
        viewModelScope.launch {
            runWithLoading {
                repo.walletDeposit(deviceId, amount, method, note)
                _balance.value = repo.getUserBalance(deviceId)
            }
        }
    }

    private suspend fun runWithLoading(block: suspend () -> Unit) {
        try {
            _error.value = null
            _loading.value = true
            block()
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _loading.value = false
        }
    }
}
