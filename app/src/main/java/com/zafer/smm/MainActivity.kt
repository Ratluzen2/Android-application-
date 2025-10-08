// app/src/main/java/com/zafer/smm/MainActivity.kt
@file:Suppress("SpellCheckingInspection", "UnusedImport")

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

/* =========================
   إعدادات الخادم
   ========================= */
private const val API_BASE = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"

// مسارات عامة
private object PublicEndpoints {
    const val health                 = "/health"
    const val upsertUid              = "/api/users/upsert"                // POST {uid}
    const val walletBalance          = "/api/wallet/balance"              // GET ?uid=
    const val createProviderOrder    = "/api/orders/create/provider"      // POST {...}
    const val createManualOrder      = "/api/orders/create/manual"        // POST {uid,title}
    const val myOrders               = "/api/orders/my"                   // GET ?uid=
    const val submitAsiacellCard     = "/api/wallet/asiacell/submit"      // POST {uid,card}
}

// مسارات الأدمن (يجب تمرير x-admin-pass بالهيدر)
private object AdminEndpoints {
    const val pendingServices        = "/api/admin/pending/services"               // GET
    const val approveService         = "/api/admin/pending/services/{id}/approve"  // POST
    const val rejectService          = "/api/admin/pending/services/{id}/reject"   // POST

    const val pendingItunes          = "/api/admin/pending/itunes"                 // GET
    const val pendingTopups          = "/api/admin/pending/cards"                  // GET (كروت أسيا سيل)
    const val pendingPubg            = "/api/admin/pending/pubg"                   // GET
    const val pendingLudo            = "/api/admin/pending/ludo"                   // GET

    const val deliverItunes          = "/api/admin/pending/itunes/{id}/deliver"    // POST (gift_code)
    const val rejectItunes           = "/api/admin/pending/itunes/{id}/reject"     // POST

    const val deliverTopup           = "/api/admin/pending/cards/{id}/accept"      // POST (amount_usd)
    const val rejectTopup            = "/api/admin/pending/cards/{id}/reject"      // POST

    const val usersCount             = "/api/admin/users/count"                    // GET -> {count}
    const val usersBalances          = "/api/admin/users/balances"                 // GET -> {list:[{uid,balance,...}]}
    const val providerBalance        = "/api/admin/provider/balance"               // GET -> {balance}

    const val userTopup(uid: String) = "/api/admin/users/$uid/topup"               // POST {amount}
    const val userDeduct(uid: String)= "/api/admin/users/$uid/deduct"              // POST {amount}
}

/* =========================
   Theme (داكن واضح)
   ========================= */
private val Bg       = Color(0xFF0F1113)
private val Surface1 = Color(0xFF171B20)
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
            onBackground = OnBg,
            onSurface = OnBg,
            primary = Accent
        ),
        content = content
    )
}

/* =========================
   نماذج
   ========================= */
enum class Tab { HOME, SERVICES, WALLET, ORDERS, SUPPORT }

