package com.zafer.smm.data.remote

import com.zafer.smm.model.AddOrderResponse
import com.zafer.smm.model.BalanceResponse
import com.zafer.smm.model.StatusResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("/api/v2")
    suspend fun placeOrder(
        @Field("key") key: String,
        @Field("action") action: String = "add",
        @Field("service") service: Int,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): AddOrderResponse

    @FormUrlEncoded
    @POST("/api/v2")
    suspend fun orderStatus(
        @Field("key") key: String,
        @Field("action") action: String = "status",
        @Field("order") orderId: Long
    ): StatusResponse

    @FormUrlEncoded
    @POST("/api/v2")
    suspend fun balance(
        @Field("key") key: String,
        @Field("action") action: String = "balance"
    ): BalanceResponse

    companion object {
        // مأخوذ حرفيًا من كودك
        const val API_KEY: String = "25a9ceb07be0d8b2ba88e70dcbe92e06"
        private const val BASE: String = "https://kd1s.com" // API_URL = https://kd1s.com/api/v2

        fun create(): ApiService {
            val log = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(log)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(ApiService::class.java)
        }
    }
}
