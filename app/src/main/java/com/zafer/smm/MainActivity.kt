@file:Suppress("UnusedImport", "SpellCheckingInspection")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

/* =========================
   إعدادات الخادم
   ========================= */
private const val API_BASE =
    "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com" // عدّلها إذا تغيّر الباكند

/** اتصال مباشر مع مزوّد SMM (اختياري) */
private const val PROVIDER_DIRECT_URL = "https://kd1s.com/api/v2"
private const val PROVIDER_DIRECT_KEY_VALUE = "25a9ceb07be0d8b2ba88e70dcbe92e06"

/** مسارات الأدمن (مطابقة للباكند الموحد) */
private object AdminEndpoints {
    const val pendingServices = "/api/admin/pending/services"
    const val pendingItunes   = "/api/admin/pending/itunes"
    const val pendingPubg     = "/api/admin/pending/pubg"
    const val pendingLudo     = "/api/admin/pending/ludo"

    // ✅ الكروت المعلّقة لأسيا سيل
    const val pendingCards    = "/api/admin/pending/cards"
    fun topupCardReject(id: Int) = "/api/admin/topup_cards/$id/reject"
    fun topupCardExecute(id: Int) = "/api/admin/topup_cards/$id/execute"

    const val orderApprove    = "/api/admin/orders/%d/approve"
    const val orderDeliver    = "/api/admin/orders/%d/deliver"

    // قد تتوفر في باكندك، وإلا ستظهر "تعذر جلب البيانات"
    const val walletTopup     = "/api/admin/wallet/topup"
    const val walletDeduct    = "/api/admin/wallet/deduct"
    const val usersCount      = "/api/admin/users/count"
    const val usersBalances   = "/api/admin/users/balances"
    const val providerBalance = "/api/admin/provider/balance"
}

/* =========================
   Theme
   ========================= */
private val Bg       = Color(0xFF0F1113)
private val Surface1 = Color(0xFF161B20)
private val OnBg     = Color(0xFFF2F4F7)
private val Accent   = Color(0xFFB388FF)
private val Good     = Color(0xFF4CAF50)
private val Bad      = Color(0xFFE53935)
private val Dim      = Color(0xFFAAB3BB)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface1,
            primary = Accent,
            onBackground = OnBg,
            onSurface = OnBg
        ),
        content = content
    )
}

/* =========================
   نماذج/حالات
   ========================= */
enum class Tab { HOME, SERVICES, WALLET, ORDERS, SUPPORT }

data class AppNotice(
    val title: String,
    val body: String,
    val ts: Long = System.currentTimeMillis(),
    val forOwner: Boolean = false
)
data class ServiceDef(
    val uiKey: String,
    val serviceId: Long,
    val min: Int,
    val max: Int,
    val pricePerK: Double,
    val category: String
)
enum class OrderStatus { Pending, Processing, Done, Rejected, Refunded }
data class OrderItem(
    val id: String,
    val title: String,
    val quantity: Int,
    val price: Double,
    val payload: String,
    val status: OrderStatus,
    val createdAt: Long
)

/* ✅ نموذج خاص بكروت أسيا سيل (لواجهة المالك) */
data class PendingCard(
    val id: Int,
    val uid: String,
    val card: String,
    val createdAt: Long
)

private val servicesCatalog = listOf(
    ServiceDef("متابعين تيكتوك",   16256,   100, 1_000_000, 3.5, "المتابعين"),
    ServiceDef("متابعين انستغرام", 16267,   100, 1_000_000, 3.0, "المتابعين"),
    ServiceDef("لايكات تيكتوك",    12320,   100, 1_000_000, 1.0, "الايكات"),
    ServiceDef("لايكات انستغرام",  1066500, 100, 1_000_000, 1.0, "الايكات"),
    ServiceDef("مشاهدات تيكتوك",    9448,     100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef("مشاهدات انستغرام",  64686464, 100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef("مشاهدات بث تيكتوك", 14442, 100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef("مشاهدات بث انستا",   646464,100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef("رفع سكور البث",     14662, 100, 1_000_000, 2.0, "رفع سكور تيكتوك"),
    ServiceDef("اعضاء قنوات تلي",   955656, 100, 1_000_000, 3.0, "خدمات التليجرام"),
    ServiceDef("اعضاء كروبات تلي",  644656, 100, 1_000_000, 3.0, "خدمات التليجرام"),
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

/* =========================
   Activity
   ========================= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppRoot() } }
    }
}

/* =========================
   Root
   ========================= */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var ownerMode by remember { mutableStateOf(loadOwnerMode(ctx)) }
    var ownerToken by remember { mutableStateOf(loadOwnerToken(ctx)) }

    var online by remember { mutableStateOf<Boolean?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    var notices by remember { mutableStateOf(loadNotices(ctx)) }
    var showNoticeCenter by remember { mutableStateOf(false) }

    val unreadUser = notices.count { !it.forOwner }
    val unreadOwner = notices.count { it.forOwner }

    var currentTab by remember { mutableStateOf(Tab.HOME) }

    // فحص الصحة + تسجيل UID
    LaunchedEffect(Unit) {
        scope.launch { tryUpsertUid(uid) }
        while (true) {
            online = pingHealth()
            delay(15_000)
        }
    }

    // Auto hide toast بعد 2 ثواني
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2000)
            toast = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        when (currentTab) {
            Tab.HOME -> {
                if (ownerMode) {
                    OwnerPanel(
                        token = ownerToken,
                        onNeedLogin = { showSettings = true },
                        onToast = { toast = it }
                    )
                } else {
                    HomeScreen()
                }
            }
            Tab.SERVICES -> ServicesScreen(
                uid = uid,
                onAddNotice = {
                    notices = notices + it
                    saveNotices(ctx, notices)
                },
                onToast = { toast = it }
            )
            Tab.WALLET -> WalletScreen(
                uid = uid,
                onAddNotice = {
                    notices = notices + it
                    saveNotices(ctx, notices)
                },
                onToast = { toast = it }
            )
            Tab.ORDERS -> OrdersScreen(uid = uid)
            Tab.SUPPORT -> SupportScreen()
        }

        // ✅ حالة السيرفر ثم الجرس ثم الضبط — عموديًا في أعلى يمين
        TopRightBar(
            online = online,
            unread = if (ownerMode) unreadOwner else unreadUser,
            onOpenNotices = { showNoticeCenter = true },
            onOpenSettings = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 10.dp)
        )

        BottomNavBar(
            current = currentTab,
            onChange = { currentTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        toast?.let { msg ->
            Box(Modifier.fillMaxSize()) {
                Surface(
                    color = Surface1, tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp)
                ) {
                    Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = OnBg)
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            uid = uid,
            ownerMode = ownerMode,
            onOwnerLogin = { token ->
                ownerToken = token
                ownerMode = true
                saveOwnerMode(ctx, true)
                saveOwnerToken(ctx, token)
            },
            onOwnerLogout = {
                ownerToken = null
                ownerMode = false
                saveOwnerMode(ctx, false)
                saveOwnerToken(ctx, null)
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showNoticeCenter) {
        NoticeCenterDialog(
            notices = if (ownerMode) notices.filter { it.forOwner } else notices.filter { !it.forOwner },
            onClear = {
                notices = if (ownerMode) notices.filter { !it.forOwner } else notices.filter { it.forOwner }
                saveNotices(ctx, notices)
            },
            onDismiss = { showNoticeCenter = false }
        )
    }
}

/* =========================
   شاشات عامة
   ========================= */
@Composable private fun HomeScreen() {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Text("مرحبًا بك 👋", color = OnBg, fontSize = 18.sp)
    }
}
@Composable private fun SupportScreen() {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الدعم", color = OnBg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("للتواصل أو الاستفسار اختر إحدى الطرق التالية:", color = OnBg)
        Spacer(Modifier.height(12.dp))
        ContactCard(
            title = "واتساب", subtitle = "+964 776 341 0970",
            actionText = "افتح واتساب", onClick = { uri.openUri("https://wa.me/9647763410970") }, icon = Icons.Filled.Call
        )
        Spacer(Modifier.height(10.dp))
        ContactCard(
            title = "تيليجرام", subtitle = "@z396r",
            actionText = "افتح تيليجرام", onClick = { uri.openUri("https://t.me/z396r") }, icon = Icons.Filled.Send
        )
    }
}
@Composable private fun ContactCard(
    title: String, subtitle: String, actionText: String,
    onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = Surface1,
            contentColor = OnBg
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = OnBg)
                Text(subtitle, color = Dim, fontSize = 13.sp)
            }
            TextButton(onClick = onClick) { Text(actionText) }
        }
    }
}

