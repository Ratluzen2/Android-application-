package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    // action=services
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun getServices(
        @Field("key") key: String,
        @Field("action") action: String = "services"
    ): ServicesResponse

    // action=add
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun placeOrder(
        @Field("key") key: String,
        @Field("action") action: String = "add",
        @Field("service") service: Long,
        @Field("link") link: String,
        @Field("quantity") quantity: Long
    ): OrderResponse

    // action=status
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun orderStatus(
        @Field("key") key: String,
        @Field("action") action: String = "status",
        @Field("order") order: Long
    ): StatusResponse

    // action=balance
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun balance(
        @Field("key") key: String,
        @Field("action") action: String = "balance"
    ): BalanceResponse
}
