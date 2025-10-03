package com.zafer.smm.data.model

// بيانات المستخدم التي يرسلها التطبيق للباكند (مثلاً عند التسجيل/التعريف)
data class UserDto(
    val user_id: Long,
    val full_name: String? = null,
    val username: String? = null
)

// عنصر خدمة واحد من كتالوج الخدمات
data class ServiceItem(
    val service: Int?,
    val name: String?,
    val category: String?,
    val rate: Double?,
    val min: Int?,
    val max: Int?
)

// ليست الخدمات تُستخدم كـ typealias لسهولة التوافق مع الكود الحالي
typealias ServicesResponse = List<ServiceItem>

// رد إنشاء طلب
data class AddOrderResponse(
    val order: Long?
)

// رد حالة الطلب
data class StatusResponse(
    val status: String?,
    val remains: Int?,
    val charge: Double?
)

// رصيد الحساب
data class BalanceResponse(
    val balance: Double?,
    val currency: String?
)

// عنصر طلب (للائحة الطلبات إن احتجتها)
data class OrderItem(
    val id: Long,
    val service_id: Int?,
    val link: String?,
    val quantity: Int?,
    val status: String?,
    val charge: Double?,
    val created_at: String?
)

// صف المتصدرين
data class LeaderboardEntry(
    val user_id: Long,
    val spent: Double,
    val full_name: String? = null,
    val username: String? = null
)

// تمثيل محلي للخدمة إن احتجته داخل التطبيق
data class LocalMappedService(
    val id: Int,
    val name: String,
    val category: String,
    val pricePer1000: Double,
    val min: Int,
    val max: Int
)
