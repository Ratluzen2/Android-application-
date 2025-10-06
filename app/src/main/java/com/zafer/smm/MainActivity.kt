
@file:Suppress("UnusedImport", "NAME_SHADOWING")

package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random

/* ==========================================================
   إعدادات عامة — عدّل عنوان خادمك هنا
   ========================================================== */
private const val API_BASE = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"

/* ==========================================================
   Theme
   ========================================================== */
private val Bg       = Color(0xFF101214)
private val Surface1 = Color(0xFF171B20)
private val OnBg     = Color(0xFFECEFF4)
private val Accent   = Color(0xFFB388FF)
private val Good     = Color(0xFF2E7D32)
private val Bad      = Color(0xFFC62828)
private val Dim      = Color(0xFF9AA3AB)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Bg,
            surface = Surface1,
            onBackground = OnBg,
            onSurface = OnBg
        ),
        typography = Typography(),
        content = content
    )
}

/* ==========================================================
   نماذج
   ========================================================== */
enum class Tab { HOME, SERVICES, WALLET, ORDERS, SUPPORT }

data class AppNotice(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val ts: Long = System.currentTimeMillis(),
)

data class ServiceDef(
    val code: Long,          // رقم الخدمة (كما في البوت)
    val name: String,        // الاسم العربي للعرض
    val min: Int,
    val max: Int,
    val pricePerK: Double,   // السعر لكل 1000
    val category: String
)

data class AppOrder(
    val id: String,
    val uid: String,
    val serviceCode: Long,
    val serviceName: String,
    val quantity: Int,
    val price: Double,
    val linkOrId: String,
    val status: String,
    val createdAt: Long
)

/* ==========================================================
   كتالوج الخدمات + الأقسام
   ========================================================== */
// أكواد الخدمات من رسالتك:
private val servicesCatalog = listOf(
    // المتابعين
    ServiceDef(16256,   "متابعين تيكتوك",   100, 1_000_000, 3.5, "المتابعين"),
    ServiceDef(16267,   "متابعين انستغرام", 100, 1_000_000, 3.0, "المتابعين"),
    // اللايكات
    ServiceDef(12320,   "لايكات تيكتوك",    100, 1_000_000, 1.0, "الايكات"),
    ServiceDef(1_066_500,"لايكات انستغرام", 100, 1_000_000, 1.0, "الايكات"),
    // المشاهدات
    ServiceDef(9448,    "مشاهدات تيكتوك",    100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef(64_686_464,"مشاهدات انستغرام",100, 1_000_000, 0.1, "المشاهدات"),
    // البث المباشر
    ServiceDef(14442,   "مشاهدات بث تيكتوك", 100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef(646_464, "مشاهدات بث انستا",  100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    // رفع سكور
    ServiceDef(14662,   "رفع سكور البث",     100, 1_000_000, 2.0, "رفع سكور تيكتوك"),
    // تلجرام
    ServiceDef(955_656, "اعضاء قنوات تلي",   100, 1_000_000, 3.0, "خدمات التليجرام"),
    ServiceDef(644_656, "اعضاء كروبات تلي",  100, 1_000_000, 3.0, "خدمات التليجرام"),
)

private val serviceCategories = listOf(
    "قسم المتابعين",
    "قسم الايكات",
    "قسم المشاهدات",
    "قسم مشاهدات البث المباشر",
    "قسم رفع سكور تيكتوك",
    "قسم خدمات التليجرام",
    "قسم شراء رصيد ايتونز",
    "قسم شراء رصيد هاتف",
    "قسم شحن شدات ببجي",
    "قسم خدمات الودو"
)

/* ==========================================================
   Activity
   ========================================================== */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppRoot() } }
    }
}

