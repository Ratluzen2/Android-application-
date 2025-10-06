@file:Suppress("UnusedImport")

package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.random.Random

/* =========================
   إعدادات عامة
   ========================= */
private const val API_BASE =
    "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com" // عدّلها إن لزم
private const val PREFS_NAME = "app_prefs"
private const val KEY_UID = "uid"
private const val KEY_OWNER = "owner_mode"
private const val OWNER_PIN = "2000"

/* =========================
   Theme
   ========================= */
private val Bg       = Color(0xFF111315)
private val Surface1 = Color(0xFF1A1F24)
private val OnBg     = Color(0xFFEDEFF2)
private val Accent   = Color(0xFFB388FF) // بنفسجي واضح
private val Good     = Color(0xFF4CAF50) // أخضر
private val Bad      = Color(0xFFE53935) // أحمر
private val Dim      = Color(0xFF9AA3AB)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface1,
            primary = Accent,
            onBackground = OnBg,
            onSurface = OnBg,
        ),
        content = content
    )
}

/* =========================
   MainActivity
   ========================= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppRoot() } }
    }
}

/* =========================
   شريط سفلي وشاشات
   ========================= */
private enum class Tab { HOME, SUPPORT, WALLET, ORDERS, SERVICES }

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }

    // وضع المالك — يبقى محفوظ بعد إعادة فتح التطبيق
    var ownerMode by remember { mutableStateOf(getOwnerMode(ctx)) }
    var showOwnerDashboard by remember { mutableStateOf(ownerMode) }

    // حالة السيرفر
    var online by remember { mutableStateOf<Boolean?>(null) }

    // فحص السيرفر دوري + تسجيل UID
    LaunchedEffect(Unit) {
        scope.launch { tryUpsertUid(uid) }
        while (true) {
            online = pingHealth()
            delay(20_000)
        }
    }

    var current by remember { mutableStateOf(Tab.HOME) }

    // الحاوية الرئيسية
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // محتوى كل تبويب
        when (current) {
            Tab.HOME     -> EmptyScreen()
            Tab.SUPPORT  -> SupportScreen()
            Tab.WALLET   -> EmptyScreen()
            Tab.ORDERS   -> EmptyScreen()
            Tab.SERVICES -> EmptyScreen()
        }

        // حالة السيرفر أعلى يمين + زر إعدادات (يعرض UID + تسجيل المالك)
        ServerStatusPill(
            online = online,
            onOpenSettings = { settingsOpen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 10.dp)
        )

        // الشريط السفلي
        BottomNavBar(
            current = current,
            onChange = { current = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // لوحة المالك — تبقى ظاهرة إذا كان المالك مسجل
        if (showOwnerDashboard) {
            OwnerDashboard(
                onClose = {
                    // زر للخروج من وضع المالك (تعطيل دائم حتى إعادة التسجيل)
                    showOwnerDashboard = false
                    ownerMode = false
                    setOwnerMode(ctx, false)
                },
                onKeepOpen = {
                    // إبقاءها مفتوحة (لا تغيير للحالة)
                    showOwnerDashboard = true
                }
            )
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            isOwner = ownerMode,
            onOwnerLoginSuccess = {
                ownerMode = true
                setOwnerMode(ctx, true)
                settingsOpen = false
                // افتح اللوحة فورًا
                // وستبقى مفتوحة بعد إعادة تشغيل التطبيق
                // بسبب حفظ ownerMode في SharedPreferences
                LaunchedEffect(Unit) {
                    // مجرد تفعيل الحالة في نفس الإطار
                }
            },
            onDismiss = { settingsOpen = false }
        )
    }
}

/* =========================
   عناصر الواجهة
   ========================= */
@Composable
private fun EmptyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    )
}

/* -------------------------
   شاشة الدعم (واتساب + تيليجرام)
   ------------------------- */
