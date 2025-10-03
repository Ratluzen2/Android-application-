package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.Network

class SmmRepository {
    private val api = Network.api

    suspend fun register(deviceId: String, username: String? = null): UserDto =
        api.register(com.zafer.smm.data.remote.RegisterBody(device_id = deviceId, username = username))

    suspend fun getServices(force: Boolean = false): List<ServiceItem> =
        api.getServices(force).services ?: emptyList()

    suspend fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int): AddOrderResponse =
        api.placeOrder(deviceId, serviceId, link, quantity)

    suspend fun getUserBalance(deviceId: String): BalanceResponse = api.balance(deviceId)

    suspend fun getOrderStatus(providerOrderId: Long): StatusResponse = api.orderStatus(providerOrderId)

    suspend fun getOrders(deviceId: String): List<OrderItem> = api.orders(deviceId)

    suspend fun getLeaderboard(): List<LeaderboardEntry> = api.leaderboard()

    suspend fun walletDeposit(deviceId: String, amount: Double, note: String? = null): Boolean =
        (api.walletDeposit(deviceId, amount, note)["ok"] == true)
}
