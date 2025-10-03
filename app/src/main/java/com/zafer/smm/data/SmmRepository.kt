package com.zafer.smm.data

import com.google.gson.JsonObject
import com.zafer.smm.data.remote.*

class SmmRepository(
    private val api: ApiService = Network.api
) {

    private fun <T> unwrap(resp: retrofit2.Response<T>): T {
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body != null) return body
        }
        val err = resp.errorBody()?.string() ?: "Unknown error"
        throw RuntimeException(err)
    }

    // Health (اختياري)
    suspend fun healthOk(): Boolean {
        return try { unwrap(api.health()); true } catch (_: Exception) { false }
    }

    suspend fun register(deviceId: String, username: String? = null, fullName: String? = null): JsonObject {
        return unwrap(api.register(RegisterBody(device_id = deviceId, username = username, full_name = fullName)))
    }

    suspend fun getServices(): List<ServiceItem> = unwrap(api.getServices())

    suspend fun getBalance(deviceId: String): BalanceDto = unwrap(api.getBalance(deviceId))

    suspend fun placeOrder(serviceId: Int, link: String, quantity: Int, deviceId: String): JsonObject {
        val body = AddOrderBody(
            service_id = serviceId,
            link = link,
            quantity = quantity,
            device_id = deviceId
        )
        return unwrap(api.addOrder(body))
    }

    suspend fun orderStatus(providerOrderId: String): JsonObject =
        unwrap(api.orderStatus(providerOrderId))

    suspend fun getOrders(deviceId: String): List<OrderItem> = unwrap(api.orders(deviceId))

    suspend fun deposit(deviceId: String, amount: Double): JsonObject =
        unwrap(api.deposit(DepositBody(device_id = deviceId, amount = amount)))

    suspend fun walletTransactions(deviceId: String): List<WalletTransaction> =
        unwrap(api.walletTransactions(deviceId))

    suspend fun leaderboard(): List<LeaderboardEntry> =
        unwrap(api.leaderboard())
}