@Composable
private fun SupportScreen() {
    val uri = LocalUriHandler.current
    val whatsappUrl = "https://wa.me/9647763410970"
    val telegramUrl = "https://t.me/z396r"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("الدعم", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("للتواصل أو الاستفسار اختر إحدى الطرق التالية:")

        Spacer(Modifier.height(12.dp))
        ContactCard(
            title = "واتساب",
            subtitle = "+964 776 341 0970",
            actionText = "افتح واتساب",
            onClick = { uri.openUri(whatsappUrl) },
            icon = Icons.Filled.Call
        )

        Spacer(Modifier.height(10.dp))
        ContactCard(
            title = "تيليجرام",
            subtitle = "@z396r",
            actionText = "افتح تيليجرام",
            onClick = { uri.openUri(telegramUrl) },
            icon = Icons.Filled.Send
        )
    }
}

@Composable
private fun ContactCard(
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Dim, fontSize = 13.sp)
            }
            TextButton(onClick = onClick) { Text(actionText) }
        }
    }
}

@Composable
private fun ServerStatusPill(
    online: Boolean?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (txt, clr) = when (online) {
        true  -> "الخادم: متصل" to Good
        false -> "الخادم: غير متصل" to Bad
        null  -> "الخادم: ..." to Dim
    }
    Row(
        modifier = modifier
            .background(Surface1, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(clr, shape = MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(6.dp))
        Text(txt, fontSize = 12.sp, color = OnBg)
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = OnBg)
        }
    }
}

@Composable
private fun BottomNavBar(
    current: Tab,
    onChange: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Surface1
    ) {
        NavItem(
            selected = current == Tab.HOME,
            onClick = { onChange(Tab.HOME) },
            icon = Icons.Filled.Home,
            label = "الرئيسية"
        )
        NavItem(
            selected = current == Tab.SUPPORT,
            onClick = { onChange(Tab.SUPPORT) },
            icon = Icons.Filled.ChatBubble,
            label = "الدعم"
        )
        NavItem(
            selected = current == Tab.WALLET,
            onClick = { onChange(Tab.WALLET) },
            icon = Icons.Filled.AccountBalanceWallet,
            label = "رصيدي"
        )
        NavItem(
            selected = current == Tab.ORDERS,
            onClick = { onChange(Tab.ORDERS) },
            icon = Icons.Filled.ShoppingCart,
            label = "الطلبات"
        )
        NavItem(
            selected = current == Tab.SERVICES,
            onClick = { onChange(Tab.SERVICES) },
            icon = Icons.Filled.List,
            label = "الخدمات"
        )
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = Color.White,
            selectedTextColor   = Color.White,
            indicatorColor      = Accent.copy(alpha = 0.25f),
            unselectedIconColor = Dim,
            unselectedTextColor = Dim
        )
    )
}

/* =========================
   نافذة الإعدادات — UID + تسجيل المالك
   ========================= */
@Composable
private fun SettingsDialog(
    uid: String,
    isOwner: Boolean,
    onOwnerLoginSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var openPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        title = { Text("الإعدادات") },
        text = {
            Column {
                Text("المعرّف الخاص بك (UID):", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uid, color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        clip.setText(AnnotatedString(uid))
                    }) { Text("نسخ") }
                }
                Spacer(Modifier.height(14.dp))
                ElevatedButton(
                    onClick = { openPin = true },
                    enabled = !isOwner
                ) { Text(if (isOwner) "المالك مسجل" else "تسجيل المالك") }

                Spacer(Modifier.height(10.dp))
                Text(
                    "يُنشأ UID تلقائياً عند أول تشغيل ويتم ربطه بحسابك على الخادم.",
                    fontSize = 12.sp,
                    color = Dim
                )
            }
        }
    )

    if (openPin) {
        OwnerPinDialog(
            onSuccess = {
                openPin = false
                onOwnerLoginSuccess()
            },
            onClose = { openPin = false }
        )
    }
}

@Composable
private fun OwnerPinDialog(
    onSuccess: () -> Unit,
    onClose: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                if (pin == OWNER_PIN) onSuccess() else err = "الرمز غير صحيح"
            }) { Text("تأكيد") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } },
        title = { Text("تسجيل المالك") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("أدخل كلمة مرور المالك") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (err != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(err!!, color = Bad, fontSize = 12.sp)
                }
            }
        }
    )
}

