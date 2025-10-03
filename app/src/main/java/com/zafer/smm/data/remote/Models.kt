
package com.zafer.smm.data.model

// شكل خدمات kd1s (للإظهار/التكامل)
data class ServiceItem(
    val service: Int?,
    val name: String?,
    val rate: Double?,
    val min: Int?,
    val max: Int?,
    val category: String?
)

// استدعاءات kd1s
data class AddOrderResponse(val order: Long? = null, val error: String? = null)
data class StatusResponse(val status: String? = null, val remains: String? = null, val charge: Double? = null, val error: String? = null)
data class BalanceResponse(val balance: Double? = null, val currency: String? = null, val error: String? = null)

// كتالوج محلي مطابق للبوت
data class LocalMappedService(
    val displayName: String,
    val serviceId: Int?,           // null = خدمة عرض/يدوية
    val quantityMultiplier: Int?,  // 1000/10000...
    val priceUsd: Double?
)

// عنصر طلب مخزّن محليًا
data class OrderItem(
    val orderId: Long?,        // من kd1s
    val deviceId: String,      // هوية الجهاز
    val serviceId: Int?,
    val serviceName: String,
    val link: String,
    val quantity: Int,
    val price: Double,         // التكلفة المحسوبة وقت الطلب
    val status: String?,       // آخر حالة معروفة
    val charge: Double?,       // آخر تكلفة من kd1s (قد تختلف عن price المحلي)
    val remains: Int?,         // آخر “remains”
    val createdAt: Long        // timestamp
)

data class LeaderboardEntry(
    val deviceId: String,
    val totalSpent: Double
)