/* ==========================================================
   Root Composable
   ========================================================== */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // UID محلي فقط (يُسجَّل في الخادم — مصدر الحقيقة هو الخادم)
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var ownerMode by remember { mutableStateOf(loadOwnerMode(ctx)) }

    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    var userNotices by remember { mutableStateOf<List<AppNotice>>(emptyList()) }
    var ownerNotices by remember { mutableStateOf<List<AppNotice>>(emptyList()) }

    var showNoticeCenter by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(Tab.HOME) }

    // فحص الصحة + تسجيل UID + تحميل إشعارات
    LaunchedEffect(uid, ownerMode) {
        apiUpsertUid(uid)
        while (true) {
            serverOnline = apiPing()
            // سحب إشعارات
            if (ownerMode) {
                ownerNotices = apiFetchNotices(owner = true, uid = uid)
            } else {
                userNotices = apiFetchNotices(owner = false, uid = uid)
            }
            delay(15_000)
        }
    }

    val unreadCount = if (ownerMode) ownerNotices.size else userNotices.size

    Box(Modifier.fillMaxSize().background(Bg)) {

        when (current) {
            Tab.HOME -> {
                if (ownerMode) {
                    OwnerScreen(
                        uid = uid,
                        notices = ownerNotices,
                        onOpenNotices = { showNoticeCenter = true }
                    )
                } else {
                    HomeScreen()
                }
            }
            Tab.SERVICES -> ServicesScreen(
                uid = uid,
                onNewUserNotice = { /* الخادم يرسل إشعار؛ للتجربة المحلية */ },
                onNewOwnerNotice = { /* الخادم */ }
            )
            Tab.WALLET -> WalletScreen(uid = uid)
            Tab.ORDERS -> OrdersScreen(uid = uid)
            Tab.SUPPORT -> SupportScreen()
        }

        // شريط علوي يمين: جرس + حالة الخادم + إعدادات
        TopRightBar(
            online = serverOnline,
            unread = unreadCount,
            onOpenNotices = { showNoticeCenter = true },
            onOpenSettings = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 10.dp)
        )

        BottomNavBar(
            current = current,
            onChange = { current = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // الإعدادات (تفعيل وضع المالك)
    if (showSettings) {
        SettingsDialog(
            uid = uid,
            ownerMode = ownerMode,
            onOwnerLogin = { ownerMode = true; saveOwnerMode(ctx, true) },
            onOwnerLogout = { ownerMode = false; saveOwnerMode(ctx, false) },
            onDismiss = { showSettings = false }
        )
    }

    // مركز الإشعارات
    if (showNoticeCenter) {
        NoticeCenterDialog(
            notices = if (ownerMode) ownerNotices else userNotices,
            onDismiss = { showNoticeCenter = false }
        )
    }
}

/* ==========================================================
   UI: عامة
   ========================================================== */
@Composable
private fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center
    ) { Text("مرحبًا بك 👋", color = OnBg, fontSize = 20.sp) }
}

@Composable
private fun SupportScreen() {
    val uri = LocalUriHandler.current
    val whatsappUrl = "https://wa.me/9647763410970"
    val telegramUrl = "https://t.me/z396r"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الدعم", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ContactCard(
            title = "واتساب",
            subtitle = "+964 776 341 0970",
            actionText = "افتح واتساب",
            onClick = { uri.openUri(whatsappUrl) },
            icon = Icons.Filled.Call
        )
        Spacer(Modifier.height(8.dp))
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
        colors = CardDefaults.elevatedCardColors(containerColor = Surface1),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Dim, fontSize = 12.sp)
            }
            TextButton(onClick = onClick) { Text(actionText) }
        }
    }
}

@Composable
private fun TopRightBar(
    online: Boolean?,
    unread: Int,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.background(Surface1, MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // إشعارات (جرس صغير)
        BadgedBox(badge = {
            if (unread > 0) Badge { Text(unread.toString()) }
        }) {
            IconButton(onClick = onOpenNotices, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات", tint = OnBg)
            }
        }
        Spacer(Modifier.width(6.dp))
        val (txt, clr) = when (online) {
            true -> "الخادم: متصل" to Good
            false -> "الخادم: غير متصل" to Bad
            null -> "الخادم: ..." to Dim
        }
        Row(
            modifier = Modifier.background(Surface1, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(clr, MaterialTheme.shapes.small))
            Spacer(Modifier.width(6.dp))
            Text(txt, fontSize = 12.sp, color = OnBg)
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = OnBg)
        }
    }
}