data class AppNotice(
    val title: String,
    val body: String,
    val ts: Long = System.currentTimeMillis(),
    val forOwner: Boolean = false
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

data class ServiceDef(
    val uiKey: String,
    val serviceId: Long,    // KD1S service_code
    val min: Int,
    val max: Int,
    val pricePerK: Double,
    val category: String
)

/* كتالوج الخدمات (أمثلة) */
private val servicesCatalog = listOf(
    // المتابعين
    ServiceDef("متابعين تيكتوك",   16256,   100, 1_000_000, 3.5, "المتابعين"),
    ServiceDef("متابعين انستغرام", 16267,   100, 1_000_000, 3.0, "المتابعين"),
    // اللايكات
    ServiceDef("لايكات تيكتوك",    12320,   100, 1_000_000, 1.0, "الايكات"),
    ServiceDef("لايكات انستغرام",  1066500, 100, 1_000_000, 1.0, "الايكات"),
    // المشاهدات
    ServiceDef("مشاهدات تيكتوك",    9448,     100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef("مشاهدات انستغرام",  64686464, 100, 1_000_000, 0.1, "المشاهدات"),
    // البث المباشر
    ServiceDef("مشاهدات بث تيكتوك", 14442, 100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef("مشاهدات بث انستا",   646464,100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    // رفع سكور
    ServiceDef("رفع سكور البث",     14662, 100, 1_000_000, 2.0, "رفع سكور تيكتوك"),
    // التلغرام
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

    // اخفاء التوست بعد 2 ثانية
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
                        onShowOwnerNotices = { showNoticeCenter = true },
                        onToast = { toast = it },
                        onRequireLogin = { showSettings = true }
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
                    Text(
                        msg,
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = OnBg
                    )
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
    Box(
        Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center
    ) { Text("مرحبًا بك 👋", color = OnBg, fontSize = 18.sp) }
}

@Composable private fun SupportScreen() {
    val uri = LocalUriHandler.current
    val whatsappUrl = "https://wa.me/9647763410970"
    val telegramUrl = "https://t.me/z396r"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الدعم", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg)
        Spacer(Modifier.height(12.dp))
        Text("للتواصل أو الاستفسار اختر إحدى الطرق التالية:", color = OnBg)
        Spacer(Modifier.height(12.dp))
        ContactCard(
            title = "واتساب", subtitle = "+964 776 341 0970",
            actionText = "افتح واتساب", onClick = { uri.openUri(whatsappUrl) }, icon = Icons.Filled.Call
        )
        Spacer(Modifier.height(10.dp))
        ContactCard(
            title = "تيليجرام", subtitle = "@z396r",
            actionText = "افتح تيليجرام", onClick = { uri.openUri(telegramUrl) }, icon = Icons.Filled.Send
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
            TextButton(onClick = onClick) { Text(actionText, color = Accent) }
        }
    }
}

/* =========================
   الشريط العلوي يمين: جرس + حالة الخادم + إعدادات
   (Badge يدوي لتجنب API تجريبية)
   ========================= */
@Composable
private fun TopRightBar(
    online: Boolean?, unread: Int,
    onOpenNotices: () -> Unit, onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Surface1, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // جرس بإشارة عدّاد يدوية
        Box {
            IconButton(onClick = onOpenNotices, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات", tint = OnBg)
            }
            if (unread > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(16.dp)
                        .background(color = Bad, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(unread.toString(), color = Color.White, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.width(6.dp))

        // حالة الخادم
        val (txt, clr) = when (online) {
            true -> "الخادم: متصل" to Good
            false -> "الخادم: غير متصل" to Bad
            else -> "الخادم: ..." to Dim
        }
        Surface(color = Surface1, shape = MaterialTheme.shapes.small) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(clr, shape = CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(txt, fontSize = 12.sp, color = OnBg)
            }
        }
        Spacer(Modifier.width(6.dp))

        // إعدادات
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = OnBg)
        }
    }
}

/* مركز الإشعارات */
@Composable
private fun NoticeCenterDialog(
    notices: List<AppNotice>, onClear: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        dismissButton = { TextButton(onClick = onClear) { Text("مسح الإشعارات") } },
        title = { Text("الإشعارات", color = OnBg) },
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
   تبويب الخدمات
   ========================= */
@Composable
private fun ServicesScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedService by remember { mutableStateOf<ServiceDef?>(null) }

    if (selectedCategory == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("الخدمات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg)
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
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

/* حوار تنفيذ طلب خدمة مربوطة بالمزوّد */
@Composable
private fun ServiceOrderDialog(
    uid: String,
    service: ServiceDef,
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
            TextButton(
                enabled = !loading,
                onClick = {
                    if (link.isBlank()) { onOrdered(false, "الرجاء إدخال الرابط"); return@TextButton }
                    if (qty < service.min || qty > service.max) { onOrdered(false, "الكمية بين ${service.min} و ${service.max}"); return@TextButton }
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
                }
            ) { Text(if (loading) "يرسل..." else "شراء", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = OnBg) } },
        title = { Text(service.uiKey, color = OnBg) },
        text = {
            Column {
                Text("الكمية بين ${service.min} و ${service.max}", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { s -> if (s.all { it.isDigit() }) qtyText = s },
                    label = { Text("الكمية", color = OnBg) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Accent,
                        focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                        focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                        focusedTextColor = OnBg, unfocusedTextColor = OnBg
                    )
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = link, onValueChange = { link = it },
                    label = { Text("الرابط (أرسل الرابط وليس اليوزر)", color = OnBg) },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Accent,
                        focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                        focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                        focusedTextColor = OnBg, unfocusedTextColor = OnBg
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

/* الأقسام اليدوية */
@Composable
private fun ManualSectionsScreen(
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
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
            }
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
   تبويب رصيدي
   ========================= */
@Composable
private fun WalletScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
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
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnBg
        )
        Spacer(Modifier.height(16.dp))
        Text("طرق الشحن:", fontWeight = FontWeight.SemiBold, color = OnBg)
        Spacer(Modifier.height(8.dp))

        // 1) أسيا سيل (كارت)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { askAsiacell = true },
            colors = CardDefaults.elevatedCardColors(
                containerColor = Surface1,
                contentColor = OnBg
            )
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SimCard, null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("شحن عبر أسيا سيل (كارت)", fontWeight = FontWeight.SemiBold, color = OnBg)
            }
        }

        // باقي الطرق — توجيه للدعم
        listOf(
            "شحن عبر هلا بي",
            "شحن عبر نقاط سنتات",
            "شحن عبر سوبركي",
            "شحن عبر زين كاش",
            "شحن عبر عملات رقمية (USDT)"
        ).forEach {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable {
                        onToast("لإتمام الشحن تواصل مع الدعم (واتساب/تيليجرام).")
                        onAddNotice(AppNotice("شحن رصيد", "يرجى التواصل مع الدعم لإكمال شحن: $it", forOwner = false))
                    },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Surface1,
                    contentColor = OnBg
                )
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
                TextButton(enabled = !sending, onClick = {
                    val digits = cardNumber.filter { it.isDigit() }
                    if (digits.length != 14 && digits.length != 16) return@TextButton
                    sending = true
                    scope.launch {
                        val ok = apiSubmitAsiacellCard(uid, digits)
                        sending = false
                        if (ok) {
                            onAddNotice(AppNotice("تم استلام كارتك", "تم إرسال كارت أسيا سيل إلى المالك للمراجعة.", forOwner = false))
                            onAddNotice(AppNotice("كارت أسيا سيل جديد", "UID=$uid | كارت: $digits", forOwner = true))
                            onToast("تم إرسال الكارت للمراجعة.")
                            cardNumber = ""
                            askAsiacell = false
                        } else {
                            // خطة بديلة: افتح واتساب برسالة جاهزة إذا فشل الخادم
                            val msg = "أرغب بشحن الرصيد لرقمي داخل التطبيق.\nUID=$uid\nكارت أسيا سيل: $digits"
                            uri.openUri("https://wa.me/9647763410970?text=" + java.net.URLEncoder.encode(msg, "UTF-8"))
                            onToast("تعذر الاتصال بالخادم — تم فتح واتساب لإرسال الكارت للدعم.")
                            cardNumber = ""
                            askAsiacell = false
                        }
                    }
                }) { Text(if (sending) "يرسل..." else "إرسال", color = Accent) }
            },
            dismissButton = { TextButton(enabled = !sending, onClick = { askAsiacell = false }) { Text("إلغاء", color = OnBg) } },
            title = { Text("شحن عبر أسيا سيل", color = OnBg) },
            text = {
                Column {
                    Text("أدخل رقم الكارت (14 أو 16 رقم):", color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { s -> if (s.all { it.isDigit() }) cardNumber = s },
                        singleLine = true,
                        label = { Text("رقم الكارت", color = OnBg) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Accent,
                            focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                            focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                            focusedTextColor = OnBg, unfocusedTextColor = OnBg
                        )
                    )
                }
            }
        )
    }
}

/* =========================
   تبويب طلباتي — من الخادم
   ========================= */
@Composable
private fun OrdersScreen(uid: String) {
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
            else -> {
                LazyColumn {
                    items(orders!!) { o ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Surface1,
                                contentColor = OnBg
                            )
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
}

/* =========================
   لوحة تحكم المالك
   ========================= */
@Composable
private fun OwnerPanel(
    token: String?,
    onShowOwnerNotices: () -> Unit,
    onToast: (String) -> Unit,
    onRequireLogin: () -> Unit
) {
    var current by remember { mutableStateOf<String?>(null) } // اسم الشاشة الفرعية

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBg, modifier = Modifier.weight(1f))
            IconButton(onClick = onShowOwnerNotices) {
                Icon(Icons.Filled.Notifications, contentDescription = "إشعارات المالك", tint = OnBg)
            }
        }
        Spacer(Modifier.height(12.dp))

        fun needToken(): Boolean {
            if (token.isNullOrBlank()) {
                onToast("سجل دخول المالك أولًا من الإعدادات.")
                onRequireLogin()
                return true
            }
            return false
        }

        if (current == null) {
            val buttons = listOf(
                "الطلبات المعلقة (الخدمات)" to "pending_services",
                "طلبات شحن الايتونز"      to "pending_itunes",
                "الكارتات المعلقة"         to "pending_topups",
                "طلبات شدات ببجي"         to "pending_pubg",
                "طلبات لودو المعلقة"       to "pending_ludo",
                "إضافة الرصيد"             to "topup",
                "خصم الرصيد"               to "deduct",
                "عدد المستخدمين"           to "users_count",
                "رصيد المستخدمين (قائمة)"  to "users_balances",
                "فحص رصيد API"             to "provider_balance"
            )

            buttons.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (title, key) ->
                        ElevatedButton(
                            onClick = {
                                if (needToken()) return@ElevatedButton
                                current = key
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Accent.copy(alpha = 0.18f),
                                contentColor = OnBg
                            )
                        ) { Text(title, fontSize = 12.sp, color = OnBg) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            when (current) {
                "pending_services" -> PendingListScreen(
                    title = "الطلبات المعلقة (الخدمات)",
                    fetch = { apiAdminGetPendingOrders(token!!, AdminEndpoints.pendingServices) },
                    actApprove = { id -> apiAdminApproveService(token!!, id) },
                    actReject =  { id -> apiAdminRejectService(token!!, id) },
                    onBack = { current = null }
                )
                "pending_itunes" -> PendingListScreen(
                    title = "طلبات شحن الايتونز",
                    fetch = { apiAdminGetPendingOrders(token!!, AdminEndpoints.pendingItunes) },
                    actApprove = { id -> apiAdminDeliverItunes(token!!, id) },
                    actReject =  { id -> apiAdminRejectItunes(token!!, id) },
                    onBack = { current = null },
                    askValueLabel = "أدخل كود الهداية (gift code)"
                )
                "pending_topups" -> PendingListScreen(
                    title = "الكارتات المعلقة",
                    fetch = { apiAdminGetPendingOrders(token!!, AdminEndpoints.pendingTopups) },
                    actApprove = { id -> apiAdminDeliverTopup(token!!, id) },
                    actReject =  { id -> apiAdminRejectTopup(token!!, id) },
                    onBack = { current = null },
                    askValueLabel = "قيمة الشحن بالدولار للمستخدم"
                )
                "pending_pubg" -> PendingListScreen(
                    title = "طلبات شدات ببجي",
                    fetch = { apiAdminGetPendingOrders(token!!, AdminEndpoints.pendingPubg) },
                    actApprove = { _ -> true }, // تنفيذ يدوي حالياً
                    actReject  = { _ -> true },
                    onBack = { current = null }
                )
                "pending_ludo" -> PendingListScreen(
                    title = "طلبات لودو المعلقة",
                    fetch = { apiAdminGetPendingOrders(token!!, AdminEndpoints.pendingLudo) },
                    actApprove = { _ -> true },
                    actReject  = { _ -> true },
                    onBack = { current = null }
                )
                "topup" -> TopupDeductScreen(
                    title = "إضافة الرصيد",
                    onSubmit = { u, amt -> apiAdminTopup(token!!, u, amt) },
                    onBack = { current = null }
                )
                "deduct" -> TopupDeductScreen(
                    title = "خصم الرصيد",
                    onSubmit = { u, amt -> apiAdminDeduct(token!!, u, amt) },
                    onBack = { current = null }
                )
                "users_count" -> UsersCountScreen(
                    fetch = { apiAdminUsersCount(token!!) },
                    onBack = { current = null }
                )
                "users_balances" -> UsersBalancesScreen(
                    fetch = { apiAdminUsersBalancesList(token!!) },
                    onBack = { current = null }
                )
                "provider_balance" -> ProviderBalanceScreen(
                    fetch = { apiAdminProviderBalance(token!!) },
                    onBack = { current = null }
                )
            }
        }
    }
}

/* قائمة معلّقة عامة للأدمن + حوار طلب قيمة/كود عند التنفيذ */
@Composable
private fun PendingListScreen(
    title: String,
    fetch: suspend () -> List<OrderItem>?,
    actApprove: suspend (String) -> Boolean,
    actReject: suspend (String) -> Boolean,
    onBack: () -> Unit,
    askValueLabel: String? = null
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<OrderItem>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    var confirmId by remember { mutableStateOf<String?>(null) }
    var rejectId  by remember { mutableStateOf<String?>(null) }
    var askValueForId by remember { mutableStateOf<String?>(null) }
    var valueText by remember { mutableStateOf("") }

    LaunchedEffect(reloadKey) {
        loading = true
        err = null
        list = fetch()
        loading = false
        if (list == null) err = "تعذر جلب البيانات"
    }

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
            else -> {
                LazyColumn {
                    items(list!!) { o ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Surface1,
                                contentColor = OnBg
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(o.title, fontWeight = FontWeight.SemiBold, color = OnBg)
                                Text("ID: ${o.id} | الكمية: ${o.quantity} | السعر: ${"%.2f".format(o.price)}$", color = Dim, fontSize = 12.sp)
                                if (o.payload.isNotBlank())
                                    Text("تفاصيل: ${o.payload}", color = Dim, fontSize = 12.sp)
                                Text("الحالة: ${o.status}", color = OnBg, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    Button(
                                        onClick = {
                                            if (askValueLabel != null) {
                                                askValueForId = o.id
                                                valueText = ""
                                            } else {
                                                confirmId = o.id
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Good,
                                            contentColor = Color.Black
                                        )
                                    ) { Text("تنفيذ") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { rejectId = o.id },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Bad,
                                            contentColor = Color.White
                                        )
                                    ) { Text("رفض") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // تأكيد تنفيذ بدون قيمة إضافية
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = actApprove(id)
                        confirmId = null
                        if (ok) reloadKey++
                    }
                }) { Text("تأكيد", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("إلغاء", color = OnBg) } },
            title = { Text("تنفيذ الطلب", color = OnBg) },
            text = { Text("هل تريد تنفيذ الطلب $id ؟", color = OnBg) }
        )
    }

    // تنفيذ مع قيمة (مثل كود آيتونز أو مبلغ الكارت)
    if (askValueForId != null && askValueLabel != null) {
        AlertDialog(
            onDismissRequest = { askValueForId = null },
            confirmButton = {
                TextButton(onClick = {
                    val id = askValueForId ?: return@TextButton
                    scope.launch {
                        val ok = actApprove(id) // الدالة المعطاة تعالج داخليًا إرسال القيمة عبر endpoint خاص
                        askValueForId = null
                        if (ok) reloadKey++
                    }
                }) { Text("إرسال", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { askValueForId = null }) { Text("إلغاء", color = OnBg) } },
            title = { Text("بيانات مطلوبة", color = OnBg) },
            text = {
                Column {
                    Text(askValueLabel, color = OnBg)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Accent,
                            focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                            focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                            focusedTextColor = OnBg, unfocusedTextColor = OnBg
                        )
                    )
                }
            }
        )
    }

    // رفض
    rejectId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectId = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = actReject(id)
                        rejectId = null
                        if (ok) reloadKey++
                    }
                }) { Text("تأكيد", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { rejectId = null }) { Text("إلغاء", color = OnBg) } },
            title = { Text("رفض الطلب", color = OnBg) },
            text = { Text("هل تريد رفض الطلب $id ؟", color = OnBg) }
        )
    }
}

