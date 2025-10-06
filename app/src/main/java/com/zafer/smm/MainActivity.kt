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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
   شريط سفلي وشاشات
   ========================= */
private enum class Tab { HOME, SUPPORT, WALLET, ORDERS, SERVICES }

@Composable
fun AppRoot() {
    val ctx = LocalContext.current

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by rememberSaveable { mutableStateOf(loadOrCreateUid(ctx)) }

    // حالة السيرفر
    var online by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // وضع المالك — محفوظ في SharedPreferences ليبقى بعد إغلاق التطبيق
    var ownerMode by rememberSaveable { mutableStateOf(loadOwnerMode(ctx)) }
    var ownerOpen by rememberSaveable { mutableStateOf(ownerMode) } // إبقاء لوحة المالك مفتوحة بين الجلسات

    // فحص السيرفر دوري + تسجيل UID
    LaunchedEffect(Unit) {
        tryUpsertUid(uid)
        while (true) {
            online = pingHealth()
            delay(20_000)
        }
    }

    var current by rememberSaveable { mutableStateOf(Tab.HOME) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    // الحاوية الرئيسية
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // محتوى التبويبات
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

        // لوحة المالك (تظهر فوق الواجهة وتبقى مفتوحة إن كان ownerOpen = true)
        if (ownerOpen) {
            OwnerDashboard(
                onClose = {
                    // إغلاق يدوي فقط (لا نغيّر وضع المالك)
                    ownerOpen = false
                },
                onLogoutOwner = {
                    ownerMode = false
                    saveOwnerMode(ctx, false)
                    ownerOpen = false
                }
            )
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            isOwner = ownerMode,
            onDismiss = { settingsOpen = false },
            onOwnerLogin = {
                ownerMode = true
                saveOwnerMode(ctx, true)
                ownerOpen = true // افتح لوحة المالك فوراً
            },
            onOwnerLogout = {
                ownerMode = false
                saveOwnerMode(ctx, false)
                ownerOpen = false
            }
        )
    }
}

/* =========================
   عناصر الواجهة الأساسية
   ========================= */
@Composable
private fun EmptyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    )
}

