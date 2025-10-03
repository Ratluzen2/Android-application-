package com.zafer.smm.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * واجهة الاتصال مع مزود SMM (kd1s.com/api/v2).
 * NOTE: إذا كان مزودك يعيد الخدمات داخل مفتاح "services"
 * بدّل نوع الإرجاع في getServices إلى ServicesEnvelope بدل List<ServiceItem>.
 */
interface ApiService {

    // جلب قائمة الخدمات
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun getServices(
        @Field("action") action: String = "services",
        @Field("key") key: String
    ): List<ServiceItem>                 // أو: ServicesEnvelope

    // إنشاء طلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun placeOrder(
        @Field("action") action: String = "add",
        @Field("key") key: String,
        @Field("service") serviceId: Long,
        @Field("link") link: String,
        @Field("quantity") quantity: Int
    ): PlaceOrderResponse

    // الاستعلام عن حالة طلب
    @FormUrlEncoded
    @POST("api/v2")
    suspend fun orderStatus(
        @Field("action") action: String = "status",
        @Field("key") key: String,
        @Field("order") orderId: Long
    ): OrderStatusResponse
}
