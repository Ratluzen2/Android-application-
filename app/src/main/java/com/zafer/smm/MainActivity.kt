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

/* مفاتيح التخزين المحلي */
private const val SP_NAME = "app_prefs"
private const val SP_UID = "uid"
private const val SP_OWNER = "owner_enabled"
private const val OWNER_PIN = "2000"

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }

    // وضع المالك محفوظ دائماً
    var ownerEnabled by remember {
        mutableStateOf(ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getBoolean(SP_OWNER, false))
    }

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

    // حوارات لوحة المالك
    var showOwnerLogin by remember { mutableStateOf(false) }
    var showOwnerPanel by remember { mutableStateOf(false) }
    var showOrderStatus by remember { mutableStateOf(false) } // فحص حالة الطلب
    var showBalanceDialog by remember { mutableStateOf(false) }
    var balanceResult by remember { mutableStateOf<String?>(null) }

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

        // حالة السيرفر أعلى يمين + زر إعدادات (يعرض UID و"تسجيل المالك")
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
    }

    /* ====== الحوارات ====== */

    // إعدادات: تُظهر UID + زر تسجيل المالك / لوحة المالك
    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            ownerEnabled = ownerEnabled,
            onOwnerLogin = { settingsOpen = false; showOwnerLogin = true },
            onOpenOwnerPanel = { settingsOpen = false; showOwnerPanel = true },
            onDismiss = { settingsOpen = false }
        )
    }

    // تسجيل المالك
    OwnerLoginDialog(
        visible = showOwnerLogin,
        onCancel = { showOwnerLogin = false },
        onSuccess = {
            showOwnerLogin = false
            val sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.edit().putBoolean(SP_OWNER, true).apply()
            ownerEnabled = true
            showOwnerPanel = true
        }
    )

    // لوحة المالك
    OwnerPanelDialog(
        visible = showOwnerPanel && ownerEnabled,
        onClose = { showOwnerPanel = false },
        onCheckBalance = {
            // فحص رصيد API
            scope.launch {
                balanceResult = "جارٍ التحقق..."
                val res = getSmmBalanceFromServer()
                balanceResult = res
                showBalanceDialog = true
            }
        },
        onCheckOrderStatus = {
            // فتح حوار إدخال رقم الطلب
            showOrderStatus = true
        }
    )

    // نتيجة فحص الرصيد
    if (showBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("نتيجة فحص رصيد API") },
            text = { Text(balanceResult ?: "—") },
            confirmButton = { TextButton(onClick = { showBalanceDialog = false }) { Text("حسناً") } }
        )
    }

    // حوار فحص حالة الطلب (إدخال رقم الطلب ثم استعلام السيرفر)
    OrderStatusDialog(
        visible = showOrderStatus,
        onClose = { showOrderStatus = false }
    )
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

/* دعم */
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
            .clickable { onClick() }
            .padding(horizontal = 0.dp),
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
    ownerEnabled: Boolean,
    onOwnerLogin: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onDismiss: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ownerEnabled) {
                    TextButton(onClick = onOpenOwnerPanel) { Text("لوحة المالك") }
                } else {
                    TextButton(onClick = onOwnerLogin) { Text("تسجيل المالك") }
                }
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
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
   حوار تسجيل المالك (PIN = 2000)
   ========================= */
@Composable
private fun OwnerLoginDialog(
    visible: Boolean,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    if (!visible) return
    var pin by rememberSaveable { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("تسجيل المالك") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; err = null },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (err != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin == OWNER_PIN) onSuccess() else err = "كلمة المرور غير صحيحة"
            }) { Text("تأكيد") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("إلغاء") }
        }
    )
}

/* =========================
   حوار لوحة المالك — أزرار فقط
   ========================= */