/* =========================
   الشريط العلوي يمين — (عمودي)
   ========================= */
@Composable private fun TopRightBar(
    online: Boolean?, unread: Int,
    onOpenNotices: () -> Unit, onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Surface1, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // حالة السيرفر
        val (txt, clr) = when (online) {
            true -> "الخادم: متصل" to Good
            false -> "الخادم: غير متصل" to Bad
            else -> "الخادم: ..." to Dim
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Surface1, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(Modifier.size(8.dp).background(clr, shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(6.dp))
            Text(txt, fontSize = 12.sp, color = OnBg)
        }

        // الجرس
        BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
            IconButton(onClick = onOpenNotices, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = OnBg)
            }
        }

        // الضبط
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = OnBg)
        }
    }
}

/* مركز الإشعارات */
@Composable private fun NoticeCenterDialog(
    notices: List<AppNotice>, onClear: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        dismissButton = { TextButton(onClick = onClear) { Text("مسح الإشعارات") } },
        title = { Text("الإشعارات") },
        text = {
            if (notices.isEmpty()) {
                Text("لا توجد إشعارات حاليًا", color = Dim)
            } else {
                LazyColumn {
                    items(notices.sortedByDescending { it.ts }) { itx ->
                        val dt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(itx.ts))
                        Text("• ${itx.title}", fontWeight = FontWeight.SemiBold, color = OnBg)
                        Text(itx.body, color = Dim, fontSize = 12.sp)
                        Text(dt, color = Dim, fontSize = 10.sp)
                        Divider(Modifier.padding(vertical = 8.dp), color = Surface1)
                    }
                }
            }
        }
    )
}

/* =========================
   تبويب الخدمات + الطلب اليدوي
   ========================= */
@Composable private fun ServicesScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedService by remember { mutableStateOf<ServiceDef?>(null) }

    if (selectedCategory == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("الخدمات", color = OnBg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            serviceCategories.forEach { cat ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { selectedCategory = cat },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Surface1,
                        contentColor = OnBg
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ChevronLeft, null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(cat, fontWeight = FontWeight.SemiBold, color = OnBg)
                    }
                }
            }
        }
        return
    }

    val inCat = when (selectedCategory) {
        "قسم المتابعين"            -> servicesCatalog.filter { it.category == "المتابعين" }
        "قسم الايكات"              -> servicesCatalog.filter { it.category == "الايكات" }
        "قسم المشاهدات"            -> servicesCatalog.filter { it.category == "المشاهدات" }
        "قسم مشاهدات البث المباشر" -> servicesCatalog.filter { it.category == "مشاهدات البث المباشر" }
        "قسم رفع سكور تيكتوك"     -> servicesCatalog.filter { it.category == "رفع سكور تيكتوك" }
        "قسم خدمات التليجرام"      -> servicesCatalog.filter { it.category == "خدمات التليجرام" }
        else -> emptyList()
    }

    if (inCat.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg)
                }
                Spacer(Modifier.width(6.dp))
                Text(selectedCategory!!, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
            }
            Spacer(Modifier.height(10.dp))

            inCat.forEach { svc ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { selectedService = svc },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Surface1,
                        contentColor = OnBg
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(svc.uiKey, fontWeight = FontWeight.SemiBold, color = OnBg)
                        Text("الكمية: ${svc.min} - ${svc.max}", color = Dim, fontSize = 12.sp)
                        Text("السعر لكل 1000: ${svc.pricePerK}\$", color = Dim, fontSize = 12.sp)
                    }
                }
            }
        }
    } else {
        ManualSectionsScreen(
            title = selectedCategory!!,
            uid = uid,
            onBack = { selectedCategory = null },
            onToast = onToast,
            onAddNotice = onAddNotice
        )
    }

    selectedService?.let { svc ->
        ServiceOrderDialog(
            uid = uid, service = svc,
            onDismiss = { selectedService = null },
            onOrdered = { ok, msg ->
                onToast(msg)
                if (ok) {
                    onAddNotice(AppNotice("طلب جديد (${svc.uiKey})", "تم استلام طلبك وسيتم تنفيذه قريبًا.", forOwner = false))
                    onAddNotice(AppNotice("طلب خدمات معلّق", "طلب ${svc.uiKey} من UID=$uid بانتظار المعالجة/التنفيذ", forOwner = true))
                }
            }
        )
    }
}

