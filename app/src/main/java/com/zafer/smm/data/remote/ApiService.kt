package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import retrofit2.http.*

/** ضع رابط الهيروكو الخاص بك هنا */
object ApiConfig {
    const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com/"
    // مفاتيح اختبار إن أردتها ظاهرة داخل الكود
    const val API_KEY = "SAMPLE_VISIBLE_KEY_12345"
}

interface ApiService {

    @GET("api/services")
    suspend fun getServices(): List<ServiceItem>

    @GET("api/user/{device_id}/balance")
    suspend fun getBalance(@Path("device_id") deviceId: String): BalanceDto

    @POST("api/order/add")
    suspend fun addOrder(@Body body: AddOrderBody): AddOrderResponse

    @GET("api/order/{provider_order_id}/status")
    suspend fun getOrderStatus(@Path("provider_order_id") providerOrderId: Long): StatusResponse

    @GET("api/orders/{device_id}")
    suspend fun getOrders(@Path("device_id") deviceId: String): List<OrderItem>

    @GET("api/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @POST("api/wallet/deposit")
    suspend fun walletDeposit(@Body body: DepositBody): WalletTransaction

    @POST("api/register")
    suspend fun register(@Body body: RegisterBody): UserDto

    @GET("health")
    suspend fun health(): Map<String, Any?>
}