@Composable
private fun NoticeCenterDialog(
    notices: List<AppNotice>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("الإشعارات") },
        text = {
            if (notices.isEmpty()) {
                Text("لا توجد إشعارات حالياً", color = Dim)
            } else {
                val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(notices.sortedByDescending { it.ts }) { n ->
                        Text("• ${n.title}", fontWeight = FontWeight.SemiBold)
                        Text(n.body, color = Dim, fontSize = 12.sp)
                        Text(fmt.format(Date(n.ts)), color = Dim, fontSize = 10.sp)
                        Divider(Modifier.padding(vertical = 8.dp), color = Surface1)
                    }
                }
            }
        }
    )
}

/* ==========================================================
   الإعدادات + وضع المالك
   ========================================================== */
@Composable
private fun SettingsDialog(
    uid: String,
    ownerMode: Boolean,
    onOwnerLogin: () -> Unit,
    onOwnerLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var askPass by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("الإعدادات") },
        text = {
            Column {
                Text("المعرّف (UID):", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uid, color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { clip.setText(AnnotatedString(uid)) }) { Text("نسخ") }
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = Surface1)
                Spacer(Modifier.height(12.dp))
                if (ownerMode) {
                    Text("وضع المالك: مفعل", color = Good, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onOwnerLogout) { Text("تسجيل خروج المالك") }
                } else {
                    Text("تسجيل المالك:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { askPass = true }) { Text("تسجيل المالك") }
                }
            }
        }
    )

    if (askPass) {
        AlertDialog(
            onDismissRequest = { askPass = false },
            confirmButton = {
                TextButton(onClick = {
                    if (pass == "2000") { onOwnerLogin(); askPass = false }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { askPass = false }) { Text("إلغاء") } },
            title = { Text("كلمة مرور المالك") },
            text = {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { s -> if (s.length <= 10) pass = s },
                    singleLine = true,
                    label = { Text("أدخل كلمة المرور: 2000") }
                )
            }
        )
    }
}

/* ==========================================================
   تبويب الخدمات
   ========================================================== */