@Composable private fun ServiceOrderDialog(
    uid: String, service: ServiceDef,
    onDismiss: () -> Unit,
    onOrdered: (Boolean, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf(service.min.toString()) }
    val qty = qtyText.toIntOrNull() ?: 0
    val price = ceil((qty / 1000.0) * service.pricePerK * 100) / 100.0

    var loading by remember { mutableStateOf(false) }
    var userBalance by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) { userBalance = apiGetBalance(uid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = !loading, onClick = {
                if (link.isBlank()) { onOrdered(false, "الرجاء إدخال الرابط"); return@TextButton }
                if (qty < service.min || qty > service.max) { onOrdered(false, "الكمية يجب أن تكون بين ${service.min} و ${service.max}"); return@TextButton }
                val bal = userBalance ?: 0.0
                if (bal < price) { onOrdered(false, "رصيدك غير كافٍ. السعر: $price\$ | رصيدك: ${"%.2f".format(bal)}\$"); return@TextButton }

                loading = true
                scope.launch {
                    val ok = apiCreateProviderOrder(
                        uid = uid,
                        serviceId = service.serviceId,
                        serviceName = service.uiKey,
                        link = link,
                        quantity = qty,
                        price = price
                    )
                    loading = false
                    if (ok) onOrdered(true, "تم إرسال الطلب بنجاح.")
                    else onOrdered(false, "فشل إرسال الطلب.")
                    onDismiss()
                }
            }) { Text(if (loading) "يرسل..." else "شراء") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text(service.uiKey) },
        text = {
            Column {
                Text("الكمية بين ${service.min} و ${service.max}", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { s -> if (s.all { it.isDigit() }) qtyText = s },
                    label = { Text("الكمية") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Accent,
                        focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                        focusedLabelColor = OnBg, unfocusedLabelColor = Dim
                    )
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = link, onValueChange = { link = it },
                    label = { Text("الرابط (أرسل الرابط وليس اليوزر)") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Accent,
                        focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                        focusedLabelColor = OnBg, unfocusedLabelColor = Dim
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text("السعر التقريبي: $price\$", fontWeight = FontWeight.SemiBold, color = OnBg)
                Spacer(Modifier.height(4.dp))
                Text("رصيدك الحالي: ${userBalance?.let { "%.2f".format(it) } ?: "..."}\$", color = Dim, fontSize = 12.sp)
            }
        }
    )
}

/* الأقسام اليدوية (ايتونز/هاتف/ببجي/لودو) */
@Composable private fun ManualSectionsScreen(
    title: String,
    uid: String,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    onAddNotice: (AppNotice) -> Unit
) {
    val scope = rememberCoroutineScope()
    val items = when (title) {
        "قسم شراء رصيد ايتونز" -> listOf("شراء رصيد ايتونز")
        "قسم شراء رصيد هاتف"  -> listOf("شراء رصيد اثير", "شراء رصيد اسياسيل", "شراء رصيد كورك")
        "قسم شحن شدات ببجي"    -> listOf("شحن شدات ببجي")
        "قسم خدمات الودو"       -> listOf("شراء الماسات لودو", "شراء ذهب لودو")
        else -> emptyList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(10.dp))

        items.forEach { name ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable {
                        scope.launch {
                            val ok = apiCreateManualOrder(uid, name)
                            if (ok) {
                                onToast("تم استلام طلبك ($name)، سيتم مراجعته من المالك.")
                                onAddNotice(AppNotice("طلب معلّق", "تم إرسال طلب $name للمراجعة.", forOwner = false))
                                onAddNotice(AppNotice("طلب يدوي جديد", "طلب $name من UID=$uid يحتاج مراجعة.", forOwner = true))
                            } else {
                                onToast("تعذر إرسال الطلب. حاول لاحقًا.")
                            }
                        }
                    },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Surface1,
                    contentColor = OnBg
                )
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ChevronLeft, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(name, fontWeight = FontWeight.SemiBold, color = OnBg)
                }
            }
        }
    }
}

/* =========================
   تبويب رصيدي (أسيا سيل)
   ========================= */