@Composable
private fun OwnerPanelDialog(
    visible: Boolean,
    onClose: () -> Unit,
    onCheckBalance: () -> Unit,
    onCheckOrderStatus: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("لوحة تحكم المالك") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OwnerActionButton("تعديل الأسعار والكميات", Icons.Filled.Tune) {}
                OwnerActionButton("الطلبات المعلقة (الخدمات)", Icons.Filled.List) {}
                OwnerActionButton("الكارتات المعلقة", Icons.Filled.CreditCard) {}
                OwnerActionButton("طلبات شدات ببجي", Icons.Filled.SportsEsports) {}
                OwnerActionButton("طلبات شحن الايتونز", Icons.Filled.Apple) {}
                OwnerActionButton("طلبات الارصدة المعلقة", Icons.Filled.AccountBalanceWallet) {}
                OwnerActionButton("طلبات لودو المعلقة", Icons.Filled.Casino) {}
                OwnerActionButton("إضافة الرصيد", Icons.Filled.AddCircle) {}
                OwnerActionButton("خصم الرصيد", Icons.Filled.RemoveCircle) {}
                OwnerActionButton("فحص رصيد API", Icons.Filled.Verified) { onCheckBalance() }
                OwnerActionButton("فحص حالة طلب API", Icons.Filled.Search) { onCheckOrderStatus() }
                OwnerActionButton("عدد المستخدمين", Icons.Filled.Groups) {}
                OwnerActionButton("رصيد المستخدمين", Icons.Filled.AccountBox) {}
                OwnerActionButton("إدارة المشرفين", Icons.Filled.AdminPanelSettings) {}
                OwnerActionButton("حظر المستخدم", Icons.Filled.Block) {}
                OwnerActionButton("الغاء حظر المستخدم", Icons.Filled.CheckCircle) {}
                OwnerActionButton("اعلان البوت", Icons.Filled.Campaign) {}
                OwnerActionButton("أكواد خدمات API", Icons.Filled.Code) {}
                OwnerActionButton("نظام الإحالة", Icons.Filled.Share) {}
                OwnerActionButton("شرح الخصومات", Icons.Filled.Info) {}
                OwnerActionButton("المتصدرين 🎉", Icons.Filled.EmojiEvents) {}
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("إغلاق") }
        }
    )
}

@Composable
private fun OwnerActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Surface1,
            contentColor = OnBg
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = Accent)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

/* =========================
   حوار فحص حالة الطلب (إدخال رقم الطلب)
   ========================= */
@Composable
private fun OrderStatusDialog(
    visible: Boolean,
    onClose: () -> Unit
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var orderId by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!loading) onClose() },
        title = { Text("فحص حالة الطلب (API)") },
        text = {
            Column {
                OutlinedTextField(
                    value = orderId,
                    onValueChange = { orderId = it },
                    label = { Text("رقم الطلب") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (!loading && result != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(result!!, color = Dim, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = orderId.isNotBlank() && !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        result = null
                        result = getOrderStatusFromServer(orderId)
                        loading = false
                    }
                }
            ) { Text("تحقق") }
        },
        dismissButton = {
            TextButton(enabled = !loading, onClick = onClose) { Text("إغلاق") }
        }
    )
}

/* =========================
   منطق UID + الشبكة
   ========================= */
private fun loadOrCreateUid(ctx: Context): String {
    val sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    val existing = sp.getString(SP_UID, null)
    if (existing != null) return existing
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString(SP_UID, fresh).apply()
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

/* فحص رصيد API */
private suspend fun getSmmBalanceFromServer(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/api/smm/balance")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
        }
        val code = con.responseCode
        val body = con.inputStream.bufferedReader().use { it.readText() }
        if (code in 200..299) {
            return@withContext runCatching {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val bal = json.opt("balance")?.toString() ?: "-"
                    "الرصيد: $bal"
                } else {
                    json.optString("detail", json.optString("message", body))
                }
            }.getOrElse { body }
        } else {
            "فشل الطلب (${code}): $body"
        }
    } catch (e: Exception) {
        "خطأ في الاتصال: ${e.message}"
    }
}

/* فحص حالة الطلب API */
private suspend fun getOrderStatusFromServer(orderId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/api/smm/order-status?order_id=${orderId.trim()}")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
        }
        val code = con.responseCode
        val body = con.inputStream.bufferedReader().use { it.readText() }
        if (code in 200..299) {
            return@withContext runCatching {
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val res = json.optJSONObject("result")
                    val status = res?.optString("status") ?: "غير معروف"
                    val charge = res?.optString("charge") ?: "-"
                    val remains = res?.optString("remains") ?: "-"
                    "الحالة: $status\nالكلفة: $charge\nالمتبقي: $remains"
                } else {
                    json.optString("detail", json.optString("message", body))
                }
            }.getOrElse { body }
        } else {
            "فشل الطلب (${code}): $body"
        }
    } catch (e: Exception) {
        "خطأ في الاتصال: ${e.message}"
    }
}
