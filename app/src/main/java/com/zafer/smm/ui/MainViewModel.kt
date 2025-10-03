package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.model.BalanceResponse
import com.zafer.smm.model.ServiceItem
import com.zafer.smm.model.StatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = SmmRepository()

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services = _services.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId = _lastOrderId.asStateFlow()

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus = _lastStatus.asStateFlow()

    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance = _balance.asStateFlow()

    fun refreshServices() {
        _services.value = repo.getServices()
    }

    fun placeOrder(serviceId: Int, link: String, quantity: Int) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val res = repo.placeOrder(serviceId, link, quantity)
                if (res.order != null) {
                    _lastOrderId.value = res.order
                } else {
                    _error.value = res.error ?: "خطأ غير معروف"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "فشل الطلب"
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkOrderStatus() {
        val oid = _lastOrderId.value ?: return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val st = repo.orderStatus(oid)
                _lastStatus.value = st
                if (st.error != null) _error.value = st.error
            } catch (e: Exception) {
                _error.value = e.message ?: "فشل جلب الحالة"
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchBalance() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val b = repo.balance()
                _balance.value = b
                if (b.error != null) _error.value = b.error
            } catch (e: Exception) {
                _error.value = e.message ?: "فشل جلب الرصيد"
            } finally {
                _loading.value = false
            }
        }
    }
}