@Composable private fun WalletScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var balance by remember { mutableStateOf<Double?>(null) }
    var askAsiacell by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { balance = apiGetBalance(uid) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رصيدي", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg)
        Spacer(Modifier.height(8.dp))
        Text(
            "الرصيد الحالي: ${balance?.let { "%.2f".format(it) } ?: "..."}$",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnBg
        )
        Spacer(Modifier.height(16.dp))
        Text("طرق الشحن:", fontWeight = FontWeight.SemiBold, color = OnBg)
        Spacer(Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { askAsiacell = true },
            colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SimCard, null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("شحن عبر أسيا سيل (كارت)", fontWeight = FontWeight.SemiBold, color = OnBg)
            }
        }

        listOf(
            "شحن عبر هلا بي",
            "شحن عبر نقاط سنتات",
            "شحن عبر سوبركي",
            "شحن عبر زين كاش",
            "شحن عبر عملات رقمية (USDT)"
        ).forEach {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    onToast("لإتمام الشحن تواصل مع الدعم (واتساب/تيليجرام).")
                    onAddNotice(AppNotice("شحن رصيد", "يرجى التواصل مع الدعم لإكمال شحن: $it", forOwner = false))
                },
                colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AttachMoney, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontWeight = FontWeight.SemiBold, color = OnBg)
                }
            }
        }
    }

    if (askAsiacell) {
        AlertDialog(
            onDismissRequest = { if (!sending) askAsiacell = false },
            confirmButton = {
                val scope2 = rememberCoroutineScope()
                TextButton(enabled = !sending, onClick = {
                    val digits = cardNumber.filter { it.isDigit() }
                    if (digits.length != 14 && digits.length != 16) return@TextButton
                    sending = true
                    scope2.launch {
                        val ok = apiSubmitAsiacellCard(uid, digits)
                        if (ok) { balance = apiGetBalance(uid) }
                        sending = false
                        if (ok) {
                            onAddNotice(AppNotice("تم استلام كارتك", "تم إرسال كارت أسيا سيل إلى المالك للمراجعة.", forOwner = false))
                            onAddNotice(AppNotice("كارت أسيا سيل جديد", "UID=$uid | كارت: $digits", forOwner = true))
                            onToast("تم إرسال الكارت بنجاح")
                            cardNumber = ""
                            askAsiacell = false
                        } else {
                            onAddNotice(AppNotice("فشل إرسال الكارت", "تحقق من الاتصال وحاول مجددًا.", forOwner = false))
                            onToast("فشل إرسال الكارت")
                        }
                    }
                }) { Text(if (sending) "يرسل..." else "إرسال") }
            },
            dismissButton = { TextButton(enabled = !sending, onClick = { askAsiacell = false }) { Text("إلغاء") } },
            title = { Text("شحن عبر أسيا سيل", color = OnBg) },
            text = {
                Column {
                    Text("أدخل رقم الكارت (14 أو 16 رقم):", color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { s -> if (s.all { it.isDigit() }) cardNumber = s },
                        singleLine = true,
                        label = { Text("رقم الكارت") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Accent,
                            focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                            focusedLabelColor = OnBg, unfocusedLabelColor = Dim
                        )
                    )
                }
            }
        )
    }
}

/* =========================
   تبويب طلباتي
   ========================= */
