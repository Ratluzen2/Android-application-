package com.zafer.smm.data.remote

// كل الحقول Nullable لتقبّل أي شكل من JSON بدون أخطاء تحويل

// طلب التسجيل (يحفظ المستخدم لأول مرة عبر device_id)
data class RegisterBody(
    val device_id: String,
    val username: String? = null,
    val full_name: String? = null
)

// رصيد المستخدم
data class BalanceDto(
    val device_id: String? = null,
    val balance: Double? = null,
    val currency: String? = null
)

// عنصر خدمة (كما يتوقعه واجهتك الحالية في الشاشة)
data class ServiceItem(
    val service: Int? = null,
    val name: String? = null,
    val category: String? = null,
    val min: Int? = null,
    val max: Int? = null,
    val rate: Double? = null,
    val type: String? = null,
    val refills: String? = null,
    val dripfeed: String? = null
)

// إنشاء طلب
data class AddOrderBody(
    val service_id: Int,
    val link: String,
    val quantity: Int,
    val device_id: String
)

// عنصر طلب
data class OrderItem(
    val id: Long? = null,
    val provider_order_id: String? = null,
    val service_id: Int? = null,
    val link: String? = null,
    val quantity: Int? = null,
    val status: String? = null,
    val charge: Double? = null,
    val remains: Int? = null,
    val start_count: Int? = null,
    val created_at: String? = null
)

// الإيداع للمحفظة
data class DepositBody(
    val device_id: String,
    val amount: Double
)

// حركة محفظة
data class WalletTransaction(
    val id: Long? = null,
    val user_id: String? = null,
    val amount: Double? = null,
    val type: String? = null,      // deposit | order | refund ...
    val ref: String? = null,
    val meta: Any? = null,
    val created_at: String? = null
)

// المتصدرين
data class LeaderboardEntry(
    val user_id: String? = null,
    val spent: Double? = null,
    val username: String? = null
)