/* شاشات بسيطة للأدمن */
@Composable
private fun TopupDeductScreen(
    title: String,
    onSubmit: suspend (String, Double) -> Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var uid by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    // اخفاء التوست بعد 2 ثانية
    LaunchedEffect(toast) { if (toast != null) { delay(2000); toast = null } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = uid,
            onValueChange = { uid = it },
            label = { Text("UID المستخدم", color = OnBg) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Accent,
                focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                focusedTextColor = OnBg, unfocusedTextColor = OnBg
            )
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) amount = s },
            label = { Text("المبلغ", color = OnBg) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Accent,
                focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                focusedTextColor = OnBg, unfocusedTextColor = OnBg
            )
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val a = amount.toDoubleOrNull() ?: return@Button
                scope.launch {
                    val ok = onSubmit(uid.trim(), a)
                    toast = if (ok) "تم بنجاح" else "فشل التنفيذ"
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black
            )
        ) { Text("تنفيذ") }

        toast?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = OnBg)
        }
    }
}

@Composable
private fun UsersCountScreen(fetch: suspend () -> Int?, onBack: () -> Unit) {
    var v by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { v = fetch() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("عدد المستخدمين", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(16.dp))
        Text("القيمة: ${v ?: "..."}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
    }
}

@Composable
private fun UsersBalancesScreen(fetch: suspend () -> List<Pair<String, Double>>?, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<Pair<String, Double>>?>(null) }
    LaunchedEffect(Unit) { list = fetch() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("رصيد المستخدمين", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(12.dp))
        if (list == null) {
            Text("يتم التحميل...", color = Dim)
        } else if (list!!.isEmpty()) {
            Text("لا يوجد بيانات.", color = Dim)
        } else {
            LazyColumn {
                items(list!!) { (uid, bal) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uid, modifier = Modifier.weight(1f), color = OnBg)
                        Text("${"%.2f".format(bal)}$", color = OnBg)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderBalanceScreen(fetch: suspend () -> Double?, onBack: () -> Unit) {
    var v by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(Unit) { v = fetch() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text("رصيد المزوّد (API)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnBg)
        }
        Spacer(Modifier.height(16.dp))
        Text("الرصيد: ${v?.let { "%.2f".format(it) } ?: "..."}$", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
    }
}

/* =========================
   الإعدادات + دخول المالك
   ========================= */
@Composable
private fun SettingsDialog(
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = OnBg) } },
        title = { Text("الإعدادات", color = OnBg) },
        text = {
            Column {
                Text("المعرّف الخاص بك (UID):", fontWeight = FontWeight.SemiBold, color = OnBg)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uid, color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { clip.setText(AnnotatedString(uid)) }) { Text("نسخ", color = OnBg) }
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = Surface1)
                Spacer(Modifier.height(12.dp))

                if (ownerMode) {
                    Text("وضع المالك: مفعل", color = Good, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onOwnerLogout) { Text("تسجيل خروج المالك", color = OnBg) }
                } else {
                    Text("تسجيل المالك:", fontWeight = FontWeight.SemiBold, color = OnBg)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { showAdminLogin = true }) { Text("تسجيل المالك", color = OnBg) }
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
                        if (token != null) {
                            onOwnerLogin(token)
                            showAdminLogin = false
                        } else {
                            err = "بيانات غير صحيحة"
                        }
                    }
                }) { Text("تأكيد", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { showAdminLogin = false }) { Text("إلغاء", color = OnBg) } },
            title = { Text("كلمة مرور/رمز المالك", color = OnBg) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        singleLine = true,
                        label = { Text("أدخل كلمة المرور أو الرمز", color = OnBg) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Accent,
                            focusedBorderColor = Accent, unfocusedBorderColor = Dim,
                            focusedLabelColor = OnBg, unfocusedLabelColor = Dim,
                            focusedTextColor = OnBg, unfocusedTextColor = OnBg
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

/* =========================
   شريط سفلي
   ========================= */
@Composable
private fun BottomNavBar(
    current: Tab, onChange: (Tab) -> Unit, modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier.fillMaxWidth(), containerColor = Surface1) {
        NavItem(current == Tab.HOME,     { onChange(Tab.HOME) },     Icons.Filled.Home,                "الرئيسية")
        NavItem(current == Tab.SERVICES, { onChange(Tab.SERVICES) }, Icons.Filled.List,                "الخدمات")
        NavItem(current == Tab.WALLET,   { onChange(Tab.WALLET) },   Icons.Filled.AccountBalanceWallet, "رصيدي")
        NavItem(current == Tab.ORDERS,   { onChange(Tab.ORDERS) },   Icons.Filled.ShoppingCart,         "الطلبات")
        NavItem(current == Tab.SUPPORT,  { onChange(Tab.SUPPORT) },  Icons.Filled.ChatBubble,           "الدعم")
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean, onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String
) {
    NavigationBarItem(
        selected = selected, onClick = onClick,
        icon = { Icon(icon, contentDescription = label, tint = if (selected) Color.White else Dim) },
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else Dim
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Accent.copy(alpha = 0.25f),
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            unselectedIconColor = Dim,
            unselectedTextColor = Dim
        )
    )
}

/* =========================
   تخزين محلي + شبكة
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
private fun saveOwnerToken(ctx: Context, token: String?) {
    prefs(ctx).edit().putString("owner_token", token).apply()
}

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

/* ===== شبكة بسيطة ===== */
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

private suspend fun httpPost(path: String, json: JSONObject, headers: Map<String, String> = emptyMap()): Pair<Int, String?> =
    withContext(Dispatchers.IO) {
        try {
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

/* ===== وظائف مشتركة مع الخادم ===== */
private suspend fun pingHealth(): Boolean? {
    val (code, _) = httpGet(PublicEndpoints.health)
    return when {
        code in 200..299 -> true
        code == -1 -> false
        else -> false
    }
}

private suspend fun tryUpsertUid(uid: String) {
    httpPost(PublicEndpoints.upsertUid, JSONObject().put("uid", uid))
}

/* رصيد المستخدم */
private suspend fun apiGetBalance(uid: String): Double? {
    val (code, txt) = httpGet("${PublicEndpoints.walletBalance}?uid=$uid")
    return if (code in 200..299 && txt != null) {
        try { JSONObject(txt).optDouble("balance") } catch (_: Exception) { null }
    } else null
}

/* طلب موفّر (تخزين pending + service_code) */
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
    val (code, txt) = httpPost(PublicEndpoints.createProviderOrder, body)
    return code in 200..299 && (txt?.contains("ok", ignoreCase = true) == true)
}

/* طلب يدوي */
private suspend fun apiCreateManualOrder(uid: String, name: String): Boolean {
    val body = JSONObject().put("uid", uid).put("title", name)
    val (code, txt) = httpPost(PublicEndpoints.createManualOrder, body)
    return code in 200..299 && (txt?.contains("ok", true) == true)
}

/* إرسال كارت أسيا سيل */
private suspend fun apiSubmitAsiacellCard(uid: String, card: String): Boolean {
    val (code, txt) = httpPost(
        PublicEndpoints.submitAsiacellCard,
        JSONObject().put("uid", uid).put("card", card)
    )
    return code in 200..299 && (txt?.contains("ok", true) == true)
}

/* طلبات المستخدم */
private suspend fun apiGetMyOrders(uid: String): List<OrderItem>? {
    val (code, txt) = httpGet("${PublicEndpoints.myOrders}?uid=$uid")
    if (code !in 200..299 || txt == null) return null
    return try {
        val arr = JSONArray(txt)
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

/* دخول المالك — نقبل 2000 محليًا أو نتحقق بنداء */
private suspend fun apiAdminLogin(password: String): String? {
    if (password == "2000") return password
    return try {
        val headers = mapOf("x-admin-pass" to password)
        val (code, _) = httpGet(AdminEndpoints.pendingServices, headers = headers)
        if (code in 200..299) password else null
    } catch (_: Exception) { null }
}

/* جلب المعلّقات بصيغة مشتركة */
private suspend fun apiAdminGetPendingOrders(token: String, endpoint: String): List<OrderItem>? {
    val (code, txt) = httpGet(endpoint, headers = mapOf("x-admin-pass" to token))
    if (code !in 200..299 || txt == null) return null
    // الخادم يعيد {"ok":true,"list":[...]} أو يعيد مصفوفة مباشرة — ندعم الحالتين
    return try {
        val root = JSONObject(txt)
        val arr = when {
            root.has("list") -> root.getJSONArray("list")
            else -> JSONArray(txt)
        }
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            OrderItem(
                id = o.opt("id").toString(),
                title = o.optString("title", o.optString("service_key", "طلب")),
                quantity = o.optInt("quantity", 0),
                price = o.optDouble("price", 0.0),
                payload = o.optString("link", o.optString("payload", "")),
                status = OrderStatus.Pending,
                createdAt = (o.optLong("created_at", System.currentTimeMillis()))
            )
        }
    } catch (_: Exception) { null }
}

/* تنفيذ/رفض الخدمات (KD1S يتم من الخادم عند الموافقة) */
private suspend fun apiAdminApproveService(token: String, id: String): Boolean {
    val path = AdminEndpoints.approveService.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject(), mapOf("x-admin-pass" to token))
    return code in 200..299
}
private suspend fun apiAdminRejectService(token: String, id: String): Boolean {
    val path = AdminEndpoints.rejectService.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject(), mapOf("x-admin-pass" to token))
    return code in 200..299
}

/* آيتونز */
private suspend fun apiAdminDeliverItunes(token: String, id: String): Boolean {
    // هذه الدالة تتطلب gift_code — في هذا القالب، نفّذ من الخادم عند استلام القيمة من Dialog
    // إذا أردت تمرير الكود من هنا، عدّل PendingListScreen لإرسال valueText داخل body:
    val path = AdminEndpoints.deliverItunes.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject().put("gift_code", "CODE"), mapOf("x-admin-pass" to token))
    return code in 200..299
}
private suspend fun apiAdminRejectItunes(token: String, id: String): Boolean {
    val path = AdminEndpoints.rejectItunes.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject(), mapOf("x-admin-pass" to token))
    return code in 200..299
}

