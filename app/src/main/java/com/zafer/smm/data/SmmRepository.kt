package com.zafer.smm.data

import com.zafer.smm.data.remote.*

class SmmRepository(
    private val api: ApiService,
    private val apiKey: String
) {
    // يرجّع قائمة الخدمات من الغلاف { "services": [ ... ] }
    suspend fun getServices(): List<ServiceDto> {
        val resp = api.getServices(apiKey)
        return resp.services
    }

    suspend fun placeOrder(serviceId: Long, link: String, quantity: Int): Long {
        val resp = api.placeOrder(
            key = apiKey,
            serviceId = serviceId,
            link = link,
            quantity = quantity
        )
        val id = resp.order ?: resp.orderId
        if (id != null) return id
        throw IllegalStateException(resp.error ?: "Unknown error while placing order")
    }

    suspend fun orderStatus(orderId: Long): OrderStatusResponse =
        api.orderStatus(key = apiKey, orderId = orderId)

    suspend fun balance(): BalanceResponse =
        api.balance(key = apiKey)
}
