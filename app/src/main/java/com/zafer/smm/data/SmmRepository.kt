package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.ApiClient
import com.zafer.smm.data.remote.ApiService

class SmmRepository(
    private val api: ApiService = ApiClient.api
) {

    suspend fun register(deviceId: String, fullName: String? = null, username: String? = null): UserDto {
        return api.register(RegisterBody(device_id = deviceId, full_name = fullName, username = username))
    }

    suspend fun getServices(): List<ServiceItem> = api.getServices()

    suspend fun getUserBalance(deviceId: String): BalanceDto = api.getBalance(deviceId)

    suspend fun placeOrder(
        deviceId: String,
        serviceId: Int,
        link: String,
        quantity: Int
    ): AddOrderResponse {
        return api.addOrder(
            AddOrderBody(
                device_id = deviceId,
                service = serviceId,
                link = link,
                quantity = quantity
            )
        )
    }

    suspend fun getOrderStatus(providerOrderId: Long): StatusResponse =
        api.getOrderStatus(providerOrderId)

    suspend fun getOrders(deviceId: String): List<OrderItem> = api.getOrders(deviceId)

    suspend fun getLeaderboard(): List<LeaderboardEntry> = api.getLeaderboard()

    suspend fun walletDeposit(deviceId: String, amount: Double, method: String = "card"): WalletTransaction =
        api.walletDeposit(DepositBody(device_id = deviceId, amount = amount, method = method))
}
