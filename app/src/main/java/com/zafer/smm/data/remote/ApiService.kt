package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import retrofit2.http.*

// أجسام الطلب
data class RegisterBody(val device_id: String, val username: String? = null)

interface ApiService {

    @POST("register")
    suspend fun register(@Body body: RegisterBody): UserDto

    @GET("services")
    suspend fun getServices(@Query("force") force: Boolean = false): ServicesResponse

    @GET("balance")
    suspend fun balance(@Query("device_id") deviceId: String): BalanceResponse

    @POST("orders")
    suspend fun placeOrder(
        @Query("device_id") deviceId: String,
        @Query("service_id") serviceId: Int,
        @Query("link") link: String,
        @Query("quantity") quantity: Int
    ): AddOrderResponse

    @GET("orders")
    suspend fun orders(@Query("device_id") deviceId: String): List<OrderItem>

    @GET("order/{provider_order_id}")
    suspend fun orderStatus(@Path("provider_order_id") providerOrderId: Long): StatusResponse

    @POST("wallet/deposit")
    suspend fun walletDeposit(
        @Query("device_id") deviceId: String,
        @Query("amount") amount: Double,
        @Query("note") note: String? = null
    ): Map<String, Boolean>
}
