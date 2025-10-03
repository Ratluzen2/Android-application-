package com.zafer.smm.data

import com.zafer.smm.BuildConfig
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import com.zafer.smm.data.remote.ApiService

class SmmRepository(private val api: ApiService) {

    private val key: String = BuildConfig.API_KEY

    suspend fun refreshServices(): List<ServiceItem> {
        return try {
            api.getServices(key)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun placeOrder(serviceId: Int, link: String, qty: Int): Long? {
        return try {
            api.placeOrder(
                key = key,
                service = serviceId,
                link = link,
                quantity = qty
            ).order
        } catch (e: Exception) {
            null
        }
    }

    suspend fun orderStatus(orderId: Long): StatusResponse? {
        return try {
            api.orderStatus(key = key, orderId = orderId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun balance(): BalanceResponse? {
        return try {
            api.balance(key = key)
        } catch (e: Exception) {
            null
        }
    }
}
