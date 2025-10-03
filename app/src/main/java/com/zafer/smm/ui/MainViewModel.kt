package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: SmmRepository = SmmRepository()
) : ViewModel() {

    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    val services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val balance = MutableStateFlow<BalanceDto?>(null)

    val lastOrderId = MutableStateFlow<Long?>(null)
    val lastProviderOrderId = MutableStateFlow<Long?>(null)
    val lastStatus = MutableStateFlow<StatusResponse?>(null)

    val orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

    private var deviceId: String? = null

    fun setDeviceId(id: String) {
        deviceId = id
        viewModelScope.launch {
            try { repo.register(id) } catch (_: Throwable) {}
        }
    }

    fun loadServices() = launchSafe {
        services.value = repo.getServices()
    }

    fun fetchBalance() {
        val id = deviceId ?: return
        launchSafe {
            balance.value = repo.getUserBalance(id)
        }
    }

    fun placeOrder(serviceId: Int, link: String, quantity: Int) {
        val id = deviceId ?: return
        launchSafe {
            val res = repo.placeOrder(id, serviceId, link, quantity)
            lastOrderId.value = res.order_id
            lastProviderOrderId.value = res.provider_order_id
        }
    }

    fun checkOrderStatus() {
        val pid = lastProviderOrderId.value ?: return
        launchSafe {
            lastStatus.value = repo.getOrderStatus(pid)
        }
    }

    fun loadOrders() {
        val id = deviceId ?: return
        launchSafe { orders.value = repo.getOrders(id) }
    }

    fun loadLeaderboard() = launchSafe {
        leaderboard.value = repo.getLeaderboard()
    }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try { block() } catch (t: Throwable) { error.value = t.message }
            finally { loading.value = false }
        }
    }
}
