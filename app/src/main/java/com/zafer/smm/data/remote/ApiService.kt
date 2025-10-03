package com.zafer.smm.data.remote

import com.zafer.smm.data.model.AddOrderResponse
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.data.model.StatusResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * ملاحظة مهمّة:
 * هذا الملف موجّه لباك-إند هيروكو الخاص بك:
 * https://ratluzen-smm-backend.herokuapp.com/
 *
 * المسارات المستخدمة هنا:
 *  - GET    /api/services                         → يرجع { fromCache?:bool, services:[...] }
 *  - POST   /api/order/add                        → ينشئ الطلب (device_id, service, link, quantity)
 *  - GET    /api/order/{providerOrderId}/status   → حالة الطلب من المزود
 *  - GET    /api/user/{deviceId}/balance          → رصيد المستخدم المحلي (في قاعدة Neon)
 *
 * تأكد أن الباك-إند لديك يوفّر هذه المسارات (تقدر تتأكد من /docs في هيروكو).
 */

// غلاف للاستجابة عند جلب الخدمات من الباك-إند
data class ServicesEnvelope(
    val fromCache: Boolean? = null,
    val services: List<ServiceItem>? = null
)

interface ApiService {

    // 1) جلب قائمة الخدمات من الباك-إند
    @GET("api/services")
    suspend fun getServices(): ServicesEnvelope

    // 2) إنشاء طلب عبر الباك-إند (وسيتم تمريره إلى kd1s وحفظه في Neon)
    @FormUrlEncoded
    @POST("api/order/add")
    suspend fun placeOrder(
        @Field("device_id") deviceId: String,
        @Field("service") serviceId: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    // 3) حالة الطلب من المزود (مع تحديث محلي في قاعدة البيانات)
    @GET("api/order/{providerOrderId}/status")
    suspend fun orderStatus(
        @Path("providerOrderId") providerOrderId: Long
    ): StatusResponse

    // 4) رصيد المستخدم المحلي (المحفوظ في Neon) بحسب deviceId
    @GET("api/user/{deviceId}/balance")
    suspend fun balance(
        @Path("deviceId") deviceId: String
    ): BalanceResponse
}

object Network {

    // 👈 هنا رابط تطبيقك على هيروكو
    const val BASE_URL: String = "https://ratluzen-smm-backend.herokuapp.com/"

    val api: ApiService by lazy {
        // مٌسجّل طلبات/استجابات للشبكة (للمساعدة على تتبّع المشاكل أثناء التطوير)
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
            .baseUrl(BASE_URL) // يجب أن ينتهي بـ /
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
