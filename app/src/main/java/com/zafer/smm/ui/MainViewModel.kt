    init {
       package com.zafer.smm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import com.zafer.smm.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {

    // Retrofit جاهز بدون استخدام الامتداد create() المسبب للخطأ
    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
            .create(ApiService::class.java)
    }

    private val repo = SmmRepository(api)

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance: StateFlow<BalanceResponse?> = _balance.asStateFlow()

    private val _lastOrderId = MutableStateFlow<Long?>(null)
    val lastOrderId: StateFlow<Long?> = _lastOrderId.asStateFlow()

    private val _lastStatus = MutableStateFlow<StatusResponse?>(null)
    val lastStatus: StateFlow<StatusResponse?> = _lastStatus.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshServices() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _services.value = repo.refreshServices()
            } catch (e: Exception) {
                _error.value = e.message
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
                _balance.value = repo.balance()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun placeOrder(serviceId: Int, link: String, qty: Int) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _lastOrderId.value = repo.placeOrder(serviceId, link, qty)
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
                _lastStatus.value = repo.orderStatus(id)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
} refreshServices()
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
