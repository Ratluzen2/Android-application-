package com.zafer.smm.data

import com.zafer.smm.data.remote.ApiService
import com.zafer.smm.model.AddOrderResponse
import com.zafer.smm.model.BalanceResponse
import com.zafer.smm.model.ServiceItem
import com.zafer.smm.model.StatusResponse

class SmmRepository {

    private val api = ApiService.create()
    private val apiKey = ApiService.API_KEY

    // جزء من القوائم مأخوذ من service_api_mapping + services_dict في كودك
    // TikTok Followers 1k..4k + Views 1k..50k + Instagram Followers 1k..2k
    // الأسعار الأساسية مأخوذة كما في ملفك (مثال 1k TikTok followers = 3.50$ إلخ)
    // citations: service ids + quantities + prices
    // services_dict pricing: 1k/2k/3k/4k TikTok followers, views, etc.
    fun getServices(): List<ServiceItem> = listOf(
        // Followers TikTok (service_id = 13912, quantities 1k..4k)
        ServiceItem(serviceId = 13912, name = "متابعين تيكتوك 1k", quantity = 1000, price = 3.50, category = "smm"),
        ServiceItem(serviceId = 13912, name = "متابعين تيكتوك 2k", quantity = 2000, price = 7.00, category = "smm"),
        ServiceItem(serviceId = 13912, name = "متابعين تيكتوك 3k", quantity = 3000, price = 10.50, category = "smm"),
        ServiceItem(serviceId = 13912, name = "متابعين تيكتوك 4k", quantity = 4000, price = 14.00, category = "smm"),

        // Views TikTok (service_id 9447 for 1k, 9543 for 10k..50k)
        ServiceItem(serviceId = 9447, name = "مشاهدات تيكتوك 1k", quantity = 1000, price = 0.10, category = "smm"),
        ServiceItem(serviceId = 9543, name = "مشاهدات تيكتوك 10k", quantity = 10000, price = 0.80, category = "smm"),
        ServiceItem(serviceId = 9543, name = "مشاهدات تيكتوك 20k", quantity = 20000, price = 1.60, category = "smm"),
        ServiceItem(serviceId = 9543, name = "مشاهدات تيكتوك 30k", quantity = 30000, price = 2.40, category = "smm"),
        ServiceItem(serviceId = 9543, name = "مشاهدات تيكتوك 50k", quantity = 50000, price = 3.20, category = "smm"),

        // Followers Instagram (service_id 13788)
        ServiceItem(serviceId = 13788, name = "متابعين انستغرام 1k", quantity = 1000, price = 3.00, category = "smm"),
        ServiceItem(serviceId = 13788, name = "متابعين انستغرام 2k", quantity = 2000, price = 6.00, category = "smm")
    )

    suspend fun placeOrder(serviceId: Int, link: String, quantity: Int): AddOrderResponse {
        return api.placeOrder(
            key = apiKey,
            service = serviceId,
            link = link,
            quantity = quantity
        )
    }

    suspend fun orderStatus(orderId: Long): StatusResponse {
        return api.orderStatus(
            key = apiKey,
            orderId = orderId
        )
    }

    suspend fun balance(): BalanceResponse {
        return api.balance(
            key = apiKey
        )
    }
}
