package com.zafer.smm.data.model

import com.google.gson.annotations.SerializedName

// ردّ الخدمات: { "services": [ ... ] }
data class ServicesResponse(
    @SerializedName("services") val services: List<ServiceItem>
)

data class ServiceItem(
    @SerializedName("service") val service: Long,
    @SerializedName("name")    val name: String,
    @SerializedName("type")    val type: String? = null,
    @SerializedName("category")val category: String? = null,
    @SerializedName("rate")    val rate: Double? = null,
    @SerializedName("min")     val min: Long? = null,
    @SerializedName("max")     val max: Long? = null,
    @SerializedName("dripfeed")val dripfeed: Boolean? = null,
    @SerializedName("refill")  val refill: Boolean? = null,
    @SerializedName("cancel")  val cancel: Boolean? = null,
    @SerializedName("desc")    val desc: String? = null
)

data class OrderResponse(
    @SerializedName("order") val orderId: Long?
)

data class StatusResponse(
    @SerializedName("status")     val status: String? = null,
    @SerializedName("charge")     val charge: Double? = null,
    @SerializedName("start_count")val startCount: Long? = null,
    @SerializedName("remains")    val remains: Long? = null
)

data class BalanceResponse(
    @SerializedName("balance")  val balance: String? = null,
    @SerializedName("currency") val currency: String? = null
)
