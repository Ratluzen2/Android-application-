package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import retrofit2.http.*

interface ApiService {

    companion object {
        // تأكد أن عنوانك ينتهي بـ /api/
        const val BASE_URL = "https://ratluzen-smm-backend.herokuapp.com/api/"

        // المفاتيح تريدها مكشوفة داخل الكود كما طلبت
        const val API_KEY = "25a9ceb07be0d8b2ba88e70dcbe92"
    }

    // قائمة الخدمات
    @GET("services")
    suspend fun getServices(
        @Header("X-API-KEY") apiKey: String = API_KEY
    ): ServicesResponse

    // الرصيد
    @GET("balance")
    suspend fun getBalance(
        @Header("X-API-KEY") apiKey: String = API_KEY
    ): BalanceResponse

    // إنشاء طلب
    @POST("orders")
    suspend fun placeOrder(
        @Header("X-API-KEY") apiKey: String = API_KEY,
        @Body body: Any               // ارسل Map<String, Any> أو داتا كلاس — الكود سيعمل
    ): AddOrderResponse

    // حالة الطلب
    @GET("orders/{id}")
    suspend fun orderStatus(
        @Header("X-API-KEY") apiKey: String = API_KEY,
        @Path("id") id: Long
    ): StatusResponse

    // تعريف/تسجيل المستخدم في الباكند (إن كان الباكند يدعم ذلك)
    @POST("users")
    suspend fun registerUser(
        @Body user: UserDto
    )

    // المتصدرون
    @GET("leaderboard")
    suspend fun leaderboard(): List<LeaderboardEntry>
}
