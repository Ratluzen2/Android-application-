package com.zafer.smm.data.model

// عناصر الخدمات (مطابقة لواجهة الهيروكو)
data class ServiceItem(
    val service: Int,
    val name: String,
    val rate: Double,
    val min: Int,
    val max: Int,
    val category: String?
)

// إضافة طلب
data class AddOrderBody(
    val service: Int,
    val link: String,
    val quantity: Int,
    val device_id: String
)
data class AddOrderResponse(
    val provider_order_id: Long?
)

// حالة طلب
data class StatusResponse(
    val status: String? = null,
    val remains: Int? = null,
    val charge: Double? = null
)

// الرصيد
data class BalanceDto(
    val balance: Double,
    val currency: String
)

// الطلبات
data class OrderItem(
    val id: Long?,
    val provider_order_id: Long,
    val service: Int,
    val link: String,
    val quantity: Int,
    val status: String?,
    val charge: Double?,
    val remains: Int?,
    val created_at: String?
)

// الإيداع + سجل المحفظة
data class DepositBody(
    val device_id: String,
    val amount: Double,
    val method: String? = null,
    val note: String? = null
)
data class WalletTransaction(
    val id: Long?,
    val user_id: Long?,
    val amount: Double,
    val type: String,
    val ref: String?,
    val meta: Map<String, Any?>? = null,
    val created_at: String?
)

// التسجيل
data class RegisterBody(
    val device_id: String,
    val full_name: String? = null,
    val username: String? = null
)
data class UserDto(
    val device_id: String,
    val full_name: String? = null,
    val username: String? = null,
    val balance: Double = 0.0
)

// المتصدرين
data class LeaderboardEntry(
    val user_id: Long,
    val username: String?,
    val spent: Double
)