@Composable
private fun ServicesScreen(
    uid: String,
    onNewUserNotice: (AppNotice) -> Unit,
    onNewOwnerNotice: (AppNotice) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var chosenService by remember { mutableStateOf<ServiceDef?>(null) }

    if (selectedCategory == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("الخدمات", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            serviceCategories.forEach { cat ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable { selectedCategory = cat },
                    colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(cat, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    val apiSections = setOf(
        "قسم المتابعين",
        "قسم الايكات",
        "قسم المشاهدات",
        "قسم مشاهدات البث المباشر",
        "قسم رفع سكور تيكتوك",
        "قسم خدمات التليجرام"
    )

    if (selectedCategory in apiSections) {
        val inCat = when (selectedCategory) {
            "قسم المتابعين"            -> servicesCatalog.filter { it.category == "المتابعين" }
            "قسم الايكات"              -> servicesCatalog.filter { it.category == "الايكات" }
            "قسم المشاهدات"            -> servicesCatalog.filter { it.category == "المشاهدات" }
            "قسم مشاهدات البث المباشر" -> servicesCatalog.filter { it.category == "مشاهدات البث المباشر" }
            "قسم رفع سكور تيكتوك"     -> servicesCatalog.filter { it.category == "رفع سكور تيكتوك" }
            "قسم خدمات التليجرام"      -> servicesCatalog.filter { it.category == "خدمات التليجرام" }
            else -> emptyList()
        }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
                }
                Spacer(Modifier.width(6.dp))
                Text(selectedCategory!!, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            inCat.forEach { svc ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable { chosenService = svc },
                    colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(svc.name, fontWeight = FontWeight.SemiBold)
                        Text("الكمية: ${svc.min} - ${svc.max}", color = Dim, fontSize = 12.sp)
                        Text("السعر لكل 1000: ${svc.pricePerK}\$", color = Dim, fontSize = 12.sp)
                        Text("كود الخدمة: ${svc.code}", color = Dim, fontSize = 12.sp)
                    }
                }
            }
        }
    } else {
        ManualSectionsScreen(
            uid = uid,
            title = selectedCategory!!,
            onBack = { selectedCategory = null }
        )
    }

    if (chosenService != null) {
        ServiceOrderDialog(
            uid = uid,
            service = chosenService!!,
            onDismiss = { chosenService = null },
            onOrderedOk = {
                // الخادم مسؤول عن الإشعارات. هنا فقط رسالة نجاح.
            }
        )
    }
}

/* طلب خدمة مربوطة بالـ API والخادم يخصم الرصيد إن كافٍ */
@Composable
private fun ServiceOrderDialog(
    uid: String,
    service: ServiceDef,
    onDismiss: () -> Unit,
    onOrderedOk: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var qtyText by remember { mutableStateOf(service.min.toString()) }
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var balance by remember { mutableStateOf<Double?>(null) }
    val qty = qtyText.toIntOrNull() ?: 0
    val price = ceil((qty / 1000.0) * service.pricePerK * 100) / 100.0

    LaunchedEffect(uid) {
        balance = apiGetBalance(uid)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    if (link.isBlank()) return@TextButton
                    if (qty < service.min || qty > service.max) return@TextButton
                    val bal = balance ?: 0.0
                    if (bal < price) return@TextButton
                    loading = true
                    scope.launch {
                        val ok = apiCreateOrder(
                            uid = uid,
                            serviceCode = service.code,
                            serviceName = service.name,
                            linkOrId = link,
                            quantity = qty,
                            price = price
                        )
                        loading = false
                        if (ok) {
                            onOrderedOk()
                            onDismiss()
                        }
                    }
                }
            ) { Text(if (loading) "يرسل..." else "شراء") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text(service.name) },
        text = {
            Column {
                Text("الكمية بين ${service.min} و ${service.max}", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { s -> if (s.all { it.isDigit() }) qtyText = s },
                    label = { Text("الكمية") }, singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("الرابط (أرسل الرابط وليس اليوزر)") }, singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                Text("السعر التقريبي: ${"%.2f".format(price)}\$", fontWeight = FontWeight.SemiBold)
                val balTxt = balance?.let { "%.2f".format(it) } ?: "..."
                Text("رصيدك: $balTxt \$", color = Dim, fontSize = 12.sp)
            }
        }
    )
}

