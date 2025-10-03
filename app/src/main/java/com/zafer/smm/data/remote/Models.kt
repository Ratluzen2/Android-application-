package com.zafer.smm.data.remote

import com.google.gson.annotations.SerializedName

// عنصر خدمة واحد داخل المصفوفة
data class ServiceDto(
    @SerializedName("service") val service: Long,
    @SerializedName("name") val name: String,
    @SerializedName("rate") val rate: Double? = null,
    @SerializedName("min") val min: Int? = null,
    @SerializedName("max") val max: Int? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("desc") val desc: String? = null
)

// غلاف للاستجابة الجديدة { "services": [ ... ] }
data class ServicesResponse(
    @SerializedName("services") val services: List<ServiceDto> = emptyList()
)

// باقي النماذج
data class AddOrderResponse(
    @SerializedName("order") val order: Long? = null,       // بعض المزودين
    @SerializedName("order_id") val orderId: Long? = null,  // مزودون آخرون
    @SerializedName("error") val error: String? = null
)

data class OrderStatusResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("charge") val charge: Double? = null,
    @SerializedName("start_count") val startCount: Int? = null,
    @SerializedName("remains") val remains: Int? = null
)

data class BalanceResponse(
    @SerializedName("balance") val balance: String? = null,
    @SerializedName("currency") val currency: String? = null
)

data class ApiError(
    @SerializedName("error") val error: String
)
