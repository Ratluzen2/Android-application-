package com.zafer.smm.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.AppModule
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.local.SmmDao
import com.zafer.smm.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repo: SmmRepository? = null) : ViewModel() {

    // سيتم حقن repo بعد initContext
    private lateinit var repository: SmmRepository
    private lateinit var deviceId: String

    private val OWNER_DEVICE_ID = "OWNER_DEVICE_ID_HERE" // ضع قيمة جهازك إن رغبت
    private val ADMIN_PIN = "1234"                       // PIN للوحة المالك

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info

    private val _user = MutableStateFlow<UserProfileEntity?>(null)
    val user: StateFlow<UserProfileEntity?> = _user

    private val _catalog = MutableStateFlow<List<LocalMappedService>>(emptyList())
    val catalog: StateFlow<List<LocalMappedService>> = _catalog

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders

    private val _wallet = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    val wallet: StateFlow<List<WalletTransactionEntity>> = _wallet

    private val _leaders = MutableStateFlow<List<LeaderboardEntryEntity>>(emptyList())
    val leaders: StateFlow<List<LeaderboardEntryEntity>> = _leaders

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus

    fun initContext(context: Context) {
        if (this::repository.isInitialized) return
        AppModule.init(context)
        val dao: SmmDao = AppModule.db.dao()
        val injected = repo ?: SmmRepository(dao)
        repository = injected
        deviceId = AppModule.deviceId(context)

        viewModelScope.launch {
            repository.ensureUser(deviceId, OWNER_DEVICE_ID, ADMIN_PIN)
            _user.value = repository.getUser(deviceId)
            _catalog.value = repository.buildLocalCatalog()
            refreshLists()
        }
    }

    private fun refreshLists() {
        viewModelScope.launch {
            _orders.value = repository.myOrders(deviceId)
            _wallet.value = repository.wallet(deviceId)
            _leaders.value = repository.leaders()
        }
    }

    fun topup(amount: Double) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                repository.topup(deviceId, amount, "topup")
                _user.value = repository.getUser(deviceId)
                refreshLists()
                _info.value = "تم شحن الرصيد: $amount$"
            } catch (t: Throwable) {
                _error.value = t.message
            } finally { _loading.value = false }
        }
    }

    fun placeOrder(name: String, link: String, quantity: Int) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null; _info.value = null
            try {
                val (remoteId, err) = repository.placeOrder(deviceId, name, link, quantity)
                if (err != null) _error.value = err
                _lastOrderId.value = remoteId
                _user.value = repository.getUser(deviceId)
                refreshLists()
            } catch (t: Throwable) {
                _error.value = t.message
            } finally { _loading.value = false }
        }
    }

    fun checkOrderStatus(orderId: Long) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                _lastStatus.value = repository.checkStatus(orderId)
            } catch (t: Throwable) {
                _error.value = t.message
            } finally { _loading.value = false }
        }
    }

    fun fetchProviderBalance() {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val b = repository.fetchBalanceFromProvider()
                _info.value = "Provider: ${b.balance ?: "-"} ${b.currency ?: ""}"
            } catch (t: Throwable) {
                _error.value = t.message
            } finally { _loading.value = false }
        }
    }
}
