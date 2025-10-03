package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.local.Prefs
import com.zafer.smm.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(
    private val repo: SmmRepository = SmmRepository()
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _services = MutableStateFlow<List<LocalMappedService>>(emptyList())
    val services: StateFlow<List<LocalMappedService>> = _services

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders

    private val _balanceProvider = MutableStateFlow<BalanceResponse?>(null)
    val balanceProvider: StateFlow<BalanceResponse?> = _balanceProvider

    private val _wallet = MutableStateFlow(0.0)
    val wallet: StateFlow<Double> = _wallet

    private val _deviceId = MutableStateFlow("unknown")
    val deviceId: StateFlow<String> = _deviceId

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus

    init {
        // تحميل أولي من التخزين المحلي
        _services.value = repo.localCatalogBlocking()
        _orders.value = Prefs.getOrders()
        _wallet.value = Prefs.getWallet()
        _isAdmin.value = Prefs.isAdmin()
        _deviceId.value = Prefs.getDeviceId() ?: "unknown"
    }

    fun refreshServices() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _services.value = repo.buildLocalCatalog()
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun attachDevice(deviceId: String) {
        Prefs.setDeviceId(deviceId)
        _deviceId.value = deviceId
    }

    fun addFunds(amount: Double) {
        val new = max(0.0, _wallet.value + amount)
        Prefs.setWallet(new)
        _wallet.value = new
    }

    fun fetchProviderBalance() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _balanceProvider.value = repo.getBalance()
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    private fun computeCost(serviceId: Int, quantity: Int): Double {
        // إيجاد الخدمة من الكتالوج لمعرفة multiplier والسعر
        val s = _services.value.firstOrNull { it.serviceId == serviceId } ?: return 0.0
        val mult = (s.quantityMultiplier ?: 1000).toDouble()
        val pricePerPack = s.priceUsd ?: 0.0
        if (mult <= 0.0 || pricePerPack <= 0.0) return 0.0
        return (quantity / mult) * pricePerPack
    }

    fun placeOrder(serviceId: Int, link: String, quantity: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // احسب التكلفة وخصمها من محفظة الجهاز
                val cost = computeCost(serviceId, quantity)
                if (cost <= 0.0) {
                    _error.value = "لا يمكن حساب التكلفة. تحقق من الخدمة والكمية."
                    return@launch
                }
                if (_wallet.value < cost) {
                    _error.value = "الرصيد غير كافٍ في محفظتك."
                    return@launch
                }

                val res = repo.placeOrder(serviceId, link, quantity)
                if (res.error != null) {
                    _error.value = res.error
                } else {
                    val oid = res.order
                    _lastOrderId.value = oid
                    // احفظ الطلب محليًا
                    val name = _services.value.firstOrNull { it.serviceId == serviceId }?.displayName
                        ?: "Service $serviceId"
                    val item = OrderItem(
                        orderId = oid,
                        deviceId = _deviceId.value,
                        serviceId = serviceId,
                        serviceName = name,
                        link = link,
                        quantity = quantity,
                        price = cost,
                        status = null,
                        charge = null,
                        remains = null,
                        createdAt = System.currentTimeMillis()
                    )
                    Prefs.addOrder(item)
                    _orders.value = Prefs.getOrders()
                    // خصم المحفظة
                    addFunds(-cost)
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
                val st = repo.getOrderStatus(id)
                _lastStatus.value = st
                // خزّن الحالة داخل الطلب
                val remainsInt = st.remains?.toIntOrNull()
                Prefs.updateOrderStatus(id, st.status, st.charge, remainsInt)
                _orders.value = Prefs.getOrders()
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleAdminWithPin(pin: String) {
        if (Prefs.checkPin(pin)) {
            Prefs.setAdmin(true)
            _isAdmin.value = true
        } else {
            _error.value = "PIN غير صحيح"
        }
    }

    fun changeOwnerPin(newPin: String) {
        Prefs.changePin(newPin)
    }
}
