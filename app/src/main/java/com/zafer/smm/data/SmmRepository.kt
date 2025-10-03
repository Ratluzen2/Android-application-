package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.Network.api

class SmmRepository {

    suspend fun registerIfNeeded(deviceId: String, fullName: String? = null, username: String? = null): UserDto {
        return api.register(RegisterBody(deviceId, fullName, username))
    }

    suspend fun getServices(): List<ServiceItem> = api.getServices()

    suspend fun getUserBalance(deviceId: String): BalanceDto = api.getBalance(deviceId)

    suspend fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int): AddOrderResponse {
        return api.addOrder(AddOrderBody(service = serviceId, link = link, quantity = quantity, device_id = deviceId))
    }

    suspend fun getOrderStatus(providerOrderId: Long): StatusResponse = api.getOrderStatus(providerOrderId)

    suspend fun getOrders(deviceId: String): List<OrderItem> = api.getOrders(deviceId)

    suspend fun getLeaderboard(): List<LeaderboardEntry> = api.getLeaderboard()

    suspend fun walletDeposit(deviceId: String, amount: Double, method: String? = null, note: String? = null): WalletTransaction {
        return api.walletDeposit(DepositBody(device_id = deviceId, amount = amount, method = method, note = note))
    }
}
