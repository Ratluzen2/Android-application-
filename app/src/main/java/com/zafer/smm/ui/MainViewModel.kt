package com.zafer.smm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.Prefs
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.*
import com.zafer.smm.util.PricingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiService(
    val raw: ServiceItem,
    val finalPrice: Double,
    val finalQty: Int,
    val label: String
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SmmRepository()
    val deviceId = Prefs.deviceId(app)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _profile = MutableStateFlow(UserProfileDto(device_id = deviceId))
    val profile: StateFlow<UserProfileDto> = _profile

    private val _balance = MutableStateFlow<BalanceDto?>(null)
    val balance: StateFlow<BalanceDto?> = _balance

    private val _services = MutableStateFlow<List<UiService>>(emptyList())
    val services: StateFlow<List<UiService>> = _services

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories
    private val _selectedCat = MutableStateFlow<String?>(null)
    val selectedCat: StateFlow<String?> = _selectedCat

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders

    private val _leader = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leader: StateFlow<List<LeaderboardEntry>> = _leader

    init { initialLoad() }

    fun initialLoad() = viewModelScope.launch {
        try {
            _loading.value = true
            val prof = repo.registerIfNeeded(deviceId)
            _profile.value = prof
            val services = repo.getServices()
            val priceOv = repo.getPriceOverrides()
            val qtyOv = repo.getQuantityOverrides()
            val isMod = prof.role == Role.moderator || prof.role == Role.owner
            val ui = services.map { s ->
                val fp = PricingUtils.finalPrice(s, priceOv, isMod)
                val fq = PricingUtils.finalQuantity(s, qtyOv)
                UiService(s, fp, fq, PricingUtils.label(s, fp, fq))
            }.sortedBy { it.raw.category.orEmpty() + it.label }
            _services.value = ui
            _categories.value = ui.map { it.raw.category ?: "Other" }.distinct()
            _selectedCat.value = _categories.value.firstOrNull()
            _balance.value = repo.getUserBalance(deviceId)
            _leader.value = repo.getLeaderboard()
        } catch (e: Exception) { _error.value = e.message }
        finally { _loading.value = false }
    }

    fun setCategory(c: String) { _selectedCat.value = c }

    fun placeOrder(s: UiService, link: String, q: Int) = viewModelScope.launch {
        try {
            _loading.value = true
            repo.addOrder(deviceId, s.raw.service, link, q)
            _orders.value = repo.getOrders(deviceId)
            _balance.value = repo.getUserBalance(deviceId)
            _error.value = null
        } catch (e: Exception) { _error.value = e.message }
        finally { _loading.value = false }
    }

    fun refreshBalance() = viewModelScope.launch {
        try { _balance.value = repo.getUserBalance(deviceId) } catch (e: Exception) { _error.value = e.message }
    }
}
