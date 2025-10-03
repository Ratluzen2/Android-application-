package com.zafer.smm.util

import com.zafer.smm.data.model.*

object PricingUtils {

    /** خصم المشرف 10% */
    private const val MOD_DISCOUNT = 0.10

    fun label(service: ServiceItem, finalPrice: Double, qty: Int): String {
        val cleanName = service.name.replace(Regex("\\s*\\(.*\\)$"), "")
        return "$cleanName ($qty) – ${"%.2f".format(finalPrice)}$"
    }

    fun inferQuantity(service: ServiceItem): Int {
        // إن لم يوجد quantity من الـOverride نبحث عن رقم في الاسم (مثلاً 1k = 1000)
        val n = Regex("(\\d+)(k)?", RegexOption.IGNORE_CASE)
            .find(service.name)?.groups?.get(1)?.value?.toIntOrNull()
        val hasK = service.name.contains(Regex("\\d+k", RegexOption.IGNORE_CASE))
        return if (n != null) (if (hasK) n * 1000 else n) else service.min.coerceAtLeast(1)
    }

    fun finalPrice(
        service: ServiceItem,
        priceOverrides: Map<String, Double>,
        isModerator: Boolean
    ): Double {
        val base = priceOverrides[service.name] ?: service.rate
        val discounted = if (isModerator) base * (1 - MOD_DISCOUNT) else base
        return "%.4f".format(discounted).toDouble()
    }

    fun finalQuantity(
        service: ServiceItem,
        qtyOverrides: Map<String, Double>
    ): Int {
        val mult = qtyOverrides[service.name] ?: 1.0
        return (inferQuantity(service) * mult).toInt().coerceIn(service.min, service.max)
    }
}
