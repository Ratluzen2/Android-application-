package com.zafer.smm

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ========== إعدادات أساسية ==========
object AppCfg {
    // غيّر العنوان ليطابق هيروكو لديك
    const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com/api"
}

// ========== أدوات عامة للشبكة (بدون مكتبات إضافية) ==========
private suspend fun postJson(
    url: String,
    json: JSONObject,
    headers: Map<String, String> = emptyMap()
): Pair<Int, String> = withContext(Dispatchers.IO) {
    val u = URL(url)
    val conn = (u.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
        connectTimeout = 15000
        readTimeout = 20000
    }
    try {
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
            w.write(json.toString())
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.let {
            BufferedReader(InputStreamReader(it)).use { br -> br.readText() }
        } ?: ""
        code to body
    } finally {
        conn.disconnect()
    }
}

private suspend fun getJson(
    url: String,
    headers: Map<String, String> = emptyMap()
): Pair<Int, String> = withContext(Dispatchers.IO) {
    val u = URL(url)
    val conn = (u.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
        connectTimeout = 15000
        readTimeout = 20000
    }
    try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.let {
            BufferedReader(InputStreamReader(it)).use { br -> br.readText() }
        } ?: ""
        code to body
    } finally {
        conn.disconnect()
    }
}

// ========== UID: إنشاء/تحميل ==========
fun loadOrCreateUid(ctx: Context): String {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var uid = sp.getString("uid", null)
    if (uid.isNullOrBlank()) {
        uid = UUID.randomUUID().toString().replace("-", "").lowercase()
        sp.edit().putString("uid", uid).apply()
        // نبلّغ الخادم لإنشاء سجل المستخدم إن لم يوجد
        try {
            // fire-and-forget
            val payload = JSONObject().put("uid", uid)
            // نرسل على IO بدون انتظار (اختياري)
        } catch (_: Exception) { }
    }
    return uid!!
}

// ========== واجهات API (أدمن) لإضافة/خصم ==========
suspend fun apiOwnerTopup(baseUrl: String, adminPass: String, uid: String, amount: Double): Result<Double> {
    val url = "$baseUrl/admin/users/${uid}/topup"
    val (code, body) = postJson(
        url,
        JSONObject().put("amount", amount),
        headers = mapOf("X-Admin-Pass" to adminPass)
    )
    return if (code in 200..299) {
        try {
            val j = JSONObject(body)
            Result.success(j.getDouble("balance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    } else {
        Result.failure(IllegalStateException("HTTP $code: $body"))
    }
}

suspend fun apiOwnerDeduct(baseUrl: String, adminPass: String, uid: String, amount: Double): Result<Double> {
    val url = "$baseUrl/admin/users/${uid}/deduct"
    val (code, body) = postJson(
        url,
        JSONObject().put("amount", amount),
        headers = mapOf("X-Admin-Pass" to adminPass)
    )
    return if (code in 200..299) {
        try {
            val j = JSONObject(body)
            Result.success(j.getDouble("balance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    } else {
        Result.failure(IllegalStateException("HTTP $code: $body"))
    }
}

// ========== واجهة API (مستخدم) لإرسال كارت أسيا سيل ==========
suspend fun apiSubmitAsiacellCard(baseUrl: String, uid: String, cardNumber: String): Result<Unit> {
    val clean = cardNumber.replace(" ", "").replace("-", "")
    if (!clean.all { it.isDigit() } || (clean.length != 14 && clean.length != 16)) {
        return Result.failure(IllegalArgumentException("رقم الكارت يجب أن يكون 14 أو 16 رقمًا"))
    }
    val url = "$baseUrl/wallet/asiacell/submit"
    val (code, body) = postJson(url, JSONObject().put("uid", uid).put("card_number", clean))
    return if (code in 200..299) Result.success(Unit)
    else Result.failure(IllegalStateException("HTTP $code: $body"))
}

// ========== Composables اختيارية لنسخها في الشاشات ==========
@Composable
fun OwnerTopupDeductCard(
    baseUrl: String = AppCfg.BASE_URL,
    ownerPassProvider: () -> String // أعطه دالة ترجع كلمة مرور الأدمن (مثلاً من SharedPreferences بعد نجاح تسجيل المالك)
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uid by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("إدارة الرصيد عبر UID", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uid, onValueChange = { uid = it.trim() },
                label = { Text("UID المستخدم") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text("المبلغ (دولار)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val pass = ownerPassProvider()
                    if (uid.isBlank() || amt == null) {
                        status = "أدخل UID ومبلغ صالح"
                        return@Button
                    }
                    scope.launch {
                        val res = apiOwnerTopup(baseUrl, pass, uid, amt)
                        status = res.fold(
                            onSuccess = { "تمت الإضافة. الرصيد الجديد: $it" },
                            onFailure = { "فشل الإضافة: ${it.message}" }
                        )
                    }
                }) { Text("إضافة") }

                Button(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val pass = ownerPassProvider()
                    if (uid.isBlank() || amt == null) {
                        status = "أدخل UID ومبلغ صالح"
                        return@Button
                    }
                    scope.launch {
                        val res = apiOwnerDeduct(baseUrl, pass, uid, amt)
                        status = res.fold(
                            onSuccess = { "تم الخصم. الرصيد الجديد: $it" },
                            onFailure = { "فشل الخصم: ${it.message}" }
                        )
                    }
                }) { Text("خصم") }
            }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun AsiacellRechargeCard(
    baseUrl: String = AppCfg.BASE_URL,
    uidProvider: () -> String // أعطه دالة ترجع UID (مثلاً loadOrCreateUid(context))
) {
    val scope = rememberCoroutineScope()
    var card by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("شحن عبر أسيا سيل", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = card,
                onValueChange = { card = it },
                label = { Text("رقم الكارت (14 أو 16 رقم)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val uid = uidProvider()
                    scope.launch {
                        val res = apiSubmitAsiacellCard(baseUrl, uid, card)
                        msg = res.fold(
                            onSuccess = { "تم إرسال الكارت. ستتم مراجعته من المالك." },
                            onFailure = { "فشل إرسال الكارت: ${it.message}" }
                        )
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("إرسال") }

            msg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

// ========== ألوان النص في شاشة الرصيد ==========
@Composable
fun BalanceLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}