/* =========================
   لوحة تحكم المالك — أزرار فقط
   تبقى مفتوحة بعد إعادة تشغيل التطبيق
   ========================= */
@Composable
private fun OwnerDashboard(
    onClose: () -> Unit,
    onKeepOpen: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var showBalanceResult by remember { mutableStateOf<String?>(null) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderResult by remember { mutableStateOf<String?>(null) }

    Surface(
        color = Bg.copy(alpha = 0.98f),
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لوحة تحكم المالك", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onKeepOpen) { Text("إبقاء مفتوحة") }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = onClose) { Text("خروج من وضع المالك") }
            }

            Spacer(Modifier.height(12.dp))

            // مجموعة أزرار — بالترتيب المطلوب
            val buttons = listOf(
                "تعديل الأسعار والكميات",
                "الطلبات المعلقة (الخدمات)",
                "الكارتات المعلقة",
                "طلبات شدات ببجي",
                "طلبات شحن الايتونز",
                "طلبات الارصدة المعلقة",
                "طلبات لودو المعلقة",
                "إضافة الرصيد",
                "خصم الرصيد",
                "فحص رصيد API",
                "فحص حالة طلب API",
                "عدد المستخدمين",
                "رصيد المستخدمين",
                "إدارة المشرفين",
                "حظر المستخدم",
                "الغاء حظر المستخدم",
                "اعلان البوت",
                "أكواد خدمات API",
                "نظام الإحالة",
                "شرح الخصومات",
                "المتصدرين 🎉"
            )

            buttons.forEach { label ->
                val act: () -> Unit = when (label) {
                    "فحص رصيد API" -> {
                        {
                            showBalanceResult = null
                            scope.launch {
                                val res = checkProviderBalance()
                                showBalanceResult = res
                            }
                        }
                    }
                    "فحص حالة طلب API" -> {
                        { showOrderDialog = true; orderResult = null }
                    }
                    else -> { { /* مستقبلًا تربطها بأنظمة داخل التطبيق */ } }
                }

                ElevatedButton(
                    onClick = act,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Surface1,
                        contentColor = OnBg
                    )
                ) { Text(label) }
            }

            // نتيجة فحص الرصيد
            if (showBalanceResult != null) {
                Spacer(Modifier.height(10.dp))
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        Text("نتيجة فحص رصيد API:", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(showBalanceResult!!)
                    }
                }
            }

            // حوار إدخال رقم الطلب + فحص الحالة
            if (showOrderDialog) {
                var orderId by remember { mutableStateOf("") }
                var loading by remember { mutableStateOf(false) }
                var err by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showOrderDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (orderId.isBlank()) {
                                    err = "أدخل رقم الطلب"
                                    return@TextButton
                                }
                                loading = true
                                err = null
                                orderResult = null
                                scope.launch {
                                    val res = checkOrderStatusFlexible(orderId.trim())
                                    orderResult = res
                                    loading = false
                                }
                            }
                        ) { Text(if (loading) "جارٍ..." else "تحقق") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOrderDialog = false }) { Text("إغلاق") }
                    },
                    title = { Text("فحص حالة طلب API") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = orderId,
                                onValueChange = { orderId = it },
                                label = { Text("رقم الطلب") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            if (err != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(err!!, color = Bad, fontSize = 12.sp)
                            }
                            if (orderResult != null) {
                                Spacer(Modifier.height(10.dp))
                                ElevatedCard {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("النتيجة:", fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        Text(orderResult!!)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/* =========================
   منطق UID + الشبكة + وضع المالك
   ========================= */
private fun getPrefs(ctx: Context) =
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun setOwnerMode(ctx: Context, enabled: Boolean) {
    getPrefs(ctx).edit().putBoolean(KEY_OWNER, enabled).apply()
}

private fun getOwnerMode(ctx: Context): Boolean =
    getPrefs(ctx).getBoolean(KEY_OWNER, false)

private fun loadOrCreateUid(ctx: Context): String {
    val sp = getPrefs(ctx)
    val existing = sp.getString(KEY_UID, null)
    if (existing != null) return existing
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString(KEY_UID, fresh).apply()
    return fresh
}

private suspend fun pingHealth(): Boolean? = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/health")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
        }
        con.connect()
        (con.responseCode in 200..299)
    } catch (_: Exception) {
        false
    }
}

private suspend fun tryUpsertUid(uid: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/api/users/upsert")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = """{"uid":"$uid"}"""
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.inputStream.bufferedReader().use(BufferedReader::readText)
    } catch (_: Exception) {
        // تجاهل الفشل — لا يؤثر على البناء
    }
}

/* ----- فحص رصيد المزود عبر الباكند ----- */
private suspend fun checkProviderBalance(): String = withContext(Dispatchers.IO) {
    val endpoints = listOf(
        "$API_BASE/api/smm/balance",
        "$API_BASE/api/smm/get-balance",
        "$API_BASE/api/smm/get_balance"
    )
    for (ep in endpoints) {
        try {
            val url = URL(ep)
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            val code = con.responseCode
            val body = (if (code in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code in 200..299) {
                // محاولة فهم JSON
                return@withContext try {
                    val j = JSONObject(body)
                    val ok = j.optBoolean("ok", false)
                    if (ok) {
                        val bal = j.optString("balance", j.optString("result", body))
                        "الرصيد: $bal"
                    } else {
                        "فشل: $body"
                    }
                } catch (_: Exception) {
                    // قد يكون body نصًّا بسيطاً
                    "الرصيد: $body"
                }
            }
        } catch (_: Exception) { /* جرّب التالي */ }
    }
    return@withContext "تعذر الحصول على الرصيد من الخادم. تأكد من وجود مسار رصيد صالح."
}

/* ----- فحص حالة طلب المزود عبر الباكند (مسارات مرنة) ----- */
private suspend fun checkOrderStatusFlexible(orderId: String): String = withContext(Dispatchers.IO) {
    val enc = URLEncoder.encode(orderId, "UTF-8")
    val getEndpoints = listOf(
        "$API_BASE/api/smm/order-status?order_id=$enc",
        "$API_BASE/api/smm/order_status?order_id=$enc"
    )
    val postEndpoints = listOf(
        "$API_BASE/api/smm/order-status",
        "$API_BASE/api/smm/order_status"
    )

    // 1) جرّب GET
    for (ep in getEndpoints) {
        try {
            val url = URL(ep)
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
            }
            val code = con.responseCode
            val body = (if (code in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code in 200..299) {
                return@withContext parseOrderStatusBody(body)
            } else {
                // تابع المحاولة التالية
            }
        } catch (_: Exception) { /* جرّب التالي */ }
    }

    // 2) جرّب POST JSON
    for (ep in postEndpoints) {
        try {
            val url = URL(ep)
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val bodyOut = """{"order_id":"$orderId"}"""
            OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(bodyOut) }

            val code = con.responseCode
            val body = (if (code in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code in 200..299) {
                return@withContext parseOrderStatusBody(body)
            } else {
                // جمع خطأ واضح
                try {
                    val err = JSONObject(body)
                    val msg = err.optString("error", body)
                    return@withContext "فشل الطلب ($code): $msg"
                } catch (_: Exception) {
                    return@withContext "فشل الطلب ($code): $body"
                }
            }
        } catch (e: Exception) {
            // آخر محاولة فاشلة
            // تابع الحلقة
        }
    }

    return@withContext "تعذر التحقق من حالة الطلب. تأكد من مسار الباكند وإعدادات المزود ورقم الطلب."
}

private fun parseOrderStatusBody(body: String): String {
    return try {
        val j = JSONObject(body)
        val ok = j.optBoolean("ok", false)
        if (ok) {
            val result = j.optJSONObject("result") ?: j
            val status = result.optString("status", "غير معروفة")
            val charge = result.optString("charge", "-")
            val remains = result.optString("remains", "-")
            "الحالة: $status\nالكلفة: $charge\nالمتبقي: $remains"
        } else {
            val msg = j.optString("error", body)
            "فشل: $msg"
        }
    } catch (_: Exception) {
        // ربما نص بسيط
        if (body.isBlank()) "لا يوجد رد" else body
    }
}
