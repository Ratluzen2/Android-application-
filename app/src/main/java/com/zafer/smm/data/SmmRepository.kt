package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmmRepository {
    private val api = RetrofitClient.api

    /* ——— التسجيل/الملف ——— */
    suspend fun registerIfNeeded(deviceId: String): UserProfileDto = withContext(Dispatchers.IO) {
        try { api.getProfile(deviceId) }          // إن endpoint موجود
        catch (_: Exception) { null } ?: run {
            try { api.register(RegisterBody(deviceId = deviceId, full_name = "User")) }
            catch (_: Exception) { UserProfileDto(device_id = deviceId) }
        }
    }

    suspend fun getProfile(deviceId: String): UserProfileDto = withContext(Dispatchers.IO) {
        try { api.getProfile(deviceId) } catch (_: Exception) { UserProfileDto(device_id = deviceId) }
    }

    /* ——— الكتالوج/التسعير ——— */
    suspend fun getServices(): List<ServiceItem> = withContext(Dispatchers.IO) { api.getServices() }
    suspend fun getPriceOverrides(): Map<String, Double> = withContext(Dispatchers.IO) {
        try { api.getPriceOverrides().associate { it.service_name to it.price } } catch (_: Exception) { emptyMap() }
    }
    suspend fun getQuantityOverrides(): Map<String, Double> = withContext(Dispatchers.IO) {
        try { api.getQuantityOverrides().associate { it.service_name to it.quantity_multiplier } } catch (_: Exception) { emptyMap() }
    }

    /* ——— الرصيد/المحفظة ——— */
    suspend fun getUserBalance(deviceId: String): BalanceDto = withContext(Dispatchers.IO) { api.getBalance(deviceId) }
    suspend fun walletDeposit(deviceId: String, amount: Double): WalletTransaction =
        withContext(Dispatchers.IO) { api.walletDeposit(DepositBody(device_id = deviceId, amount = amount)) }

    /* ——— الطلبات ——— */
    suspend fun addOrder(deviceId: String, serviceId: Int, link: String, qty: Int): AddOrderResponse =
        withContext(Dispatchers.IO) { api.addOrder(AddOrderBody(deviceId, serviceId, link, qty)) }
    suspend fun getOrders(deviceId: String): List<OrderItem> = withContext(Dispatchers.IO) { api.getOrders(deviceId) }
    suspend fun getOrderStatus(orderId: Long): StatusResponse = withContext(Dispatchers.IO) { api.getOrderStatus(orderId) }

    /* ——— إحالات/متصدرين ——— */
    suspend fun getReferralStats(deviceId: String): ReferralStats = withContext(Dispatchers.IO) {
        try { api.getReferralStats(deviceId) } catch (_: Exception) { ReferralStats(0,0,0,0.0) }
    }
    suspend fun getLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) { api.getLeaderboard() }
}