@Composable private fun OrdersScreen(uid: String) {
    var orders by remember { mutableStateOf<List<OrderItem>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        loading = true
        err = null
        orders = apiGetMyOrders(uid).also { loading = false }
        if (orders == null) err = "تعذر جلب الطلبات"
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتي", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg)
        Spacer(Modifier.height(10.dp))

        when {
            loading -> Text("يتم التحميل...", color = Dim)
            err != null -> Text(err!!, color = Bad)
            orders.isNullOrEmpty() -> Text("لا توجد طلبات حتى الآن.", color = Dim)
            else -> LazyColumn {
                items(orders!!) { o ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(o.title, fontWeight = FontWeight.SemiBold, color = OnBg)
                            Text("الكمية: ${o.quantity} | السعر: ${"%.2f".format(o.price)}$", color = Dim, fontSize = 12.sp)
                            Text("المعرف: ${o.id}", color = Dim, fontSize = 12.sp)
                            Text("الحالة: ${o.status}", color = when (o.status) {
                                OrderStatus.Done -> Good
                                OrderStatus.Rejected -> Bad
                                OrderStatus.Refunded -> Accent
                                else -> OnBg
                            }, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/* =========================
   فلاتر لتصنيف الطلبات
   ========================= */
private fun isPhoneTopupTitle(title: String): Boolean {
    val t = title.lowercase()
    return t.contains("شراء رصيد") || t.contains("رصيد هاتف")
            || t.contains("اثير") || t.contains("اسياسيل") || t.contains("كورك")
}
private fun isApiOrder(o: OrderItem): Boolean {
    val tl = o.title.lowercase()
    val notManualPhone = !isPhoneTopupTitle(o.title)
    val notItunes = !tl.contains("ايتونز")
    val notPubg = !tl.contains("ببجي")
    val notLudo = !tl.contains("لودو")
    // نعتبر طلب API إذا له كمية > 0 أو عنده تفاصيل (رابط/بايلود)،
    // وبشرط ألا يكون من الأقسام اليدوية المذكورة:
    return (o.quantity > 0 || o.payload.isNotBlank()) && notManualPhone && notItunes && notPubg && notLudo
}

/* =========================
   لوحة تحكم المالك
   ========================= */
@Composable private fun OwnerPanel(
    token: String?,
    onNeedLogin: () -> Unit,
    onToast: (String) -> Unit
) {
    var current by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg, modifier = Modifier.weight(1f))
            IconButton(onClick = { current = "notices" }) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = OnBg)
            }
        }
        Spacer(Modifier.height(12.dp))

        fun needToken(): Boolean {
            if (token.isNullOrBlank()) {
                onToast("سجل دخول المالك أولًا من الإعدادات.")
                onNeedLogin()
                return true
            }
            return false
        }

        if (current == null) {
            val buttons = listOf(
                "الطلبات المعلقة (الخدمات)" to "pending_services",
                "طلبات الايتونز المعلقة"   to "pending_itunes",
                "طلبات شدات ببجي"          to "pending_pubg",
                "طلبات لودو المعلقة"       to "pending_ludo",
                "طلبات الأرصدة المعلقة"    to "pending_phone",   // ✅ جديد
                "الكروت المعلقة"           to "pending_cards",
                "إضافة الرصيد"             to "topup",
                "خصم الرصيد"               to "deduct",
                "عدد المستخدمين"           to "users_count",
                "أرصدة المستخدمين"         to "users_balances",
                "فحص رصيد API"             to "provider_balance"
            )
            buttons.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (title, key) ->
                        ElevatedButton(
                            onClick = { if (!needToken()) current = key },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Accent.copy(alpha = 0.18f),
                                contentColor = OnBg
                            )
                        ) { Text(title, fontSize = 12.sp) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            when (current) {
                "pending_services" -> AdminPendingGenericList(
                    title = "الطلبات المعلقة (الخدمات)",
                    token = token!!,
                    fetchUrl = AdminEndpoints.pendingServices,
                    itemFilter = { item -> isApiOrder(item) },   // ✅ فقط طلبات API
                    onBack = { current = null }
                )
                "pending_itunes" -> AdminPendingGenericList(
                    title = "طلبات الايتونز المعلقة",
                    token = token!!,
                    fetchUrl = AdminEndpoints.pendingItunes,
                    itemFilter = null,
                    onBack = { current = null }
                )
                "pending_pubg" -> AdminPendingGenericList(
                    title = "طلبات شدات ببجي",
                    token = token!!,
                    fetchUrl = AdminEndpoints.pendingPubg,
                    itemFilter = null,
                    onBack = { current = null }
                )
                "pending_ludo" -> AdminPendingGenericList(
                    title = "طلبات لودو المعلقة",
                    token = token!!,
                    fetchUrl = AdminEndpoints.pendingLudo,
                    itemFilter = null,
                    onBack = { current = null }
                )
                "pending_phone" -> AdminPendingGenericList(
                    title = "طلبات الأرصدة المعلقة",
                    token = token!!,
                    // استعمل مسار يعيد كل الطلبات اليدوية إن توفر، وإلا نستخدم خدمات مع فلترة العنوان:
                    fetchUrl = AdminEndpoints.pendingServices,
                    itemFilter = { item -> isPhoneTopupTitle(item.title) && item.quantity == 0 },
                    onBack = { current = null }
                )
                // ✅ شاشة الكروت المعلّقة الخاصة — UID + كارت + تنفيذ/رفض
                "pending_cards" -> AdminPendingCardsScreen(
                    token = token!!,
                    onBack = { current = null }
                )
                // إجراءات رصيد
                "topup" -> TopupDeductScreen(
                    title = "إضافة الرصيد",
                    token = token!!,
                    endpoint = AdminEndpoints.walletTopup,
                    onBack = { current = null }
                )
                "deduct" -> TopupDeductScreen(
                    title = "خصم الرصيد",
                    token = token!!,
                    endpoint = AdminEndpoints.walletDeduct,
                    onBack = { current = null }
                )
                "users_count" -> UsersCountScreen(
                    token = token!!,
                    onBack = { current = null }
                )
                "users_balances" -> UsersBalancesScreen(
                    token = token!!,
                    onBack = { current = null }
                )
                "provider_balance" -> ProviderBalanceScreen(
                    token = token!!,
                    onBack = { current = null }
                )
            }
        }
    }
}

/** قائمة عامة للمعلّقات مع مُرشِّح OrderItem */
@Composable private fun AdminPendingGenericList(
    title: String,
    token: String,
    fetchUrl: String,
    itemFilter: ((OrderItem) -> Boolean)?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<OrderItem>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var snack by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        loading = true; err = null
        val (code, txt) = httpGet(fetchUrl, headers = mapOf("x-admin-password" to token))
        if (code in 200..299 && txt != null) {
            try {
                val parsed = mutableListOf<OrderItem>()
                val trimmed = txt.trim()
                val arr: JSONArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    when {
                        obj.has("list") -> obj.optJSONArray("list") ?: JSONArray()
                        obj.has("data") -> obj.optJSONArray("data") ?: JSONArray()
                        else -> JSONArray()
                    }
                }
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val item = OrderItem(
                        id = o.optString("id", o.optInt("id", 0).toString()),
                        title = o.optString("title",""),
                        quantity = o.optInt("quantity", 0),
                        price = o.optDouble("price", 0.0),
                        payload = o.optString("link",""),
                        status = OrderStatus.Pending,
                        createdAt = o.optLong("created_at", 0L)
                    )
                    if (itemFilter == null || itemFilter.invoke(item)) {
                        parsed += item
                    }
                }
                list = parsed
            } catch (_: Exception) {
                list = null
                err = "تعذر جلب البيانات"
            }
        } else {
            list = null
            err = "تعذر جلب البيانات"
        }
        loading = false
    }

    suspend fun doApprove(id: String): Boolean =
        apiAdminPOST(String.format(AdminEndpoints.orderApprove, id.toInt()), token)

    suspend fun doDeliver(id: String): Boolean =
        apiAdminPOST(String.format(AdminEndpoints.orderDeliver, id.toInt()), token)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(10.dp))

        when {
            loading -> Text("يتم التحميل...", color = Dim)
            err != null -> Text(err!!, color = Bad)
            list.isNullOrEmpty() -> Text("لا يوجد شيء معلق.", color = Dim)
            else -> LazyColumn {
                items(list!!) { o ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(o.title, fontWeight = FontWeight.SemiBold, color = OnBg)
                            if (o.payload.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("تفاصيل: ${o.payload}", color = Dim, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = {
                                    scope.launch {
                                        val ok = doApprove(o.id)
                                        snack = if (ok) "تم التنفيذ" else "فشل التنفيذ"
                                        if (ok) reloadKey++
                                    }
                                }) { Text("تنفيذ") }
                                TextButton(onClick = {
                                    scope.launch {
                                        val ok = doDeliver(o.id)
                                        snack = if (ok) "تم الرفض" else "فشل التنفيذ"
                                        if (ok) reloadKey++
                                    }
                                }) { Text("رفض") }
                            }
                        }
                    }
                }
            }
        }

        snack?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = OnBg)
            LaunchedEffect(it) { delay(2000); snack = null }
        }
    }
}

/* =========================
   شاشة الكروت المعلّقة (المالك)
   ========================= */
