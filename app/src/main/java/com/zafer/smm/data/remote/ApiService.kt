package com.zafer.smm.data.remote

import com.zafer.smm.model.*
import retrofit2.http.*

interface ApiService {
    // قائمة الخدمات
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun getServices(
        @Field("key") apiKey: String,
        @Field("action") action: String = "services"
    ): List<ServiceDtoV2>

    // إنشاء طلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun addOrder(
        @Field("key") apiKey: String,
        @Field("action") action: String = "add",
        @Field("service") serviceId: Long,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    // حالة طلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun orderStatus(
        @Field("key") apiKey: String,
        @Field("action") action: String = "status",
        @Field("order") orderId: Long
    ): OrderStatusResponse
}