/* شاشة الدعم (واتساب/تيليجرام) */
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
    onDismiss: () -> Unit,
    onOwnerLogin: () -> Unit,
    onOwnerLogout: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var askPin by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }

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

                Spacer(Modifier.height(18.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                Text("وضع المالك", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                if (!isOwner) {
                    Button(
                        onClick = { askPin = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
                    ) { Text("تسجيل المالك") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Good)
                        Spacer(Modifier.width(8.dp))
                        Text("أنت في وضع المالك الآن", color = Good)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOwnerLogout) { Text("الخروج من وضع المالك") }
                }

                if (askPin) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; pinError = null },
                        label = { Text("كلمة المرور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null
                    )
                    pinError?.let { Text(it, color = Bad, fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { askPin = false; pin = ""; pinError = null }) { Text("إلغاء") }
                        Button(
                            onClick = {
                                if (pin == OWNER_PIN) {
                                    onOwnerLogin()
                                    askPin = false
                                    pin = ""
                                } else {
                                    pinError = "كلمة المرور غير صحيحة"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
                        ) { Text("تأكيد") }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "يُنشأ UID تلقائياً عند أول تشغيل ويتم ربطه بحسابك على الخادم.",
                    fontSize = 12.sp,
                    color = Dim
                )
            }
        }
    )
}

/* =========================
   لوحة تحكم المالك
   ========================= */
@Composable
private fun OwnerDashboard(
    onClose: () -> Unit,
    onLogoutOwner: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var showBalanceResult by rememberSaveable { mutableStateOf<String?>(null) }
    var showOrderDialog by rememberSaveable { mutableStateOf(false) }
    var orderIdInput by rememberSaveable { mutableStateOf("") }
    var orderStatusResult by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        color = Bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("إغلاق") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onLogoutOwner) { Text("تسجيل خروج المالك") }
            }

            Spacer(Modifier.height(12.dp))

            // أزرار فقط (شكل)
            OwnerButton("تعديل الأسعار والكميات")
            OwnerButton("الطلبات المعلقة (الخدمات)")
            OwnerButton("الكارتات المعلقة")
            OwnerButton("طلبات شدات ببجي")
            OwnerButton("طلبات شحن الايتونز")
            OwnerButton("طلبات الارصدة المعلقة")
            OwnerButton("طلبات لودو المعلقة")
            OwnerButton("إضافة الرصيد")
            OwnerButton("خصم الرصيد")

            // فحص رصيد API — فعّال
            OwnerButton("فحص رصيد API") {
                scope.launch {
                    val res = checkProviderBalance()
                    showBalanceResult = res
                }
            }

            // فحص حالة طلب API — يطلب رقم طلب ثم يستعلم
            OwnerButton("فحص حالة طلب API") {
                showOrderDialog = true
            }

            OwnerButton("عدد المستخدمين")
            OwnerButton("رصيد المستخدمين")
            OwnerButton("إدارة المشرفين")
            OwnerButton("حظر المستخدم")
            OwnerButton("الغاء حظر المستخدم")
            OwnerButton("اعلان البوت")
            OwnerButton("أكواد خدمات API")
            OwnerButton("نظام الإحالة")
            OwnerButton("شرح الخصومات")
            OwnerButton("المتصدرين 🎉")
        }
    }

    // Dialog نتيجة رصيد API
    showBalanceResult?.let { txt ->
        AlertDialog(
            onDismissRequest = { showBalanceResult = null },
            confirmButton = { TextButton(onClick = { showBalanceResult = null }) { Text("حسناً") } },
            title = { Text("نتيجة فحص الرصيد") },
            text = { Text(txt) }
        )
    }

    // Dialog إدخال رقم الطلب
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("فحص حالة طلب API") },
            text = {
                Column {
                    Text("أدخل رقم الطلب للتحقق من حالته:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = orderIdInput,
                        onValueChange = { orderIdInput = it },
                        singleLine = true,
                        label = { Text("رقم الطلب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = orderIdInput.trim()
                    if (id.isNotEmpty()) {
                        scope.launch {
                            val res = checkOrderStatus(id)
                            orderStatusResult = res
                        }
                    }
                    showOrderDialog = false
                }) { Text("تحقق") }
            },
            dismissButton = {
                TextButton(onClick = { showOrderDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Dialog نتيجة حالة الطلب
    orderStatusResult?.let { txt ->
        AlertDialog(
            onDismissRequest = { orderStatusResult = null },
            confirmButton = { TextButton(onClick = { orderStatusResult = null }) { Text("حسناً") } },
            title = { Text("نتيجة فحص الطلب") },
            text = { Text(txt) }
        )
    }
}

@Composable
private fun OwnerButton(title: String, onClick: (() -> Unit)? = null) {
    val btnColors = ButtonDefaults.buttonColors(
        containerColor = Surface1,
        contentColor = OnBg
    )
    ElevatedButton(
        onClick = { onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = btnColors
    ) {
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Accent)
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
    }
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

private fun loadOwnerMode(ctx: Context): Boolean {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return sp.getBoolean("owner_mode", false)
}

private fun saveOwnerMode(ctx: Context, enabled: Boolean) {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    sp.edit().putBoolean("owner_mode", enabled).apply()
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

/* =========================
   مزود الخدمات — رصيد و حالة طلب
   ========================= */
private suspend fun checkProviderBalance(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/api/admin/balance")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
        }
        val code = con.responseCode
        val body = con.inputStream.bufferedReader().use(BufferedReader::readText)
        "HTTP $code\n$body"
    } catch (e: Exception) {
        "تعذُّر الحصول على الرصيد من الخادم.\n${e.message ?: "خطأ غير معروف"}"
    }
}

private suspend fun checkOrderStatus(orderId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/api/admin/order-status?order_id=$orderId")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        val code = con.responseCode
        val body = con.inputStream.bufferedReader().use(BufferedReader::readText)
        "HTTP $code\n$body"
    } catch (e: Exception) {
        "تعذُّر فحص حالة الطلب.\n${e.message ?: "خطأ غير معروف"}"
    }
}