@Composable private fun AdminPendingCardsScreen(
    token: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<PendingCard>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    var execFor by remember { mutableStateOf<PendingCard?>(null) }
    var amountText by remember { mutableStateOf("") }
    var snack by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        loading = true; err = null
        list = apiAdminFetchPendingCards(token)
        if (list == null) err = "تعذر جلب البيانات"
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("الكروت المعلقة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(10.dp))

        when {
            loading -> Text("يتم التحميل...", color = Dim)
            err != null -> Text(err!!, color = Bad)
            list.isNullOrEmpty() -> Text("لا توجد كروت معلّقة.", color = Dim)
            else -> LazyColumn {
                items(list!!) { c ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("طلب #${c.id}", fontWeight = FontWeight.SemiBold, color = OnBg)
                            Spacer(Modifier.height(4.dp))
                            Text("UID: ${c.uid}", color = Dim, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            val clip = LocalClipboardManager.current
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("الكارت: ", color = OnBg)
                                Text(
                                    c.card,
                                    color = Accent,
                                    modifier = Modifier
                                        .clickable {
                                            clip.setText(AnnotatedString(c.card))
                                            snack = "تم نسخ رقم الكارت"
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = { execFor = c }) { Text("تنفيذ") }
                                TextButton(onClick = {
                                    scope.launch {
                                        val ok = apiAdminRejectTopupCard(c.id, token)
                                        snack = if (ok) "تم الرفض" else "فشل الرفض"
                                        if (ok) reloadKey++
                                    }
                                }) { Text("رفض") }
                            }
                        }
                    }
                }
            }
        }

        snack?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = OnBg)
            LaunchedEffect(it) { delay(2000); snack = null }
        }
    }

    if (execFor != null) {
        AlertDialog(
            onDismissRequest = { execFor = null },
            confirmButton = {
                val scope2 = rememberCoroutineScope()
                TextButton(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) return@TextButton
                    scope2.launch {
                        val ok = apiAdminExecuteTopupCard(execFor!!.id, amt, token)
                        if (ok) {
                            execFor = null
                            amountText = ""
                            // بعد التنفيذ سيتم تحديث القائمة عبر reloadKey في الأعلى عند الحاجة
                        }
                    }
                }) { Text("إرسال") }
            },
            dismissButton = { TextButton(onClick = { execFor = null }) { Text("إلغاء") } },
            title = { Text("تنفيذ الشحن", color = OnBg) },
            text = {
                Column {
                    Text("أدخل مبلغ الشحن ليُضاف لرصيد المستخدم", color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { s -> if (s.isEmpty() || s.toDoubleOrNull() != null) amountText = s },
                        singleLine = true,
                        label = { Text("المبلغ") }
                    )
                }
            }
        )
    }
}

/* =========================
   شاشات مضافة لإكمال النواقص
   ========================= */

/** إضافة/خصم رصيد — تنفذ الطلب بنفسها داخل Coroutine */
@Composable private fun TopupDeductScreen(
    title: String,
    token: String,
    endpoint: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var uid by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uid, onValueChange = { uid = it.trim() },
            singleLine = true, label = { Text("UID المستخدم") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount, onValueChange = { s -> if (s.isEmpty() || s.toDoubleOrNull() != null) amount = s },
            singleLine = true, label = { Text("المبلغ") }
        )
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !busy,
            onClick = {
                val a = amount.toDoubleOrNull()
                if (uid.isBlank() || a == null || a <= 0.0) { msg = "أدخل UID ومبلغًا صحيحًا"; return@Button }
                busy = true
                scope.launch {
                    val ok = apiAdminWalletChange(endpoint, token, uid, a)
                    busy = false
                    msg = if (ok) "تمت العملية بنجاح" else "فشلت العملية"
                }
            }
        ) { Text(if (busy) "جارٍ التنفيذ..." else "تنفيذ") }

        Spacer(Modifier.height(10.dp))
        msg?.let { Text(it, color = OnBg) }
    }
}

/** عدد المستخدمين */
@Composable private fun UsersCountScreen(
    token: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var count by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        count = apiAdminUsersCount(token)
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("عدد المستخدمين", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(12.dp))
        if (loading) Text("يتم التحميل...", color = Dim)
        else Text("العدد: ${count ?: 0}", color = OnBg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            loading = true
            scope.launch { count = apiAdminUsersCount(token); loading = false }
        }) { Text("تحديث") }
    }
}

/** أرصدة المستخدمين */
@Composable private fun UsersBalancesScreen(
    token: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Triple<String,String,Double>>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        rows = apiAdminUsersBalances(token)
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("أرصدة المستخدمين", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Text("يتم التحميل...", color = Dim)
            rows == null -> Text("تعذر جلب البيانات", color = Bad)
            rows!!.isEmpty() -> Text("لا توجد بيانات.", color = Dim)
            else -> LazyColumn {
                items(rows!!) { (u, state, bal) ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1, contentColor = OnBg)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("UID: $u", fontWeight = FontWeight.SemiBold, color = OnBg)
                                Text("الحالة: $state", color = Dim, fontSize = 12.sp)
                            }
                            Text("${"%.2f".format(bal)}$", color = OnBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            loading = true
            scope.launch { rows = apiAdminUsersBalances(token); loading = false }
        }) { Text("تحديث") }
    }
}

/** فحص رصيد المزود */
@Composable private fun ProviderBalanceScreen(
    token: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var bal by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true; err = null
        bal = apiAdminProviderBalance(token)
        if (bal == null) err = "تعذر جلب الرصيد"
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("فحص رصيد API", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Text("يتم التحميل...", color = Dim)
            err != null -> Text(err!!, color = Bad)
            else -> Text("الرصيد: ${"%.2f".format(bal ?: 0.0)}", color = OnBg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            loading = true; err = null
            scope.launch {
                bal = apiAdminProviderBalance(token)
                if (bal == null) err = "تعذر جلب الرصيد"
                loading = false
            }
        }) { Text("تحديث") }
    }
}

/* =========================
   شريط سفلي
   ========================= */
@Composable private fun BottomNavBar(current: Tab, onChange: (Tab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier.fillMaxWidth(), containerColor = Surface1) {
        NavItem(current == Tab.HOME, { onChange(Tab.HOME) }, Icons.Filled.Home, "الرئيسية")
        NavItem(current == Tab.SERVICES, { onChange(Tab.SERVICES) }, Icons.Filled.List, "الخدمات")
        NavItem(current == Tab.WALLET, { onChange(Tab.WALLET) }, Icons.Filled.AccountBalanceWallet, "رصيدي")
        NavItem(current == Tab.ORDERS, { onChange(Tab.ORDERS) }, Icons.Filled.ShoppingCart, "الطلبات")
        NavItem(current == Tab.SUPPORT, { onChange(Tab.SUPPORT) }, Icons.Filled.ChatBubble, "الدعم")
    }
}
@Composable private fun RowScope.NavItem(
    selected: Boolean, onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String
) {
    NavigationBarItem(
        selected = selected, onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White, selectedTextColor = Color.White,
            indicatorColor = Accent.copy(alpha = 0.25f),
            unselectedIconColor = Dim, unselectedTextColor = Dim
        )
    )
}