/* أقسام تُنَفَّذ يدويًا من المالك ويُنشأ طلب داخل لوحات الأدمن */
@Composable
private fun ManualSectionsScreen(
    uid: String,
    title: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    val items = when (title) {
        "قسم شراء رصيد ايتونز" -> listOf("شراء رصيد ايتونز")
        "قسم شراء رصيد هاتف" -> listOf("شراء رصيد اثير", "شراء رصيد اسياسيل", "شراء رصيد كورك")
        "قسم شحن شدات ببجي" -> listOf("شحن شدات ببجي")
        "قسم خدمات الودو" -> listOf("شراء الماسات لودو", "شراء ذهب لودو")
        else -> emptyList()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
            }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        items.forEach { name ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .clickable {
                        // الخادم ينشئ طلب يدوي لهذا القسم
                        if (!loading) {
                            loading = true
                            scope.launch {
                                apiCreateManualRequest(uid, name)
                                loading = false
                            }
                        }
                    },
                colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(name, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ==========================================================
   تبويب رصيدي
   ========================================================== */
@Composable
private fun WalletScreen(uid: String) {
    val scope = rememberCoroutineScope()
    var balance by remember { mutableStateOf<Double?>(null) }
    var askAsiacell by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        balance = apiGetBalance(uid)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رصيدي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("الرصيد الحالي: ${balance?.let { "%.2f".format(it) } ?: "..."}$", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text("طرق الشحن:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        CardRow("شحن عبر أسيا سيل", Icons.Filled.SimCard) { askAsiacell = true }
        CardRow("شحن عبر هلا بي", Icons.Filled.AttachMoney) { openSupportHint() }
        CardRow("شحن عبر نقاط سنتات", Icons.Filled.AttachMoney) { openSupportHint() }
        CardRow("شحن عبر سوبركي", Icons.Filled.AttachMoney) { openSupportHint() }
        CardRow("شحن عبر زين كاش", Icons.Filled.AttachMoney) { openSupportHint() }
        CardRow("شحن عبر عملات رقمية (USDT)", Icons.Filled.AttachMoney) { openSupportHint() }
    }

    if (askAsiacell) {
        AlertDialog(
            onDismissRequest = { askAsiacell = false },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val digits = cardNumber.filter { it.isDigit() }
                        if (digits.length == 14 || digits.length == 16) {
                            busy = true
                            // إرسال للمالك — الخادم يضيفه لجدول "الكارتات المعلقة"
                            val n = cardNumber
                            val uid0 = uid
                            LaunchedEffect(n, uid0) {
                                val ok = apiWalletAsiacell(uid0, n)
                                busy = false
                                askAsiacell = false
                                if (ok) {
                                    // تحديث الرصيد من الخادم لاحقًا (عند قبول المالك)
                                }
                            }
                        }
                    }
                ) { Text(if (busy) "يرسل..." else "إرسال") }
            },
            dismissButton = { TextButton(onClick = { askAsiacell = false }) { Text("إلغاء") } },
            title = { Text("شحن عبر أسيا سيل") },
            text = {
                Column {
                    Text("أدخل رقم الكارت (14 أو 16 رقم):", color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { s -> if (s.all { it.isDigit() }) cardNumber = s },
                        singleLine = true,
                        label = { Text("رقم الكارت") }
                    )
                }
            }
        )
    }
}

@Composable
private fun CardRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Accent)
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun openSupportHint() {
    // مجرد تلميح — القنوات موجودة في "الدعم"
    // بالإمكان إضافة Toast إن رغبت (لكن Compose M3 لا يملك Toast افتراضيًا)
}

/* ==========================================================
   تبويب طلباتي
   ========================================================== */
