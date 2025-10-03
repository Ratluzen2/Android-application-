package com.zafer.smm.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    // استرجاع الخدمات: الآن يرجع ServicesResponse (غلاف يحوي "services")
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun getServices(
        @Field("key") key: String,
        @Field("action") action: String = "services"
    ): ServicesResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun placeOrder(
        @Field("key") key: String,
        @Field("action") action: String = "add",
        @Field("service") serviceId: Long,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun orderStatus(
        @Field("key") key: String,
        @Field("action") action: String = "status",
        @Field("order") orderId: Long
    ): OrderStatusResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun balance(
        @Field("key") key: String,
        @Field("action") action: String = "balance"
    ): BalanceResponse
}
