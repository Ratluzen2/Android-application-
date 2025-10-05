@file:Suppress("UnusedImport")

package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    val ctx = remember { LocalAppContext.current }
    val scope = rememberCoroutineScope()

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }

    // حالة السيرفر
    var online by remember { mutableStateOf<Boolean?>(null) }

    // فحص السيرفر دوري
    LaunchedEffect(Unit) {
        // أولاً سجّل/حدّث UID في الخادم (غير معطل لو فشل)
        scope.launch { tryUpsertUid(uid) }
        // مراقبة الصحة كل 20 ثانية
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
        // محتوى كل تبويب — واجهة فارغة بسيطة (حسب طلبك)
        when (current) {
            Tab.HOME     -> EmptyScreen()
            Tab.SUPPORT  -> EmptyScreen()
            Tab.WALLET   -> EmptyScreen()
            Tab.ORDERS   -> EmptyScreen()
            Tab.SERVICES -> EmptyScreen()
        }

        // حالة السيرفر أعلى يمين + زر إعدادات لعرض UID فقط
        ServerStatusPill(
            online = online,
            onOpenSettings = { settingsOpen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
        )

        // الشريط السفلي (الأيقونات + الأسماء)
        BottomNavBar(
            current = current,
            onChange = { current = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (settingsOpen) {
        SettingsDialog(uid = uid, onDismiss = { settingsOpen = false })
    }
}

/* =========================
   عناصر الواجهة
   ========================= */
@Composable
private fun EmptyScreen() {
    // شاشة فارغة متناسقة اللون
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    )
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
   نافذة الإعدادات — UID فقط
   ========================= */
@Composable
private fun SettingsDialog(uid: String, onDismiss: () -> Unit) {
    val clip: ClipboardManager = LocalClipboardManager.current
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
        // لا يهم الرد حالياً
    } catch (_: Exception) {
        // تجاهل الفشل — سيتم تكراره لاحقاً إذا رغبت
    }
}

/* =========================
   Local Context Helper
   ========================= */
private object LocalAppContext {
    val current: Context
        @Composable get() = androidx.compose.ui.platform.LocalContext.current
}
