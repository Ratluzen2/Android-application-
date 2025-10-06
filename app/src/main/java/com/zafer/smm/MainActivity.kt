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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/* =========================
   إعدادات عامة
   ========================= */
private const val API_BASE =
    "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com" // عدّلها إن لزم

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
   تبويبات
   ========================= */
private enum class Tab { HOME, SUPPORT, WALLET, ORDERS, SERVICES, OWNER }

/* =========================
   جذر التطبيق
   ========================= */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }

    // حالة السيرفر
    var online by remember { mutableStateOf<Boolean?>(null) }

    // حالة المالك محفوظة
    var isOwner by rememberSaveable { mutableStateOf(loadOwnerMode(ctx)) }
    var current by rememberSaveable { mutableStateOf(loadLastTab(ctx, if (isOwner) Tab.OWNER else Tab.HOME)) }

    // فحص السيرفر دوري + تسجيل UID
    LaunchedEffect(Unit) {
        scope.launch { tryUpsertUid(uid) }
        while (true) {
            online = pingHealth()
            delay(20_000)
        }
    }

    // إن كان المالك مفعّلًا؛ تأكد أن الشاشة تبقى على OWNER
    LaunchedEffect(isOwner) {
        if (isOwner) {
            current = Tab.OWNER
            saveLastTab(ctx, Tab.OWNER)
        }
        saveOwnerMode(ctx, isOwner)
    }

    // الحاوية
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        when (current) {
            Tab.HOME     -> EmptyScreen()
            Tab.SUPPORT  -> SupportScreen()
            Tab.WALLET   -> EmptyScreen()
            Tab.ORDERS   -> EmptyScreen()
            Tab.SERVICES -> EmptyScreen()
            Tab.OWNER    -> OwnerDashboard()
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
            onChange = {
                current = it
                saveLastTab(ctx, it)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            isOwner = isOwner
        )
    }

    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            isOwner = isOwner,
            onDismiss = { settingsOpen = false },
            onOwnerLogin = { pin ->
                if (pin == OWNER_PIN) {
                    isOwner = true
                    saveOwnerMode(ctx, true)   // <-- استبدلنا LocalContext.current بـ ctx
                }
            },
            onOwnerLogout = {
                isOwner = false
                saveOwnerMode(ctx, false)     // <-- استبدلنا LocalContext.current بـ ctx
            }
        )
    }
}

/* =========================
   عناصر الواجهة العامة
   ========================= */
@Composable
private fun EmptyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    )
}

/* شاشة الدعم */
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
    modifier: Modifier = Modifier,
    isOwner: Boolean
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
        if (isOwner) {
            NavItem(
                selected = current == Tab.OWNER,
                onClick = { onChange(Tab.OWNER) },
                icon = Icons.Filled.Settings,
                label = "المالك"
            )
        } else {
            NavItem(
                selected = current == Tab.SERVICES,
                onClick = { onChange(Tab.SERVICES) },
                icon = Icons.Filled.List,
                label = "الخدمات"
            )
        }
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
    onDismiss: () -> Unit,
    onOwnerLogin: (String) -> Unit,
    onOwnerLogout: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var showPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }

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
                    OutlinedButton(onClick = { clip.setText(AnnotatedString(uid)) }) { Text("نسخ") }
                }
                Spacer(Modifier.height(10.dp))

                Divider(Modifier.padding(vertical = 8.dp))

                if (!isOwner) {
                    ElevatedButton(onClick = { showPin = true }) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل المالك")
                    }
                } else {
                    OutlinedButton(onClick = onOwnerLogout) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إلغاء وضع المالك")
                    }
                }
            }
        }
    )

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text("أدخل كلمة المرور") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    placeholder = { Text("PIN") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onOwnerLogin(pin)
                    showPin = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showPin = false }) { Text("إلغاء") } }
        )
    }
}

/* =========================
   لوحة تحكم المالك (أزرار + استدعاءات API)
   ========================= */