/* كروت أسيا سيل (قبول مع مبلغ) */
private suspend fun apiAdminDeliverTopup(token: String, id: String): Boolean {
    val path = AdminEndpoints.deliverTopup.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject().put("amount_usd", 1.0), mapOf("x-admin-pass" to token))
    return code in 200..299
}
private suspend fun apiAdminRejectTopup(token: String, id: String): Boolean {
    val path = AdminEndpoints.rejectTopup.replace("{id}", id)
    val (code, _) = httpPost(path, JSONObject(), mapOf("x-admin-pass" to token))
    return code in 200..299
}

/* شحن/خصم رصيد يدوي */
private suspend fun apiAdminTopup(token: String, uid: String, amount: Double): Boolean {
    val (code, _) = httpPost(AdminEndpoints.userTopup(uid), JSONObject().put("amount", amount), mapOf("x-admin-pass" to token))
    return code in 200..299
}
private suspend fun apiAdminDeduct(token: String, uid: String, amount: Double): Boolean {
    val (code, _) = httpPost(AdminEndpoints.userDeduct(uid), JSONObject().put("amount", amount), mapOf("x-admin-pass" to token))
    return code in 200..299
}

/* إحصائيات */
private suspend fun apiAdminUsersCount(token: String): Int? {
    val (code, txt) = httpGet(AdminEndpoints.usersCount, mapOf("x-admin-pass" to token))
    if (code !in 200..299 || txt == null) return null
    return try { JSONObject(txt).optInt("count") } catch (_: Exception) { null }
}
private suspend fun apiAdminUsersBalancesList(token: String): List<Pair<String, Double>>? {
    val (code, txt) = httpGet(AdminEndpoints.usersBalances, mapOf("x-admin-pass" to token))
    if (code !in 200..299 || txt == null) return null
    return try {
        val root = JSONObject(txt)
        val arr = root.getJSONArray("list")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            o.optString("uid") to o.optDouble("balance", 0.0)
        }
    } catch (_: Exception) { null }
}
private suspend fun apiAdminProviderBalance(token: String): Double? {
    val (code, txt) = httpGet(AdminEndpoints.providerBalance, mapOf("x-admin-pass" to token))
    if (code !in 200..299 || txt == null) return null
    return try { JSONObject(txt).optDouble("balance") } catch (_: Exception) { null }
}
