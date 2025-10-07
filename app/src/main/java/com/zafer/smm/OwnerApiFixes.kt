@file:Suppress("UnusedImport", "SpellCheckingInspection")

package com.zafer.smm

import android.content.Context
import androidx.compose.foundation.text.KeyboardOptions   // يزيل خطأ Unresolved reference إن استُخدم هذا النوع في مكان آخر
import androidx.compose.ui.text.input.KeyboardType      // استيرادات وقائية
import androidx.compose.ui.text.input.ImeAction         // استيرادات وقائية
import kotlin.random.Random

/**
 * ملف OwnerApiFixes.kt مبسّط جدًا لتجنّب تعارضات واستيرادات Compose.
 * يوفر فقط دالة UID المستخدمة من قبل MainActivity.
 * تأكد ألا توجد نسخة أخرى من هذه الدالة في MainActivity.kt (حتى لا يحدث "Conflicting overloads").
 */
fun loadOrCreateUid(ctx: Context): String {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val existing = sp.getString("uid", null)
    if (existing != null) return existing

    // إنشاء UID بسيط وتخزينه
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString("uid", fresh).apply()
    return fresh
}