@Composable
private fun OwnerDashboard() {
    val scope = rememberCoroutineScope()

    var showMsg by remember { mutableStateOf<Triple<String, String?, String?>?>(null) }

    // حوار إدخال رقم الطلب
    var askOrder by remember { mutableStateOf(false) }
    var orderId by remember { mutableStateOf("") }

    fun showInfo(title: String, msg: String?, raw: String? = null) {
        showMsg = Triple(title, msg, raw)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // عرض الأزرار عموديًا
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

        buttons.forEach { title ->
            OwnerButton(
                title = title,
                onClick = {
                    when (title) {
                        "فحص رصيد API" -> {
                            scope.launch {
                                val res = checkProviderBalance()
                                if (res.first) {
                                    val j = res.second!!
                                    val bal = j.optString("balance", "?")
                                    val cur = j.optString("currency", "")
                                    showInfo("رصيد المزود", "الرصيد: $bal $cur", res.third)
                                } else {
                                    val reason = res.third ?: "فشل غير معروف"
                                    showInfo("فشل", "تعذر الحصول على الرصيد من الخادم.\n$reason", res.third)
                                }
                            }
                        }
                        "فحص حالة طلب API" -> {
                            askOrder = true
                        }
                        else -> {
                            // placeholder
                            showInfo(title, "سيتم ربط هذا الزر لاحقًا.")
                        }
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    // إدخال رقم الطلب
    if (askOrder) {
        AlertDialog(
            onDismissRequest = { askOrder = false },
            title = { Text("أدخل رقم الطلب") },
            text = {
                OutlinedTextField(
                    value = orderId,
                    onValueChange = { orderId = it },
                    placeholder = { Text("مثال: 123456") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = orderId.trim()
                    if (id.isNotEmpty()) {
                        askOrder = false
                        orderId = ""
                        scope.launch {
                            val res = checkOrderStatus(id)
                            if (res.first) {
                                val j = res.second!!
                                val status = j.optString("status", "?")
                                val remains = j.optString("remains", "?")
                                val charge = j.optString("charge", "?")
                                val currency = j.optString("currency", "")
                                showInfo(
                                    "حالة الطلب",
                                    "الحالة: $status\nالمتبقي: $remains\nالتكلفة: $charge $currency",
                                    res.third
                                )
                            } else {
                                val reason = res.third ?: "فشل غير معروف"
                                showInfo(
                                    "فشل",
                                    "تعذر فحص حالة الطلب.\n$reason",
                                    res.third
                                )
                            }
                        }
                    }
                }) { Text("تحقق") }
            },
            dismissButton = { TextButton(onClick = { askOrder = false }) { Text("إلغاء") } }
        )
    }

    // نافذة نتيجة عامة
    showMsg?.let { (title, msg, raw) ->
        AlertDialog(
            onDismissRequest = { showMsg = null },
            title = { Text(title) },
            text = {
                Column {
                    Text(msg ?: "")
                    raw?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Raw:", fontWeight = FontWeight.SemiBold, color = Dim, fontSize = 12.sp)
                        Text(it.take(800), fontSize = 12.sp, color = Dim)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMsg = null }) { Text("إغلاق") } }
        )
    }
}

/** زر مالك منسق */
@Composable
private fun OwnerButton(title: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Surface1,
            contentColor = OnBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
    }
}

/* =========================
   تخزين محلي لحالة المالك والتبويب الأخير
   ========================= */
private fun saveOwnerMode(ctx: Context, enabled: Boolean) {
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("owner_mode", enabled).apply()
}
private fun loadOwnerMode(ctx: Context): Boolean =
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("owner_mode", false)

private fun saveLastTab(ctx: Context, tab: Tab) {
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit().putString("last_tab", tab.name).apply()
}
private fun loadLastTab(ctx: Context, defaultTab: Tab): Tab {
    val name = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getString("last_tab", null)
    return runCatching { if (name != null) Tab.valueOf(name) else defaultTab }.getOrDefault(defaultTab)
}

/* =========================
   منطق UID + الشبكة
   ========================= */
private fun loadOrCreateUid(ctx: Context): String {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val existing = sp.getString("uid", null)
    if (existing != null) return existing
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString("uid", fresh).apply()
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
    } catch (_: Exception) { /* تجاهل */ }
}

/* =========================
   استدعاءات الباكند للمزوّد KD1S عبر باكندك
   ========================= */
private suspend fun checkProviderBalance(): Triple<Boolean, JSONObject?, String?> = withContext(Dispatchers.IO) {
    val url = URL("$API_BASE/api/provider/balance")
    try {
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        val code = con.responseCode
        val stream = if (code in 200..299) con.inputStream else con.errorStream
        val raw = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        if (code in 200..299) {
            val j = JSONObject(raw)
            Triple(true, j, raw)
        } else {
            Triple(false, null, raw)
        }
    } catch (e: Exception) {
        Triple(false, null, e.message ?: "Exception")
    }
}

private suspend fun checkOrderStatus(orderId: String): Triple<Boolean, JSONObject?, String?> = withContext(Dispatchers.IO) {
    val url = URL("$API_BASE/api/provider/order/status?order_id=$orderId")
    try {
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        val code = con.responseCode
        val stream = if (code in 200..299) con.inputStream else con.errorStream
        val raw = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        if (code in 200..299) {
            val j = JSONObject(raw).optJSONObject("data") ?: JSONObject(raw)
            Triple(true, j, raw)
        } else {
            Triple(false, null, raw)
        }
    } catch (e: Exception) {
        Triple(false, null, e.message ?: "Exception")
    }
}
