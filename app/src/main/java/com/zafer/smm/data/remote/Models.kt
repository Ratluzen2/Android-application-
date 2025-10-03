package com.zafer.smm.data.remote

import com.google.gson.annotations.SerializedName

/** عنصر خدمة واحد ضمن قائمة الخدمات */
data class ServiceItem(
    val service: Long,                 // رقم الخدمة
    val name: String,                  // اسم الخدمة
    val category: String? = null,      // تصنيف (اختياري)
    val type: String? = null,          // نوع الخدمة (Default/Package/..)
    val rate: String? = null,          // السعر لكل 1000 (أحيانًا String من المزود)
    val min: Int? = null,              // أقل كمية
    val max: Int? = null,              // أعلى كمية
    val dripfeed: Boolean? = null,
    val refill: Boolean? = null,
    val cancel: Boolean? = null
)

/** بعض المزودين يعيدون الخدمات داخل مفتاح "services" */
data class ServicesEnvelope(
    val services: List<ServiceItem>? = null,
    val error: String? = null
)

/** نتيجة إنشاء طلب */
data class PlaceOrderResponse(
    val order: Long? = null,
    val error: String? = null
)

/** نتيجة حالة طلب */
data class OrderStatusResponse(
    val status: String? = null,          // Pending/In progress/Completed/Partial/Cancelled
    val charge: String? = null,          // قد تأتي نصًا
    @SerializedName("start_count")
    val startCount: Int? = null,
    val remains: Int? = null,
    val currency: String? = null,
    val error: String? = null
)
