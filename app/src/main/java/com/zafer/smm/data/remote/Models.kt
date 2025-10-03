package com.zafer.smm.data.model

// استجابات kd1s القياسية
data class ServiceItem(
    val service: Int?,      // id
    val name: String?,
    val rate: Double?,      // السعر لكل 1000 عادةً (في لوحات SMM)
    val min: Int?,
    val max: Int?,
    val category: String?
)

data class AddOrderResponse(
    val order: Long? = null,
    val error: String? = null
)

data class StatusResponse(
    val status: String? = null,   // "In progress", "Completed", ...
    val remains: String? = null,
    val charge: Double? = null,
    val error: String? = null
)

data class BalanceResponse(
    val balance: Double? = null,
    val currency: String? = null,
    val error: String? = null
)

// كتالوج مدمج (اسم الخدمة + السعر الافتراضي + رقم الخدمة + مضروب الكمية)
data class LocalMappedService(
    val displayName: String,
    val serviceId: Int?,           // null إذا خدمة يدوية/غير مدعومة API
    val quantityMultiplier: Int?,  // مثلاً 1000 أو 10000...
    val priceUsd: Double?          // من services_dict و telegram/ludo ...إلخ
)
