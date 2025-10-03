package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.ApiService

class SmmRepository(private val api: ApiService) {

    suspend fun getServices(): Result<List<ServiceItem>> = runCatching {
        api.listServices().services
    }

    suspend fun placeOrder(serviceId: Int, link: String, quantity: Int): Result<Long> = runCatching {
        api.add(serviceId, link, quantity).order ?: error("No order id")
    }

    suspend fun orderStatus(orderId: Long): Result<StatusResponse> = runCatching {
        api.status(orderId)
    }

    suspend fun balance(): Result<BalanceResponse> = runCatching {
        api.balance()
    }
}
