package com.zafer.smm.data

import com.zafer.smm.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmmRepository(
    private val api: ApiService = RetrofitClient.api
) {
    suspend fun fetchServices(key: String): List<ServiceItem> = withContext(Dispatchers.IO) {
        try {
            api.getServices(key = key) // إن كان مزودك يرجع { "services": [...] } بدّل للتغليف بالشرح أسفل
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun placeOrder(key: String, serviceId: Long, link: String, qty: Int): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.placeOrder(key = key, serviceId = serviceId, link = link, quantity = qty)
                val orderId = resp.order ?: resp.orderId
                if (orderId != null) Result.success(orderId)
                else Result.failure(Exception(resp.error ?: "Unknown error"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
