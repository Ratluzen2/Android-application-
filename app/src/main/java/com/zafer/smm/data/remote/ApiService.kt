package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class ServicesEnvelope(
    val fromCache: Boolean? = null,
    val services: List<ServiceItem>? = null
)

data class RegisterBody(val device_id: String, val username: String? = null)
data class UserDto(val id: String?, val device_id: String?, val balance: Double?, val currency: String?)

interface ApiService {

    @GET("api/services")
    suspend fun getServices(@Query("force") force: Boolean = false): ServicesEnvelope

    @POST("api/register")
    suspend fun register(@Body body: RegisterBody): UserDto

    @FormUrlEncoded
    @POST("api/order/add")
    suspend fun placeOrder(
        @Field("device_id") deviceId: String,
        @Field("service") serviceId: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    @GET("api/order/{providerOrderId}/status")
    suspend fun orderStatus(@Path("providerOrderId") providerOrderId: Long): StatusResponse

    @GET("api/user/{deviceId}/balance")
    suspend fun balance(@Path("deviceId") deviceId: String): BalanceResponse

    @GET("api/orders/{deviceId}")
    suspend fun orders(@Path("deviceId") deviceId: String, @Query("limit") limit: Int = 50): List<OrderItem>

    @GET("api/leaderboard")
    suspend fun leaderboard(@Query("limit") limit: Int = 20): List<LeaderboardEntry>

    @FormUrlEncoded
    @POST("api/wallet/deposit")
    suspend fun walletDeposit(
        @Field("device_id") deviceId: String,
        @Field("amount") amount: Double,
        @Field("note") note: String? = null
    ): Map<String, Any?>
}

object Network {
    // اسم تطبيقك على هيروكو
    const val BASE_URL: String = "https://ratluzen-smm-backend.herokuapp.com/"

    val api: ApiService by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