/* =========================
   تخزين محلي + أدوات شبكة
   ========================= */
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
private fun loadOwnerToken(ctx: Context): String? = prefs(ctx).getString("owner_token", null)
private fun saveOwnerToken(ctx: Context, token: String?) { prefs(ctx).edit().putString("owner_token", token).apply() }

private fun loadNotices(ctx: Context): List<AppNotice> {
    val raw = prefs(ctx).getString("notices_json", "[]") ?: "[]"
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AppNotice(
                title = o.optString("title"),
                body = o.optString("body"),
                ts = o.optLong("ts"),
                forOwner = o.optBoolean("forOwner")
            )
        }
    } catch (_: Exception) { emptyList() }
}
private fun saveNotices(ctx: Context, notices: List<AppNotice>) {
    val arr = JSONArray()
    notices.forEach {
        val o = JSONObject()
        o.put("title", it.title)
        o.put("body", it.body)
        o.put("ts", it.ts)
        o.put("forOwner", it.forOwner)
        arr.put(o)
    }
    prefs(ctx).edit().putString("notices_json", arr.toString()).apply()
}

/* شبكة - GET (suspend) */
private suspend fun httpGet(path: String, headers: Map<String, String> = emptyMap()): Pair<Int, String?> =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE$path")
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = con.responseCode
            val txt = (if (code in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader()?.use { it.readText() }
            code to txt
        } catch (_: Exception) { -1 to null }
    }

/* POST JSON (blocking) — نغلفها بدالة suspend أدناه */
private fun httpPostBlocking(path: String, json: JSONObject, headers: Map<String, String> = emptyMap()): Pair<Int, String?> {
    return try {
        val url = URL("$API_BASE$path")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(json.toString()) }
        val code = con.responseCode
        val txt = (if (code in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.use { it.readText() }
        code to txt
    } catch (_: Exception) { -1 to null }
}

/* POST form مطلق (KD1S) — نغلفها بدالة suspend */
private fun httpPostFormAbsolute(fullUrl: String, fields: Map<String, String>, headers: Map<String, String> = emptyMap()): Pair<Int, String?> {
    return try {
        val url = URL(fullUrl)
        val form = fields.entries.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        con.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
        val code = con.responseCode
        val txt = (if (code in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.use { it.readText() }
        code to txt
    } catch (_: Exception) { -1 to null }
}

/* أغلفة suspend للـ POSTs */
private suspend fun httpPost(path: String, json: JSONObject, headers: Map<String, String> = emptyMap()): Pair<Int, String?> =
    withContext(Dispatchers.IO) { httpPostBlocking(path, json, headers) }

private suspend fun httpPostFormAbs(fullUrl: String, fields: Map<String, String>, headers: Map<String, String> = emptyMap()): Pair<Int, String?> =
    withContext(Dispatchers.IO) { httpPostFormAbsolute(fullUrl, fields, headers) }

/* ===== وظائف مشتركة مع الخادم ===== */
private suspend fun pingHealth(): Boolean? {
    val (code, _) = httpGet("/health")
    return code in 200..299
}
private suspend fun tryUpsertUid(uid: String) {
    httpPost("/api/users/upsert", JSONObject().put("uid", uid))
}
private suspend fun apiGetBalance(uid: String): Double? {
    val (code, txt) = httpGet("/api/wallet/balance?uid=$uid")
    return if (code in 200..299 && txt != null) {
        try { JSONObject(txt.trim()).optDouble("balance") } catch (_: Exception) { null }
    } else null
}
private suspend fun apiCreateProviderOrder(
    uid: String, serviceId: Long, serviceName: String, link: String, quantity: Int, price: Double
): Boolean {
    val body = JSONObject()
        .put("uid", uid)
        .put("service_id", serviceId)
        .put("service_name", serviceName)
        .put("link", link)
        .put("quantity", quantity)
        .put("price", price)
    val (code, txt) = httpPost("/api/orders/create/provider", body)
    return code in 200..299 && (txt?.contains("ok", ignoreCase = true) == true)
}

/* أسيا سيل */
private suspend fun apiSubmitAsiacellCard(uid: String, card: String): Boolean {
    val (code, txt) = httpPost(
        "/api/wallet/asiacell/submit",
        JSONObject().put("uid", uid).put("card", card)
    )
    if (code !in 200..299) return false
    return try {
        if (txt == null) return true
        val obj = JSONObject(txt.trim())
        obj.optBoolean("ok", true) || obj.optString("status").equals("received", true)
    } catch (_: Exception) { true }
}

private suspend fun apiCreateManualOrder(uid: String, name: String): Boolean {
    val body = JSONObject().put("uid", uid).put("title", name)
    val (code, txt) = httpPost("/api/orders/create/manual", body)
    return code in 200..299 && (txt?.contains("ok", true) == true)
}

private suspend fun apiGetMyOrders(uid: String): List<OrderItem>? {
    val (code, txt) = httpGet("/api/orders/my?uid=$uid")
    if (code !in 200..299 || txt == null) return null
    return try {
        val trimmed = txt.trim()
        val arr: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            when {
                obj.has("orders") -> obj.optJSONArray("orders") ?: JSONArray()
                obj.has("list")   -> obj.optJSONArray("list") ?: JSONArray()
                else -> JSONArray()
            }
        }
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            OrderItem(
                id = o.optString("id"),
                title = o.optString("title"),
                quantity = o.optInt("quantity"),
                price = o.optDouble("price"),
                payload = o.optString("payload"),
                status = when (o.optString("status")) {
                    "Done" -> OrderStatus.Done
                    "Rejected" -> OrderStatus.Rejected
                    "Refunded" -> OrderStatus.Refunded
                    "Processing" -> OrderStatus.Processing
                    else -> OrderStatus.Pending
                },
                createdAt = o.optLong("created_at")
            )
        }
    } catch (_: Exception) { null }
}

/* دخول المالك */
private suspend fun apiAdminLogin(password: String): String? {
    if (password == "2000") return password
    val (code, _) = httpGet(AdminEndpoints.pendingServices, headers = mapOf("x-admin-password" to password))
    return if (code in 200..299) password else null
}
private suspend fun apiAdminPOST(path: String, token: String, body: JSONObject? = null): Boolean {
    val (code, _) = if (body == null) {
        httpPost(path, JSONObject(), headers = mapOf("x-admin-password" to token))
    } else {
        httpPost(path, body, headers = mapOf("x-admin-password" to token))
    }
    return code in 200..299
}
private suspend fun apiAdminWalletChange(endpoint: String, token: String, uid: String, amount: Double): Boolean {
    val body = JSONObject().put("uid", uid).put("amount", amount)
    val (code, _) = httpPost(endpoint, body, headers = mapOf("x-admin-password" to token))
    return code in 200..299
}
private suspend fun apiAdminUsersCount(token: String): Int? {
    val (c, t) = httpGet(AdminEndpoints.usersCount, mapOf("x-admin-password" to token))
    return if (c in 200..299 && t != null) try { JSONObject(t.trim()).optInt("count") } catch (_: Exception) { null } else null
}
private suspend fun apiAdminUsersBalances(token: String): List<Triple<String,String,Double>>? {
    val (c, t) = httpGet(AdminEndpoints.usersBalances, mapOf("x-admin-password" to token))
    if (c !in 200..299 || t == null) return null
    return try {
        val trimmed = t.trim()
        val arr: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            when {
                root.has("list") -> root.optJSONArray("list") ?: JSONArray()
                root.has("data") -> root.optJSONArray("data") ?: JSONArray()
                else -> JSONArray()
            }
        }
        val out = mutableListOf<Triple<String,String,Double>>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uid = o.optString("uid")
            val bal = o.optDouble("balance", 0.0)
            val banned = if (o.optBoolean("is_banned", false)) "محظور" else "نشط"
            out += Triple(uid, banned, bal)
        }
        out
    } catch (_: Exception) { null }
}

