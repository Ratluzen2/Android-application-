package com.zafer.smm.data.model

data class ServiceItem(
    val service: Int?,
    val name: String?,
    val category: String?,
    val rate: Double?,
    val min: Int?,
    val max: Int?,
    val type: String?
)

data class AddOrderResponse(
    val ok: Boolean? = null,
    val order: String? = null,
    val charge: Double? = null,
    val error: String? = null,
    val order_id: String? = null
)

data class StatusResponse(
    val status: String? = null,
    val remains: Int? = null,
    val charge: Double? = null
)

data class BalanceResponse(
    val balance: Double? = null,
    val currency: String? = null
)

data class OrderItem(
    val order: String?,
    val service: Int?,
    val link: String?,
    val quantity: Int?,
    val status: String?,
    val charge: Double?,
    val remains: Int?,
    val created_at: String?
)

data class WalletTx(
    val amount: Double,
    val type: String,
    val ref: String?,
    val created_at: String
)

data class LeaderboardEntry(
    val user_id: Long,
    val name: String?,
    val spent: Double
)
