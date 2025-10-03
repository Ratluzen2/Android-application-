package com.zafer.smm.util

import com.zafer.smm.data.model.ServiceItem
import kotlin.math.round

object PricingUtils {

    /** خصم المشرفين 10% */
    private const val MOD_DISCOUNT = 0.10

    /**
     * يستخرج كمية رقمية من اسم الخدمة مثل:
     * "1000"، "1k"، "2K"، "1.5k"، "3m" ...
     * يرجّع null إذا لم يجد رقمًا.
     */
    fun extractQuantityFromName(name: String?): Int? {
        if (name.isNullOrBlank()) return null
        // رقم + اختيارياً لاحقة k/m (حسّاسة/غير حسّاسة)
        val rx = Regex("""(\d+(?:\.\d+)?)\s*(k|K|m|M)?""")
        val match = rx.find(name) ?: return null

        val num = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues.getOrNull(2)?.lowercase()

        val multiplier = when (unit) {
            "k" -> 1_000.0
            "m" -> 1_000_000.0
            else -> 1.0
        }
        val value = num * multiplier
        return value.toInt()
    }

    /**
     * يحسب السعر النهائي مع خصم المشرف إن لزم.
     * يتعامل بأمان مع القيم null.
     */
    fun finalPrice(basePrice: Double?, isModerator: Boolean): Double {
        val base = basePrice ?: 0.0
        val discounted = if (isModerator) base * (1.0 - MOD_DISCOUNT) else base
        return round2(discounted)
    }

    /**
     * يبني ملصق موحّد للخدمة: الاسم + (الكمية) + السعر.
     * إذا لم توجد min يستخدم الكمية المستخرجة من الاسم.
     */
    fun labelForService(service: ServiceItem, isModerator: Boolean): String {
        val qty: Int? = service.min ?: extractQuantityFromName(service.name)
        val price = finalPrice(service.rate, isModerator)

        val qtyStr = qty?.toString() ?: "?"
        val name = service.name
        return "$name ($qtyStr) – $price$"
    }

    private fun round2(x: Double): Double = round(x * 100.0) / 100.0
}
