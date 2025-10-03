package com.zafer.smm.data.model

// ============ DTOs ============

data class UserDto(
    val device_id: String,
    val full_name: String? = null,
    val username: String? = null,
    val role: String? = null,
    val balance: Double = 0.0,
    val currency: String? = "USD"
)

data class ServiceItem(
    val service: Int,
    val name: String,
    val category: String? = null,
    val rate: Double? = null,
    val min: Int? = null,
    val max: Int? = null
)

data class AddOrderBody(
    val device_id: String,
    val service: Int,
    val link: String? = null,
    val quantity: Int? = null
)

data class AddOrderResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val order_id: Long? = null,
    val provider_order_id: Long? = null
)

data class StatusResponse(
    val status: String? = null,
    val remains: Int? = null,
    val charge: Double? = null,
    val start_count: Int? = null
)

data class BalanceDto(
    val balance: Double,
    val currency: String? = "USD"
)

data class OrderItem(
    val id: Long,
    val category: String? = null,
    val service: String? = null,
    val price: Double? = null,
    val status: String? = null,
    val api_order_number: Long? = null,
    val ordered_at: String? = null
)

data class LeaderboardEntry(
    val user_id: String,
    val full_name: String? = null,
    val total_spent: Double
)

data class WalletTransaction(
    val id: Long? = null,
    val device_id: String,
    val amount: Double,
    val method: String? = null,
    val created_at: String? = null
)

data class DepositBody(
    val device_id: String,
    val amount: Double,
    val method: String? = "card"
)

data class RegisterBody(
    val device_id: String,
    val full_name: String? = null,
    val username: String? = null
)
