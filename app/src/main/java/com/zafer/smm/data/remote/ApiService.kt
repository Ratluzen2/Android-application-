package com.zafer.smm.data.remote

import com.google.gson.annotations.SerializedName
import com.zafer.smm.BuildConfig
import com.zafer.smm.data.model.AddOrderResponse
import com.zafer.smm.data.model.BalanceResponse
import com.zafer.smm.data.model.ServicesResponse
import com.zafer.smm.data.model.StatusResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * واجهة REST الخاصة بـ kd1s (API v2)
 * BASE_URL = https://kd1s.com/api/v2/
 *
 * ملاحظة: نمرر المفتاح تلقائياً من BuildConfig.API_KEY في bodies الافتراضية.
 */
interface ApiService {

    // 1) جلب الخدمات
    @POST("services")
    suspend fun getServices(
        @Body body: KeyBody = KeyBody(BuildConfig.API_KEY)
    ): ServicesResponse

    // 2) إنشاء طلب جديد
    @POST("add")
    suspend fun addOrder(
        @Body body: AddOrderBody
    ): AddOrderResponse

    // 3) حالة طلب
    @POST("status")
    suspend fun getStatus(
        @Body body: StatusBody
    ): StatusResponse

    // 4) الرصيد
    @POST("balance")
    suspend fun getBalance(
        @Body body: KeyBody = KeyBody(BuildConfig.API_KEY)
    ): BalanceResponse

    companion object {
        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val okHttp = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL) // مثال: https://kd1s.com/api/v2/
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

/* ---------------------- أجسام الطلبات (Requests Bodies) ---------------------- */

data class KeyBody(
    @SerializedName("key") val key: String
)

/**
 * الحقول الإلزامية: key, service, link, quantity
 * باقي الحقول اختيارية وتختلف حسب نوع الخدمة في الـ SMM.
 */
data class AddOrderBody(
    @SerializedName("key") val key: String,
    @SerializedName("service") val service: Long,
    @SerializedName("link") val link: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("runs") val runs: Int? = null,
    @SerializedName("interval") val interval: Int? = null,
    @SerializedName("comments") val comments: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("user_id") val userId: String? = null
)

data class StatusBody(
    @SerializedName("key") val key: String,
    // هنا نتعامل مع طلب واحد للتبسيط. إن أردت "orders" لقائمة IDs عدّل النموذج حسب الحاجة.
    @SerializedName("order") val orderId: Long
)
