package com.zafer.smm.data.model

// عنصر خدمة واحد كما ترجعه أغلب SMM APIs (كل الحقول اختيارية لتفادي الأعطال)
data class ServiceItem(
    val service: Int? = null,
    val name: String? = null,
    val rate: Double? = null,
    val min: Int? = null,
    val max: Int? = null,
    val category: String? = null
)

data class OrderResponse(
    val order: Long? = null
)

data class StatusResponse(
    val status: String? = null,
    val charge: Double? = null,
    val remains: Int? = null
)

data class BalanceResponse(
    val balance: Double? = null,
    val currency: String? = null
)
