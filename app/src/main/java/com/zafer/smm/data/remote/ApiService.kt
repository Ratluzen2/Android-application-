package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    // تُرجع بالشكل { "services": [ { service:1, name:"...", ... } ] }
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun listServices(
        @Field("key") key: String = API_KEY,
        @Field("action") action: String = "services"
    ): ServicesResponse

    // إنشاء طلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun add(
        @Field("service") serviceId: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int,
        @Field("key") key: String = API_KEY,
        @Field("action") action: String = "add"
    ): AddOrderResponse

    // حالة الطلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun status(
        @Field("order") orderId: Long,
        @Field("key") key: String = API_KEY,
        @Field("action") action: String = "status"
    ): StatusResponse

    // الرصيد
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun balance(
        @Field("key") key: String = API_KEY,
        @Field("action") action: String = "balance"
    ): BalanceResponse

    companion object {
        // <<<< القيم المعبّأة >>>>
        const val BASE_URL = "https://kd1s.com/"
        const val API_KEY  = "25a9ceb07be0d8b2ba88e70dcbe92e06"

        fun create(): ApiService {
            val client = OkHttpClient.Builder().build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
