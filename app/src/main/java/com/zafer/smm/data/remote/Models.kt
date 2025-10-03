package com.zafer.smm.model

data class ServiceItem(
    val serviceId: Int,
    val name: String,
    val quantity: Int,
    val price: Double,
    val category: String
)

data class AddOrderResponse(
    val order: Long? = null,
    val error: String? = null
)

data class StatusResponse(
    val status: String? = null,
    val charge: Double? = null,
    val remains: Int? = null,
    val error: String? = null
)

data class BalanceResponse(
    val balance: String? = null,
    val currency: String? = null,
    val error: String? = null
)