@Composable
private fun OrdersScreen(uid: String) {
    var orders by remember { mutableStateOf<List<AppOrder>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        busy = true
        orders = apiFetchOrders(uid)
        busy = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (busy) {
            CircularProgressIndicator()
        } else if (orders.isEmpty()) {
            Text("لا توجد طلبات بعد.", color = Dim)
        } else {
            LazyColumn {
                items(orders.sortedByDescending { it.createdAt }) { o ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(o.serviceName, fontWeight = FontWeight.SemiBold)
                            Text("الكمية: ${o.quantity} | السعر: ${"%.2f".format(o.price)}\$", color = Dim, fontSize = 12.sp)
                            Text("الرابط/المعرّف: ${o.linkOrId}", color = Dim, fontSize = 12.sp)
                            Text("الحالة: ${o.status}", color = Dim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================================
   لوحة تحكم المالك (حقيقية، مربوطة بمسارات الأدمن)
   ========================================================== */
@Composable
private fun OwnerScreen(uid: String, notices: List<AppNotice>, onOpenNotices: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "الطلبات المعلقة (الخدمات)",
        "الكارتات المعلقة",
        "طلبات شحن الايتونز",
        "طلبات شدات ببجي",
        "طلبات لودو"
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // جرس إشعارات المالك أعلى يمين
            IconButton(onClick = onOpenNotices) {
                Icon(Icons.Filled.Notifications, contentDescription = "إشعارات المالك", tint = OnBg)
            }
        }
        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = tab, containerColor = Surface1) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = i == tab,
                    onClick = { tab = i },
                    text = { Text(t, fontSize = 12.sp) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        when (tab) {
            0 -> PendingListScreen(
                title = tabs[0],
                fetchPath = "/api/admin/pending?type=services",
                onExecute = { orderId -> apiAdminExecute(orderId) },
                onReject  = { orderId -> apiAdminReject(orderId) }
            )
            1 -> PendingListScreen(
                title = tabs[1],
                fetchPath = "/api/admin/pending?type=asiacell",
                onExecute = { itemId -> apiAdminAcceptCard(itemId) },
                onReject  = { itemId -> apiAdminRejectCard(itemId) }
            )
            2 -> PendingListScreen(
                title = tabs[2],
                fetchPath = "/api/admin/pending?type=itunes",
                onExecute = { itemId -> apiAdminExecuteItunes(itemId) },
                onReject  = { itemId -> apiAdminRejectItunes(itemId) }
            )
            3 -> PendingListScreen(
                title = tabs[3],
                fetchPath = "/api/admin/pending?type=pubg",
                onExecute = { itemId -> apiAdminExecutePubg(itemId) },
                onReject  = { itemId -> apiAdminRejectPubg(itemId) }
            )
            4 -> PendingListScreen(
                title = tabs[4],
                fetchPath = "/api/admin/pending?type=ludo",
                onExecute = { itemId -> apiAdminExecuteLudo(itemId) },
                onReject  = { itemId -> apiAdminRejectLudo(itemId) }
            )
        }
    }
}

/** واجهة عامة لقائمة مُعلّقات الأدمن (تجلب JSON عام وتعرض بطاقة بها زران تنفيذ/رفض) */
@Composable
private fun PendingListScreen(
    title: String,
    fetchPath: String,
    onExecute: suspend (String) -> Boolean,
    onReject: suspend (String) -> Boolean
) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var opBusyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fetchPath) {
        busy = true
        items = apiFetchAdminPending(fetchPath)
        busy = false
    }

    if (busy) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (items.isEmpty()) {
        Text("لا توجد عناصر الآن.", color = Dim)
        return
    }

    LazyColumn {
        items(items) { o ->
            val id = o.optString("id")
            val title0 = o.optString("title", o.optString("service_name", "طلب"))
            val details = o.toString(2)
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Surface1),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(title0, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(details, color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = {
                                if (opBusyId == null) {
                                    opBusyId = id
                                    LaunchedEffect(id) {
                                        val ok = onExecute(id)
                                        if (ok) { items = items.filterNot { it.optString("id") == id } }
                                        opBusyId = null
                                    }
                                }
                            },
                            enabled = opBusyId == null,
                            colors = ButtonDefaults.buttonColors(containerColor = Good)
                        ) { Text(if (opBusyId == id) "..." else "تنفيذ") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (opBusyId == null) {
                                    opBusyId = id
                                    LaunchedEffect(id) {
                                        val ok = onReject(id)
                                        if (ok) { items = items.filterNot { it.optString("id") == id } }
                                        opBusyId = null
                                    }
                                }
                            },
                            enabled = opBusyId == null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Bad)
                        ) { Text(if (opBusyId == id) "..." else "رفض") }
                    }
                }
            }
        }
    }
}

/* ==========================================================
   شريط سفلي
   ========================================================== */
