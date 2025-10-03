package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.ApiService

class SmmRepository(
    private val api: ApiService,
    private val apiKey: String
) {
    suspend fun fetchServices(): List<ServiceItem> =
        api.getServices(apiKey).services

    suspend fun createOrder(serviceId: Long, link: String, quantity: Long): OrderResponse =
        api.placeOrder(apiKey, service = serviceId, link = link, quantity = quantity)

    suspend fun getOrderStatus(orderId: Long): StatusResponse =
        api.orderStatus(apiKey, order = orderId)

    suspend fun getBalance(): BalanceResponse =
        api.balance(apiKey)
}
