package com.zafer.smm.data.remote

import com.zafer.smm.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface ApiRaw {
    // بعض لوحات SMM (ومنها kd1s) تُرجع قائمة JSON مباشرةً للخدمات
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun services(
        @Field("key") key: String,
        @Field("action") action: String = "services"
    ): List<ServiceItem>

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun add(
        @Field("key") key: String,
        @Field("action") action: String = "add",
        @Field("service") service: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun status(
        @Field("key") key: String,
        @Field("action") action: String = "status",
        @Field("order") order: Long
    ): StatusResponse

    @FormUrlEncoded
    @POST("api/v2")
    suspend fun balance(
        @Field("key") key: String,
        @Field("action") action: String = "balance"
    ): BalanceResponse
}

object ApiService {
    // مأخوذة من كود البوت كما هي:
    // API_URL=https://kd1s.com/api/v2  => baseUrl = https://kd1s.com/
    // API_KEY=25a9ceb07be0d8b2ba88e70dcbe92e06
    const val BASE_URL = "https://kd1s.com/"
    const val API_KEY = "25a9ceb07be0d8b2ba88e70dcbe92e06"

    val api: ApiRaw by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(log).build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRaw::class.java)
    }
}
