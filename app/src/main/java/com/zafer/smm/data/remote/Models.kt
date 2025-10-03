package com.zafer.smm.data.model

data class ServiceItem(
    val service: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val category: String? = null,
    val rate: Double? = null,
    val min: Int? = null,
    val max: Int? = null,
    val dripfeed: Boolean? = null
)

data class ServicesResponse(
    val services: List<ServiceItem> = emptyList()
)

data class AddOrderResponse(val order: Long? = null)

data class StatusResponse(
    val status: String? = null,
    val charge: String? = null,
    val start_count: String? = null,
    val remains: String? = null
)

data class BalanceResponse(
    val balance: String? = null,
    val currency: String? = null
)
