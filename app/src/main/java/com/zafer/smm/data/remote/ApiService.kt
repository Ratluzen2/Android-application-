package com.zafer.smm.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

/**
 * واجهة REST لخادمك على هيروكو.
 * تأكد أن BASE_URL ينتهي بـ '/'.
 */
interface ApiService {

    companion object {
        // عدّلها فقط إذا تغيّر رابط هيروكو لديك
        const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com/"
    }

    // --- Health / Docs ---
    @GET("health")
    suspend fun health(): Response<JsonObject>

    // --- Auth / Register ---
    @POST("api/register")
    suspend fun register(@Body body: RegisterBody): Response<JsonObject>

    // --- Wallet / Balance ---
    @GET("api/user/{device_id}/balance")
    suspend fun getBalance(@Path("device_id") deviceId: String): Response<BalanceDto>

    @POST("api/wallet/deposit")
    suspend fun deposit(@Body body: DepositBody): Response<JsonObject>

    @GET("api/wallet/transactions")
    suspend fun walletTransactions(@Query("device_id") deviceId: String): Response<List<WalletTransaction>>

    // --- Services & Orders ---
    @GET("api/services")
    suspend fun getServices(): Response<List<ServiceItem>>

    @POST("api/order/add")
    suspend fun addOrder(@Body body: AddOrderBody): Response<JsonObject>

    @GET("api/order/{provider_order_id}/status")
    suspend fun orderStatus(
        @Path("provider_order_id") providerOrderId: String
    ): Response<JsonObject>

    @GET("api/orders/{device_id}")
    suspend fun orders(@Path("device_id") deviceId: String): Response<List<OrderItem>>

    // --- Leaderboard ---
    @GET("api/leaderboard")
    suspend fun leaderboard(): Response<List<LeaderboardEntry>>
}
