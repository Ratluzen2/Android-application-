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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    // حالة السيرفر
    var online by remember { mutableStateOf<Boolean?>(null) }

    // حالة المالك/لوحته
    var isOwner by rememberSaveable { mutableStateOf(false) }
    var askOwnerPin by remember { mutableStateOf(false) }
    var showOwnerDashboard by remember { mutableStateOf(false) }

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
    }

    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            onOwnerLoginClick = { askOwnerPin = true },
            onDismiss = { settingsOpen = false }
        )
    }

    // نافذة إدخال كلمة مرور المالك
    if (askOwnerPin) {
        OwnerLoginDialog(
            onCancel = { askOwnerPin = false },
            onSubmit = { pin ->
                if (pin == "2000") {
                    isOwner = true
                    askOwnerPin = false
                    showOwnerDashboard = true
                }
            }
        )
    }

    // واجهة لوحة تحكم المالك (أزرار فقط)
    if (showOwnerDashboard && isOwner) {
        OwnerDashboard(onClose = { showOwnerDashboard = false })
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
   شاشة الدعم
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
private fun SettingsDialog(uid: String, onOwnerLoginClick: () -> Unit, onDismiss: () -> Unit) {
    val clip: ClipboardManager = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(16.dp))
                Divider(color = Dim.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                // زر تسجيل المالك
                ElevatedButton(
                    onClick = onOwnerLoginClick,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Accent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تسجيل المالك")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "خاص بالمالك فقط، يتطلب كلمة مرور.",
                    fontSize = 12.sp,
                    color = Dim
                )
            }
        }
    )
}

/* =========================
   نافذة إدخال كلمة مرور المالك
   ========================= */
@Composable
private fun OwnerLoginDialog(
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("تسجيل المالك") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        error = null
                    },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.isBlank()) {
                    error = "أدخل كلمة المرور"
                } else {
                    onSubmit(pin)
                    if (pin != "2000") error = "كلمة المرور غير صحيحة"
                }
            }) { Text("تأكيد") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("إلغاء") }
        }
    )
}

/* =========================
   لوحة تحكم المالك — أزرار فقط
   ========================= */
@Composable
private fun OwnerDashboard(onClose: () -> Unit) {
    val actions = listOf(
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

    // صفحة كاملة فوق التطبيق
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        color = Bg
    ) {
        Column(Modifier.fillMaxSize()) {
            // شريط علوي بسيط داخل الصفحة نفسها (ليس TopAppBar النظامي)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = OnBg)
                }
                Spacer(Modifier.width(6.dp))
                Text("لوحة تحكم المالك", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            // شبكة أزرار بشكل صفين (عمودين)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // اصنع أزواجًا (2 في كل صف)
                actions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { label ->
                            OwnerActionButton(
                                label = label,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerActionButton(label: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable { /* لا شيء الآن */ },
        colors = CardDefaults.elevatedCardColors(containerColor = Surface1),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Accent
                )
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
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
