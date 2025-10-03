package com.zafer.smm.data

import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmmRepository(
    private val api: com.zafer.smm.data.remote.ApiRaw = ApiService.api
) {
    // نفس خرائط الخدمات والأسعار كما قمنا بتجهيزها سابقًا (مطابقة للبوت)
    private val serviceApiMapping: Map<String, Pair<Int, Int>> = mapOf(
        "متابعين تيكتوك 1k" to (13912 to 1000),
        "متابعين تيكتوك 2k" to (13912 to 2000),
        "متابعين تيكتوك 3k" to (13912 to 3000),
        "متابعين تيكتوك 4k" to (13912 to 4000),
        "مشاهدات تيكتوك 1k"  to (9447 to 1000),
        "مشاهدات تيكتوك 10k" to (9543 to 10000),
        "مشاهدات تيكتوك 20k" to (9543 to 20000),
        "مشاهدات تيكتوك 30k" to (9543 to 30000),
        "مشاهدات تيكتوك 50k" to (9543 to 50000),
        "متابعين انستغرام 1k" to (13788 to 1000),
        "متابعين انستغرام 2k" to (13788 to 2000),
        "متابعين انستغرام 3k" to (13788 to 3000),
        "متابعين انستغرام 4k" to (13788 to 4000),
        "لايكات تيكتوك 1k" to (12320 to 1000),
        "لايكات تيكتوك 2k" to (12320 to 2000),
        "لايكات تيكتوك 3k" to (12320 to 3000),
        "لايكات تيكتوك 4k" to (12320 to 4000),
        "لايكات انستغرام 1k" to (7973 to 1000),
        "لايكات انستغرام 2k" to (7973 to 2000),
        "لايكات انستغرام 3k" to (7973 to 3000),
        "لايكات انستغرام 4k" to (7973 to 4000),
        "مشاهدات انستغرام 10k" to (13531 to 10000),
        "مشاهدات انستغرام 20k" to (13531 to 20000),
        "مشاهدات انستغرام 30k" to (13531 to 30000),
        "مشاهدات انستغرام 50k" to (13531 to 50000),
        "مشاهدات بث تيكتوك 1k" to (13259 to 1000),
        "مشاهدات بث تيكتوك 2k" to (13259 to 2000),
        "مشاهدات بث تيكتوك 3k" to (13259 to 3000),
        "مشاهدات بث تيكتوك 4k" to (13259 to 4000),
        "مشاهدات بث انستغرام 1k" to (12595 to 1000),
        "مشاهدات بث انستغرام 2k" to (12595 to 2000),
        "مشاهدات بث انستغرام 3k" to (12595 to 3000),
        "مشاهدات بث انستغرام 4k" to (12595 to 4000),
        "رفع سكور بثك1k" to (13125 to 1000),
        "رفع سكور بثك2k" to (13125 to 2000),
        "رفع سكور بثك3k" to (13125 to 3000),
        "رفع سكور بثك10k" to (13125 to 10000),
        // Telegram
        "أعضاء قنوات تلي 1k" to (14021 to 1000),
        "أعضاء قنوات تلي 2k" to (14021 to 2000),
        "أعضاء قنوات تلي 3k" to (14021 to 3000),
        "أعضاء قنوات تلي 4k" to (14021 to 4000),
        "أعضاء كروبات تلي 1k" to (14022 to 1000),
        "أعضاء كروبات تلي 2k" to (14022 to 2000),
        "أعضاء كروبات تلي 3k" to (14022 to 3000),
        "أعضاء كروبات تلي 4k" to (14022 to 4000)
    )

    private val servicesDict: Map<String, Double> = mapOf(
        "متابعين تيكتوك 1k" to 3.50, "متابعين تيكتوك 2k" to 7.0,
        "متابعين تيكتوك 3k" to 10.50, "متابعين تيكتوك 4k" to 14.0,
        "مشاهدات تيكتوك 1k" to 0.10, "مشاهدات تيكتوك 10k" to 0.80,
        "مشاهدات تيكتوك 20k" to 1.60, "مشاهدات تيكتوك 30k" to 2.40, "مشاهدات تيكتوك 50k" to 3.20,
        "متابعين انستغرام 1k" to 3.0, "متابعين انستغرام 2k" to 6.0,
        "متابعين انستغرام 3k" to 9.0, "متابعين انستغرام 4k" to 12.0,
        "لايكات تيكتوك 1k" to 1.0, "لايكات تيكتوك 2k" to 2.0, "لايكات تيكتوك 3k" to 3.0, "لايكات تيكتوك 4k" to 4.0,
        "لايكات انستغرام 1k" to 1.0, "لايكات انستغرام 2k" to 2.0, "لايكات انستغرام 3k" to 3.0, "لايكات انستغرام 4k" to 4.0,
        "مشاهدات انستغرام 10k" to 0.80, "مشاهدات انستغرام 20k" to 1.60, "مشاهدات انستغرام 30k" to 2.40, "مشاهدات انستغرام 50k" to 3.20,
        "مشاهدات بث تيكتوك 1k" to 2.0, "مشاهدات بث تيكتوك 2k" to 4.0, "مشاهدات بث تيكتوك 3k" to 6.0, "مشاهدات بث تيكتوك 4k" to 8.0,
        "مشاهدات بث انستغرام 1k" to 2.0, "مشاهدات بث انستغرام 2k" to 4.0, "مشاهدات بث انستغرام 3k" to 6.0, "مشاهدات بث انستغرام 4k" to 8.0,
        "رفع سكور بثك1k" to 2.0, "رفع سكور بثك2k" to 4.0, "رفع سكور بثك3k" to 6.0, "رفع سكور بثك10k" to 20.0
    )

    fun localCatalogBlocking(): List<LocalMappedService> =
        servicesDict.map { (name, price) ->
            val m = serviceApiMapping[name]
            LocalMappedService(
                displayName = name,
                serviceId = m?.first,
                quantityMultiplier = m?.second,
                priceUsd = price
            )
        }

    suspend fun buildLocalCatalog(): List<LocalMappedService> =
        withContext(Dispatchers.Default) { localCatalogBlocking() }

    // kd1s:
    suspend fun placeOrder(serviceId: Int, link: String, quantity: Int): AddOrderResponse =
        withContext(Dispatchers.IO) { api.add(ApiService.API_KEY, service = serviceId, link = link, quantity = quantity) }

    suspend fun getOrderStatus(orderId: Long): StatusResponse =
        withContext(Dispatchers.IO) { api.status(ApiService.API_KEY, order = orderId) }

    suspend fun getBalance(): BalanceResponse =
        withContext(Dispatchers.IO) { api.balance(ApiService.API_KEY) }
}
