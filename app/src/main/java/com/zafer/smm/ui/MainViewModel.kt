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

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus

    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance: StateFlow<BalanceResponse?> = _balance

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders

    private val _leaders = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaders: StateFlow<List<LeaderboardEntry>> = _leaders

    fun autoRegister(deviceId: String, username: String? = null) = viewModelScope.launch {
        try { repo.register(deviceId, username) } catch (_: Exception) {}
    }

    fun refreshServices(force: Boolean=false) {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _services.value = repo.getServices(force) }
            catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }

    fun fetchBalance(deviceId: String) {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _balance.value = repo.getUserBalance(deviceId) }
            catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }

    fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int) {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try {
                val res = repo.placeOrder(deviceId, serviceId, link, quantity)
                _lastOrderId.value = res.order?.toLongOrNull() ?: res.order_id?.toLongOrNull()
            } catch (e: Exception) {
                _error.value = e.message
            } finally { _loading.value = false }
        }
    }

    fun checkOrderStatus() {
        val id = _lastOrderId.value ?: return
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _lastStatus.value = repo.getOrderStatus(id) }
            catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }

    fun loadOrders(deviceId: String) {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _orders.value = repo.getOrders(deviceId) }
            catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }

    fun loadLeaders() {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try { _leaders.value = repo.getLeaderboard() }
            catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }

    fun deposit(deviceId: String, amount: Double, note: String?=null) {
        _loading.value = true; _error.value = null
        viewModelScope.launch {
            try {
                repo.walletDeposit(deviceId, amount, note)
                _balance.value = repo.getUserBalance(deviceId)
            } catch (e: Exception) { _error.value = e.message }
            finally { _loading.value = false }
        }
    }
}