@Composable
private fun BottomNavBar(
    current: Tab,
    onChange: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier.fillMaxWidth(), containerColor = Surface1) {
        NavItem(current == Tab.HOME,    { onChange(Tab.HOME) },    Icons.Filled.Home,               "الرئيسية")
        NavItem(current == Tab.SERVICES,{ onChange(Tab.SERVICES) },Icons.Filled.List,               "الخدمات")
        NavItem(current == Tab.WALLET,  { onChange(Tab.WALLET) },  Icons.Filled.AccountBalanceWallet,"رصيدي")
        NavItem(current == Tab.ORDERS,  { onChange(Tab.ORDERS) },  Icons.Filled.ShoppingCart,       "الطلبات")
        NavItem(current == Tab.SUPPORT, { onChange(Tab.SUPPORT) }, Icons.Filled.ChatBubble,         "الدعم")
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
        label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = Color.White,
            selectedTextColor   = Color.White,
            indicatorColor      = Accent.copy(alpha = 0.22f),
            unselectedIconColor = Dim,
            unselectedTextColor = Dim
        )
    )
}

/* ==========================================================
   تخزين إعدادات بسيطة محليًا (UID + وضع المالك)
   ========================================================== */
private fun prefs(ctx: Context) = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

private fun loadOrCreateUid(ctx: Context): String {
    val sp = prefs(ctx)
    val existing = sp.getString("uid", null)
    if (existing != null) return existing
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString("uid", fresh).apply()
    return fresh
}
private fun loadOwnerMode(ctx: Context): Boolean = prefs(ctx).getBoolean("owner_mode", false)
private fun saveOwnerMode(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("owner_mode", on).apply() }

/* ==========================================================
   شبكة — دوال REST (الخادم = مصدر الحقيقة)
   ملاحظة: تأكد من وجود هذه المسارات في الباك-إند:
   - GET  /health
   - POST /api/users/upsert            body: { uid }
   - GET  /api/wallet/balance?uid=...
   - POST /api/orders                  body: { uid, service_code, service_name, link_or_id, quantity, price }
   - GET  /api/orders?uid=...
   - POST /api/wallet/asiacell        body: { uid, card_number }
   - GET  /api/notices?uid=...        (للمستخدم)
   - GET  /api/notices/owner          (للمالك)
   - GET  /api/admin/pending?type=...
   - POST /api/admin/orders/{id}/execute /reject
   - POST /api/admin/cards/{id}/accept /reject
   - POST /api/admin/itunes/{id}/execute /reject
   - POST /api/admin/pubg/{id}/execute /reject
   - POST /api/admin/ludo/{id}/execute /reject
   ========================================================== */

private suspend fun apiPing(): Boolean? = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE/health")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 4000; readTimeout = 4000
        }
        con.connect()
        (con.responseCode in 200..299)
    } catch (_: Exception) { false }
}

private suspend fun apiUpsertUid(uid: String) = withContext(Dispatchers.IO) {
    try {
        httpPostJson("$API_BASE/api/users/upsert", JSONObject().put("uid", uid))
    } catch (_: Exception) { /* تجاهل */ }
}

private suspend fun apiGetBalance(uid: String): Double = withContext(Dispatchers.IO) {
    try {
        val txt = httpGet("$API_BASE/api/wallet/balance?uid=$uid")
        val obj = JSONObject(txt)
        obj.optDouble("balance", 0.0)
    } catch (_: Exception) { 0.0 }
}

private suspend fun apiCreateOrder(
    uid: String,
    serviceCode: Long,
    serviceName: String,
    linkOrId: String,
    quantity: Int,
    price: Double
): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject()
            .put("uid", uid)
            .put("service_code", serviceCode)
            .put("service_name", serviceName)
            .put("link_or_id", linkOrId)
            .put("quantity", quantity)
            .put("price", price)
        val txt = httpPostJson("$API_BASE/api/orders", body)
        txt.contains("ok", true)
    } catch (_: Exception) { false }
}

