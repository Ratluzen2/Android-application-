package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** إعدادات واجهة الـ API الخاصة بباكند هيروكو */
object ApiConfig {
    // عدّل الرابط لو غيّرت اسم تطبيق هيروكو
    const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com/"
    // مفتاح ظاهري للاختبار (لن يُستخدم في الباكند إلا إذا احتجته)
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

/** عميل Retrofit واحد للاستخدام عبر التطبيق */
object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
