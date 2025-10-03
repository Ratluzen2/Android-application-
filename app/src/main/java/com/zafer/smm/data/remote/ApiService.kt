package com.zafer.smm.data.remote

import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.OrderResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    companion object {
        const val BASE_URL = "https://kd1s.com/"
    }

    // kd1s يرجّع مصفوفة خدمات مباشرة
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun getServices(
        @Field("key") key: String,
        @Field("action") action: String = "services"
    ): List<ServiceItem>

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun placeOrder(
        @Field("key") key: String,
        @Field("action") action: String = "add",
        @Field("service") service: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): OrderResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun orderStatus(
        @Field("key") key: String,
        @Field("action") action: String = "status",
        @Field("order") orderId: Long
    ): StatusResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun balance(
        @Field("key") key: String,
        @Field("action") action: String = "balance"
    ): BalanceResponse
}