private suspend fun apiFetchOrders(uid: String): List<AppOrder> = withContext(Dispatchers.IO) {
    try {
        val txt = httpGet("$API_BASE/api/orders?uid=$uid")
        val arr = JSONArray(txt)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AppOrder(
                id = o.optString("id"),
                uid = o.optString("uid"),
                serviceCode = o.optLong("service_code"),
                serviceName = o.optString("service_name"),
                quantity = o.optInt("quantity"),
                price = o.optDouble("price"),
                linkOrId = o.optString("link_or_id"),
                status = o.optString("status"),
                createdAt = o.optLong("created_at")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private suspend fun apiWalletAsiacell(uid: String, cardNumber: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().put("uid", uid).put("card_number", cardNumber)
        val r = httpPostJson("$API_BASE/api/wallet/asiacell", body)
        r.contains("ok", true)
    } catch (_: Exception) { false }
}

private suspend fun apiFetchNotices(owner: Boolean, uid: String): List<AppNotice> = withContext(Dispatchers.IO) {
    try {
        val url = if (owner) "$API_BASE/api/notices/owner" else "$API_BASE/api/notices?uid=$uid"
        val txt = httpGet(url)
        val arr = JSONArray(txt)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AppNotice(
                id = o.optString("id", UUID.randomUUID().toString()),
                title = o.optString("title"),
                body = o.optString("body"),
                ts = o.optLong("ts", System.currentTimeMillis())
            )
        }
    } catch (_: Exception) { emptyList() }
}

private suspend fun apiFetchAdminPending(path: String): List<JSONObject> = withContext(Dispatchers.IO) {
    try {
        val txt = httpGet(API_BASE + path)
        val arr = JSONArray(txt)
        (0 until arr.length()).map { i -> arr.getJSONObject(i) }
    } catch (_: Exception) { emptyList() }
}

/* ======== عمليات الأدمن ======== */
private suspend fun apiAdminExecute(orderId: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/orders/$orderId/execute", JSONObject()); true } catch (_: Exception) { false }
}
private suspend fun apiAdminReject(orderId: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/orders/$orderId/reject", JSONObject()); true } catch (_: Exception) { false }
}

private suspend fun apiAdminAcceptCard(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/cards/$id/accept", JSONObject()); true } catch (_: Exception) { false }
}
private suspend fun apiAdminRejectCard(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/cards/$id/reject", JSONObject()); true } catch (_: Exception) { false }
}

private suspend fun apiAdminExecuteItunes(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/itunes/$id/execute", JSONObject()); true } catch (_: Exception) { false }
}
private suspend fun apiAdminRejectItunes(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/itunes/$id/reject", JSONObject()); true } catch (_: Exception) { false }
}

private suspend fun apiAdminExecutePubg(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/pubg/$id/execute", JSONObject()); true } catch (_: Exception) { false }
}
private suspend fun apiAdminRejectPubg(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/pubg/$id/reject", JSONObject()); true } catch (_: Exception) { false }
}

private suspend fun apiAdminExecuteLudo(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/ludo/$id/execute", JSONObject()); true } catch (_: Exception) { false }
}
private suspend fun apiAdminRejectLudo(id: String): Boolean = withContext(Dispatchers.IO) {
    try { httpPostJson("$API_BASE/api/admin/ludo/$id/reject", JSONObject()); true } catch (_: Exception) { false }
}

/* ==========================================================
   HTTP Helpers (بدون مكتبات خارجية)
   ========================================================== */
private fun httpGet(urlStr: String): String {
    val url = URL(urlStr)
    val con = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 12_000
        setRequestProperty("Accept", "application/json")
    }
    val code = con.responseCode
    val stream = if (code in 200..299) con.inputStream else con.errorStream
    return stream.bufferedReader().use(BufferedReader::readText)
}

private fun httpPostJson(urlStr: String, json: JSONObject): String {
    val url = URL(urlStr)
    val con = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 12_000
        readTimeout = 15_000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }
    OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(json.toString()) }
    val code = con.responseCode
    val stream = if (code in 200..299) con.inputStream else con.errorStream
    return stream.bufferedReader().use(BufferedReader::readText)
}