/** فحص رصيد API (KD1S أولًا ثم مسار الباكند) */
private suspend fun apiAdminProviderBalance(token: String): Double? {
    if (PROVIDER_DIRECT_URL.isNotBlank() && PROVIDER_DIRECT_KEY_VALUE.isNotBlank()) {
        val fields = mapOf("key" to PROVIDER_DIRECT_KEY_VALUE, "action" to "balance")
        val (c, t) = httpPostFormAbs(PROVIDER_DIRECT_URL, fields)
        parseBalancePayload(t)?.let { if (c in 200..299) return it }
    }
    val (c2, t2) = httpGet(AdminEndpoints.providerBalance, mapOf("x-admin-password" to token))
    return if (c2 in 200..299) parseBalancePayload(t2) else null
}

private fun parseBalancePayload(t: String?): Double? {
    if (t == null) return null
    val s = t.trim()
    return try {
        when {
            s.matches(Regex("""\d+(\.\d+)?""")) -> s.toDouble()
            s.startsWith("{") -> {
                val o = JSONObject(s)
                when {
                    o.has("balance") -> o.optString("balance").toDoubleOrNull() ?: o.optDouble("balance", Double.NaN)
                    o.has("data") && o.get("data") is JSONObject -> o.getJSONObject("data").optDouble("balance", Double.NaN)
                    else -> Double.NaN
                }.let { if (it.isNaN()) null else it }
            }
            else -> null
        }
    } catch (_: Exception) { null }
}

/* ============== واجهات إدارة الكروت ============== */
private suspend fun apiAdminFetchPendingCards(token: String): List<PendingCard>? {
    val (c, t) = httpGet(AdminEndpoints.pendingCards, mapOf("x-admin-password" to token))
    if (c !in 200..299 || t == null) return null
    return try {
        val arr = JSONArray(t.trim())
        val out = mutableListOf<PendingCard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += PendingCard(
                id = o.optInt("id"),
                uid = o.optString("uid"),
                card = o.optString("card"),
                createdAt = o.optLong("created_at", 0L)
            )
        }
        out
    } catch (_: Exception) { null }
}
private suspend fun apiAdminRejectTopupCard(id: Int, token: String): Boolean {
    val (c, _) = httpPost(AdminEndpoints.topupCardReject(id), JSONObject(), mapOf("x-admin-password" to token))
    return c in 200..299
}
private suspend fun apiAdminExecuteTopupCard(id: Int, amount: Double, token: String): Boolean {
    val (c, _) = httpPost(
        AdminEndpoints.topupCardExecute(id),
        JSONObject().put("amount", amount),
        mapOf("x-admin-password" to token)
    )
    return c in 200..299
}

/* =========================
   الإعدادات + دخول المالك
   ========================= */
@Composable private fun SettingsDialog(
    uid: String,
    ownerMode: Boolean,
    onOwnerLogin: (token: String) -> Unit,
    onOwnerLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var showAdminLogin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("الإعدادات", color = OnBg) },
        text = {
            Column {
                Text("المعرّف الخاص بك (UID):", fontWeight = FontWeight.SemiBold, color = OnBg)
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
                    Text("تسجيل المالك (كلمة المرور):", fontWeight = FontWeight.SemiBold, color = OnBg)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { showAdminLogin = true }) { Text("تسجيل المالك") }
                }
            }
        }
    )

    if (showAdminLogin) {
        var pass by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showAdminLogin = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        err = null
                        val token = apiAdminLogin(pass)
                        if (token != null) { onOwnerLogin(token); showAdminLogin = false }
                        else { err = "بيانات غير صحيحة" }
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showAdminLogin = false }) { Text("إلغاء") } },
            title = { Text("كلمة مرور المالك", color = OnBg) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        singleLine = true,
                        label = { Text("أدخل كلمة المرور") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Accent,
                            focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                            focusedLabelColor = OnBg, unfocusedLabelColor = Dim
                        )
                    )
                    if (err != null) {
                        Spacer(Modifier.height(6.dp)); Text(err!!, color = Bad, fontSize = 12.sp)
                    }
                }
            }
        )
    }
}
