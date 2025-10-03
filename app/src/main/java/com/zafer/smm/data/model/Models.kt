package com.zafer.smm.data.model

/* —— الكتالوج والخدمات —— */
data class ServiceItem(
    val service: Int,
    val name: String,
    val rate: Double,
    val min: Int,
    val max: Int,
    val category: String?
)

/* —— الطلبات —— */
data class AddOrderBody(
    val device_id: String,
    val service_id: Int,
    val link: String,
    val quantity: Int
)
data class AddOrderResponse(
    val provider_order_id: Long? = null,
    val status: String? = null
)
data class StatusResponse(
    val status: String? = null,
    val remains: Int? = null,
    val charge: Double? = null
)
data class OrderItem(
    val id: Long,
    val service_id: Int,
    val link: String,
    val quantity: Int,
    val status: String? = null,
    val charge: Double? = null,
    val created_at: String? = null,
    val provider_order_id: Long? = null,
    val category: String? = null,
    val price: Double? = null
)

/* —— الرصيد والمحفظة —— */
data class BalanceDto(val balance: Double, val currency: String)
data class WalletTransaction(
    val id: Long,
    val amount: Double,
    val type: String,
    val created_at: String?
)
data class DepositBody(val device_id: String, val amount: Double)

/* —— المستخدم/الدور —— */
enum class Role { owner, moderator, user }
data class UserProfileDto(
    val device_id: String,
    val full_name: String? = null,
    val username: String? = null,
    val role: Role = Role.user,
    val balance: Double = 0.0,
    val total_spent: Double = 0.0,
    val referral_code: String? = null
)

/* —— Overrides للتسعير/الكمية —— */
data class PriceOverride(val service_name: String, val price: Double)
data class QuantityOverride(val service_name: String, val quantity_multiplier: Double)

/* —— إحالات ومتصدّرين —— */
data class ReferralStats(
    val total: Int,
    val paid: Int,
    val pending: Int,
    val earnings: Double,
    val last_invitees: List<String> = emptyList()
)
data class LeaderboardEntry(val user_id: Long? = null, val spent: Double)

/* —— تسجيل —— */
data class RegisterBody(val device_id: String, val full_name: String? = null, val username: String? = null)
