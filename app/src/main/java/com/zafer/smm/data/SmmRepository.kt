package com.zafer.smm.data

import com.zafer.smm.data.model.AddOrderResponse
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import com.zafer.smm.data.remote.Network

class SmmRepository {
    private val api = Network.api

    suspend fun getServices(): List<ServiceItem> {
        return try {
            api.getServices().services ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun placeOrder(deviceId: String, serviceId: Int, link: String, quantity: Int): AddOrderResponse {
        return api.placeOrder(
            deviceId = deviceId,
            serviceId = serviceId,
            link = link,
            quantity = quantity
        )
    }

    suspend fun getUserBalance(deviceId: String): BalanceResponse {
        return api.balance(deviceId)
    }

    suspend fun getOrderStatus(providerOrderId: Long): StatusResponse {
        return api.orderStatus(providerOrderId)
    }
}
