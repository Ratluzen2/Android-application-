@file:Suppress("UnusedImport")

package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

/* =========================================
   إعدادات عامة + مفاتيح
   ========================================= */
private const val API_BASE = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"
private const val OWNER_PIN = "2000" // PIN المالك

/* =========================================
   Theme
   ========================================= */
private val Bg       = Color(0xFF111315)
private val Surface1 = Color(0xFF1A1F24)
private val OnBg     = Color(0xFFEDEFF2)
private val Accent   = Color(0xFFB388FF)
private val Good     = Color(0xFF4CAF50)
private val Bad      = Color(0xFFE53935)
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

/* =========================================
   نماذج بيانات
   ========================================= */
private enum class Tab { HOME, SERVICES, WALLET, ORDERS, SUPPORT, OWNER }

data class AppNotice(
    val id: String,
    val title: String,
    val body: String,
    val ts: Long,
    val forOwner: Boolean,
    val read: Boolean
)

data class ServiceDef(
    val uiKey: String,
    val serviceId: String,
    val min: Int,
    val max: Int,
    val pricePerK: Double,
    val category: String
)

/* فئات الخدمات */
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

/* كتالوج الخدمات المربوطة بـ API مع أكوادك */
private val servicesCatalog = listOf(
    // المتابعين
    ServiceDef("متابعين تيكتوك",   "16256",   100, 1_000_000, 3.5, "المتابعين"),
    ServiceDef("متابعين انستغرام", "16267",   100, 1_000_000, 3.0, "المتابعين"),
    // اللايكات
    ServiceDef("لايكات تيكتوك",    "12320",   100, 1_000_000, 1.0, "الايكات"),
    ServiceDef("لايكات انستغرام",  "1066500", 100, 1_000_000, 1.0, "الايكات"),
    // المشاهدات
    ServiceDef("مشاهدات تيكتوك",   "9448",    100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef("مشاهدات انستغرام", "64686464",100, 1_000_000, 0.1, "المشاهدات"),
    // البث المباشر
    ServiceDef("مشاهدات بث تيكتوك", "14442",  100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef("مشاهدات بث انستا",  "646464", 100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    // رفع سكور
    ServiceDef("رفع سكور البث",     "14662",  100, 1_000_000, 2.0, "رفع سكور تيكتوك"),
    // تلجرام
    ServiceDef("اعضاء قنوات تلي",   "955656", 100, 1_000_000, 3.0, "خدمات التليجرام"),
    ServiceDef("اعضاء كروبات تلي",  "644656", 100, 1_000_000, 3.0, "خدمات التليجرام"),
)

/* =========================================
   MainActivity
   ========================================= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppRoot() } }
    }
}

/* =========================================
   App Root
   ========================================= */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var isOwner by rememberSaveable { mutableStateOf(loadOwnerMode(ctx)) }
    var current by rememberSaveable { mutableStateOf(loadLastTab(ctx, if (isOwner) Tab.OWNER else Tab.HOME)) }

    var online by remember { mutableStateOf<Boolean?>(null) }

    // إشعارات
    var showNoticeCenter by remember { mutableStateOf(false) }
    var userNotices by remember { mutableStateOf<List<AppNotice>>(emptyList()) }
    var ownerNotices by remember { mutableStateOf<List<AppNotice>>(emptyList()) }
    val unreadUser = userNotices.count { !it.read }
    val unreadOwner = ownerNotices.count { !it.read }

    // رصيد المستخدم
    var userBalance by remember { mutableStateOf(0.0) }

    // فحص السيرفر ودورية جلب البيانات
    LaunchedEffect(uid, isOwner) {
        scope.launch { tryUpsertUid(uid) }
        while (true) {
            online = pingHealth()
            // اسحب الرصيد
            fetchUserBalance(uid)?.let { userBalance = it }
            // اسحب الإشعارات
            fetchNotices(uid = uid, forOwner = false)?.let { userNotices = it }
            if (isOwner) fetchNotices(uid = uid, forOwner = true)?.let { ownerNotices = it }
            delay(20_000)
        }
    }

    // حاوية شاشات
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        when (current) {
            Tab.HOME -> HomeScreen()
            Tab.SERVICES -> ServicesScreen(
                uid = uid,
                balance = userBalance,
                onBalanceChanged = { userBalance = it }
            )
            Tab.WALLET -> WalletScreen(
                uid = uid,
                balance = userBalance,
                onBalanceChanged = { userBalance = it }
            )
            Tab.ORDERS -> OrdersScreen(uid = uid)
            Tab.SUPPORT -> SupportScreen()
            Tab.OWNER -> OwnerDashboard(
                ownerUid = uid,
                onRequireRefresh = { /* سيُعاد الجلب في الدورة القادمة */ }
            )
        }

        // الشريط العلوي: حالة الخادم + جرس الإشعارات + إعدادات
        TopRightBar(
            online = online,
            unread = if (isOwner && current == Tab.OWNER) unreadOwner else unreadUser,
            onOpenNotices = { showNoticeCenter = true },
            onOpenSettings = { /* حوار الإعدادات */ },
            isOwner = isOwner,
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

    // إعدادات (UID + تسجيل المالك)
    var settingsOpen by remember { mutableStateOf(false) }
    if (settingsOpen) {
        SettingsDialog(
            uid = uid,
            isOwner = isOwner,
            onDismiss = { settingsOpen = false },
            onOwnerLogin = { pin ->
                if (pin == OWNER_PIN) {
                    isOwner = true
                    saveOwnerMode(ctx, true)
                    saveLastTab(ctx, Tab.OWNER)
                }
            },
            onOwnerLogout = {
                isOwner = false
                saveOwnerMode(ctx, false)
                saveLastTab(ctx, Tab.HOME)
            }
        )
    }

    // مركز الإشعارات
    if (showNoticeCenter) {
        NoticeCenterDialog(
            notices = if (isOwner && current == Tab.OWNER) ownerNotices else userNotices,
            onClear = {
                // تعليم كمقروء على الخادم
                CoroutineScope(Dispatchers.IO).launch {
                    markAllRead(uid = uid, forOwner = isOwner && current == Tab.OWNER)
                }
                showNoticeCenter = false
            },
            onDismiss = { showNoticeCenter = false }
        )
    }
}

/* =========================================
   عناصر واجهة عامة
   ========================================= */
@Composable
private fun HomeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("مرحباً بك 👋", color = OnBg)
    }
}

@Composable
private fun SupportScreen() {
    val uri = LocalUriHandler.current
    val whatsappUrl = "https://wa.me/9647763410970"
    val telegramUrl = "https://t.me/z396r"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
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
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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

/* شريط علوي يمين (حالة الخادم + جرس + إعدادات) */
@Composable
private fun TopRightBar(
    online: Boolean?,
    unread: Int,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    isOwner: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Surface1, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // جرس مع عدد غير المقروء
        BadgedBox(
            badge = { if (unread > 0) Badge { Text(unread.toString()) } }
        ) {
            IconButton(onClick = onOpenNotices, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات", tint = OnBg)
            }
        }
        Spacer(Modifier.width(8.dp))

        // حالة الخادم
        val (txt, clr) = when (online) {
            true -> "الخادم: متصل" to Good
            false -> "الخادم: غير متصل" to Bad
            null -> "الخادم: ..." to Dim
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Surface1, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(clr, shape = RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text(txt, fontSize = 12.sp, color = OnBg)
        }

        Spacer(Modifier.width(8.dp))

        // إعدادات
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = OnBg)
        }
    }
}

/* مركز الإشعارات */
@Composable
private fun NoticeCenterDialog(
    notices: List<AppNotice>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        dismissButton = { TextButton(onClick = onClear) { Text("تمييز كمقروء") } },
        title = { Text("الإشعارات") },
        text = {
            if (notices.isEmpty()) {
                Text("لا توجد إشعارات حالياً", color = Dim)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    notices.sortedByDescending { it.ts }.forEach {
                        val dt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(it.ts))
                        Text("• ${it.title}", fontWeight = FontWeight.SemiBold)
                        Text(it.body, color = Dim, fontSize = 12.sp)
                        Text(dt, color = Dim, fontSize = 10.sp)
                        Divider(Modifier.padding(vertical = 8.dp), color = Surface1)
                    }
                }
            }
        }
    )
}

/* =========================================
   تبويب الخدمات
   ========================================= */
@Composable
private fun ServicesScreen(
    uid: String,
    balance: Double,
    onBalanceChanged: (Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedService by remember { mutableStateOf<ServiceDef?>(null) }

    // شاشة الأقسام
    if (selectedCategory == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("الخدمات", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            serviceCategories.forEach { cat ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
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

    // الخدمات حسب القسم
    val apiItems: List<ServiceDef> = when (selectedCategory) {
        "قسم المتابعين"            -> servicesCatalog.filter { it.category == "المتابعين" }
        "قسم الايكات"              -> servicesCatalog.filter { it.category == "الايكات" }
        "قسم المشاهدات"            -> servicesCatalog.filter { it.category == "المشاهدات" }
        "قسم مشاهدات البث المباشر" -> servicesCatalog.filter { it.category == "مشاهدات البث المباشر" }
        "قسم رفع سكور تيكتوك"     -> servicesCatalog.filter { it.category == "رفع سكور تيكتوك" }
        "قسم خدمات التليجرام"      -> servicesCatalog.filter { it.category == "خدمات التليجرام" }
        else -> emptyList()
    }

    if (apiItems.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
                }
                Spacer(Modifier.width(6.dp))
                Text(selectedCategory!!, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            apiItems.forEach { svc ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { selectedService = svc },
                    colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(svc.uiKey, fontWeight = FontWeight.SemiBold)
                        Text("الكمية: ${svc.min} - ${svc.max}", color = Dim, fontSize = 12.sp)
                        Text("السعر لكل 1000: ${svc.pricePerK}\$", color = Dim, fontSize = 12.sp)
                    }
                }
            }
        }
    } else {
        // الأقسام اليدوية
        ManualSectionsScreen(
            title = selectedCategory!!,
            onBack = { selectedCategory = null },
            uid = uid
        )
    }

    // نافذة طلب خدمة API
    selectedService?.let { svc ->
        ServiceOrderDialog(
            uid = uid,
            service = svc,
            userBalance = balance,
            onBalanceChanged = onBalanceChanged,
            onDismiss = { selectedService = null }
        )
    }
}

/* طلب خدمة مربوطة بالـ API */
@Composable
private fun ServiceOrderDialog(
    uid: String,
    service: ServiceDef,
    userBalance: Double,
    onBalanceChanged: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf(service.min.toString()) }
    val qty = qtyText.toIntOrNull() ?: 0
    val price = ceil((qty / 1000.0) * service.pricePerK * 100) / 100.0
    var working by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        AlertDialog(
            onDismissRequest = { toast = null },
            confirmButton = { TextButton(onClick = { toast = null }) { Text("حسناً") } },
            title = { Text("تنبيه") },
            text = { Text(toast!!) }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        confirmButton = {
            TextButton(
                enabled = !working,
                onClick = {
                    if (link.isBlank()) { toast = "الرجاء إدخال الرابط"; return@TextButton }
                    if (qty < service.min || qty > service.max) {
                        toast = "الكمية يجب أن تكون بين ${service.min} و ${service.max}"
                        return@TextButton
                    }
                    if (userBalance < price) {
                        toast = "رصيدك غير كافٍ. السعر: $price\$ | رصيدك: ${"%.2f".format(userBalance)}$"
                        return@TextButton
                    }
                    working = true
                    scope.launch {
                        val res = placeProviderOrder(uid, service, link, qty)
                        if (res.first) {
                            // خصم محلي مؤقت — الخادم سيحفظ القيمة الفعلية
                            val newBal = (userBalance - price).coerceAtLeast(0.0)
                            onBalanceChanged(newBal)
                            toast = "تم إرسال الطلب بنجاح. رقم الطلب: ${res.second ?: "—"}"
                        } else {
                            toast = "فشل إرسال الطلب:\n${res.third ?: "تعذر الاتصال"}"
                        }
                        working = false
                        onDismiss()
                    }
                }
            ) { Text(if (working) "يرسل..." else "شراء") }
        },
        dismissButton = { TextButton(enabled = !working, onClick = onDismiss) { Text("إلغاء") } },
        title = { Text(service.uiKey) },
        text = {
            Column {
                Text("الكمية بين ${service.min} و ${service.max}", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { s -> if (s.all { it.isDigit() }) qtyText = s },
                    label = { Text("الكمية") },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("الرابط (أرسل الرابط وليس اليوزر)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("السعر التقريبي: $price\$", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("رصيدك الحالي: ${"%.2f".format(userBalance)}$", color = Dim, fontSize = 12.sp)
            }
        }
    )
}

/* أقسام تُراجع يدوياً من المالك */
@Composable
private fun ManualSectionsScreen(
    title: String,
    onBack: () -> Unit,
    uid: String
) {
    val scope = rememberCoroutineScope()
    var toast by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        AlertDialog(
            onDismissRequest = { toast = null },
            confirmButton = { TextButton(onClick = { toast = null }) { Text("حسناً") } },
            title = { Text("تم الإرسال") },
            text = { Text(toast!!) }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))

        val items = when (title) {
            "قسم شراء رصيد ايتونز" -> listOf("شراء رصيد ايتونز")
            "قسم شراء رصيد هاتف"  -> listOf("شراء رصيد اثير", "شراء رصيد اسياسيل", "شراء رصيد كورك")
            "قسم شحن شدات ببجي"    -> listOf("شحن شدات ببجي")
            "قسم خدمات الودو"       -> listOf("شراء الماسات لودو", "شراء ذهب لودو")
            else -> emptyList()
        }

        items.forEach { name ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable {
                        scope.launch {
                            val ok = createManualRequest(uid, name)
                            toast = if (ok) "تم استلام طلبك ($name). سيُراجع من المالك." else "تعذر إرسال الطلب."
                        }
                    },
                colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ChevronLeft, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(name, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* =========================================
   تبويب رصيدي
   ========================================= */
@Composable
private fun WalletScreen(
    uid: String,
    balance: Double,
    onBalanceChanged: (Double) -> Unit
) {
    val scope = rememberCoroutineScope()
    var askAsiacell by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        AlertDialog(
            onDismissRequest = { toast = null },
            confirmButton = { TextButton(onClick = { toast = null }) { Text("حسناً") } },
            title = { Text("تنبيه") },
            text = { Text(toast!!) }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رصيدي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("الرصيد الحالي: ${"%.2f".format(balance)}$", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text("طرق الشحن:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // 1: أسيا سيل
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { askAsiacell = true },
            colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SimCard, null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("شحن عبر أسيا سيل", fontWeight = FontWeight.SemiBold)
            }
        }

        // 2..6: بقية الطرق -> الدعم
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
                    .clickable { toast = "لإتمام الشحن تواصل مع الدعم (واتساب/تيليجرام) من تبويب (الدعم)." },
                colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AttachMoney, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (askAsiacell) {
        AlertDialog(
            onDismissRequest = { askAsiacell = false },
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
            },
            confirmButton = {
                TextButton(onClick = {
                    val digits = cardNumber.filter { it.isDigit() }
                    if (digits.length != 14 && digits.length != 16) return@TextButton
                    // أرسل للمالك كطلب كارت معلّق
                    scope.launch {
                        val ok = submitAsiacellCard(uid, digits)
                        toast = if (ok) "تم إرسال الكارت للمراجعة." else "تعذر إرسال الطلب."
                        askAsiacell = false
                        cardNumber = ""
                    }
                }) { Text("إرسال") }
            },
            dismissButton = { TextButton(onClick = { askAsiacell = false }) { Text("إلغاء") } }
        )
    }
}

/* =========================================
   تبويب طلباتي — يسحب من الخادم
   ========================================= */
@Composable
private fun OrdersScreen(uid: String) {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        loading = true
        scope.launch {
            orders = fetchUserOrders(uid) ?: emptyList()
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (loading) {
            Text("يتم التحميل...", color = Dim)
        } else if (orders.isEmpty()) {
            Text("لا توجد طلبات بعد.", color = Dim)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                orders.forEach { o ->
                    val id = o.optString("order_id", "—")
                    val name = o.optString("name", "—")
                    val qty = o.optInt("quantity", 0)
                    val status = o.optString("status", "pending")
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("رقم الطلب: $id", fontWeight = FontWeight.SemiBold)
                            Text("الخدمة: $name", color = Dim, fontSize = 12.sp)
                            Text("الكمية: $qty", color = Dim, fontSize = 12.sp)
                            Text("الحالة: $status", color = if (status == "completed") Good else Dim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/* =========================================
   لوحة تحكم المالك — واجهات عرض + ربط
   ========================================= */
@Composable
private fun OwnerDashboard(
    ownerUid: String,
    onRequireRefresh: () -> Unit
) {
    var screen by remember { mutableStateOf("MAIN") }
    var payload by remember { mutableStateOf<JSONArray?>(null) }
    val scope = rememberCoroutineScope()

    // قوائم الأزرار
    if (screen == "MAIN") {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = OnBg) // جرس المالك أعلى اليمين
            }
            Spacer(Modifier.height(12.dp))

            OwnerButton("الطلبات المعلقة (الخدمات)") {
                scope.launch {
                    payload = adminFetchList("services")
                    screen = "PENDING_SERVICES"
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("الكارتات المعلقة") {
                scope.launch {
                    payload = adminFetchList("cards")
                    screen = "PENDING_CARDS"
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("طلبات شدات ببجي") {
                scope.launch {
                    payload = adminFetchList("pubg")
                    screen = "PENDING_PUBG"
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("طلبات شحن الايتونز") {
                scope.launch {
                    payload = adminFetchList("itunes")
                    screen = "PENDING_ITUNES"
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("طلبات الارصدة المعلقة") {
                scope.launch {
                    payload = adminFetchList("topups")
                    screen = "PENDING_TOPUPS"
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("طلبات لودو المعلقة") {
                scope.launch {
                    payload = adminFetchList("ludo")
                    screen = "PENDING_LUDO"
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = Surface1)
            Spacer(Modifier.height(16.dp))

            OwnerButton("فحص رصيد API") {
                scope.launch {
                    val res = checkProviderBalance()
                    val msg = if (res.first) {
                        val j = res.second!!
                        "الرصيد: ${j.optString("balance","?")} ${j.optString("currency","")}"
                    } else {
                        "تعذر جلب الرصيد: ${res.third ?: "?"}"
                    }
                    showInfoDialog(msg)
                }
            }
            Spacer(Modifier.height(8.dp))
            OwnerButton("فحص حالة طلب API") {
                askOrderDialog { orderId ->
                    scope.launch {
                        val res = checkOrderStatus(orderId)
                        val msg = if (res.first) {
                            val j = res.second!!
                            "الحالة: ${j.optString("status")} | المتبقي: ${j.optString("remains")} | التكلفة: ${j.optString("charge")} ${j.optString("currency")}"
                        } else {
                            "تعذر الفحص: ${res.third ?: "?"}"
                        }
                        showInfoDialog(msg)
                    }
                }
            }
        }
    }

    // شاشات القوائم المعلّقة
    if (screen.startsWith("PENDING_")) {
        PendingListScreen(
            title = when (screen) {
                "PENDING_SERVICES" -> "الطلبات المعلقة (الخدمات)"
                "PENDING_CARDS"    -> "الكارتات المعلقة"
                "PENDING_PUBG"     -> "طلبات شدات ببجي"
                "PENDING_ITUNES"   -> "طلبات شحن الايتونز"
                "PENDING_TOPUPS"   -> "طلبات الارصدة المعلقة"
                "PENDING_LUDO"     -> "طلبات لودو المعلقة"
                else -> "قائمة"
            },
            rows = payload ?: JSONArray(),
            onBack = { screen = "MAIN" },
            onApprove = { id ->
                scope.launch {
                    val ok = when (screen) {
                        "PENDING_SERVICES" -> adminApprove("services", id)
                        "PENDING_PUBG"     -> adminApprove("pubg", id)
                        "PENDING_ITUNES"   -> adminApprove("itunes", id)
                        "PENDING_TOPUPS"   -> adminApprove("topups", id)
                        "PENDING_LUDO"     -> adminApprove("ludo", id)
                        else               -> false
                    }
                    showInfoDialog(if (ok) "تم التنفيذ." else "فشل التنفيذ.")
                    payload = adminFetchList(when (screen) {
                        "PENDING_SERVICES" -> "services"
                        "PENDING_CARDS"    -> "cards"
                        "PENDING_PUBG"     -> "pubg"
                        "PENDING_ITUNES"   -> "itunes"
                        "PENDING_TOPUPS"   -> "topups"
                        "PENDING_LUDO"     -> "ludo"
                        else -> "services"
                    })
                }
            },
            onReject = { id ->
                scope.launch {
                    val ok = when (screen) {
                        "PENDING_SERVICES" -> adminReject("services", id)
                        "PENDING_CARDS"    -> adminReject("cards", id)
                        "PENDING_PUBG"     -> adminReject("pubg", id)
                        "PENDING_ITUNES"   -> adminReject("itunes", id)
                        "PENDING_TOPUPS"   -> adminReject("topups", id)
                        "PENDING_LUDO"     -> adminReject("ludo", id)
                        else               -> false
                    }
                    showInfoDialog(if (ok) "تم الرفض وإرجاع الرصيد إن وُجد." else "فشل الرفض.")
                    payload = adminFetchList(when (screen) {
                        "PENDING_SERVICES" -> "services"
                        "PENDING_CARDS"    -> "cards"
                        "PENDING_PUBG"     -> "pubg"
                        "PENDING_ITUNES"   -> "itunes"
                        "PENDING_TOPUPS"   -> "topups"
                        "PENDING_LUDO"     -> "ludo"
                        else -> "services"
                    })
                }
            },
            // قبول خاص للكارت: يحتاج إدخال مبلغ
            onApproveCard = { id, amount ->
                scope.launch {
                    val ok = adminApproveCard(id, amount)
                    showInfoDialog(if (ok) "تم قبول الكارت وشحن الرصيد." else "فشل قبول الكارت.")
                    payload = adminFetchList("cards")
                }
            }
        )
    }
}

/* عنصر زر للمالك */
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
    ) { Text(title, fontWeight = FontWeight.SemiBold) }
}

/* بطاقة عناصر معلّقة عامة */
@Composable
private fun PendingListScreen(
    title: String,
    rows: JSONArray,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onApproveCard: (String, Double) -> Unit
) {
    var askAmountForCard by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnBg) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))

        if (rows.length() == 0) {
            Text("لا توجد عناصر حالياً.", color = Dim)
        } else {
            repeat(rows.length()) { idx ->
                val o = rows.optJSONObject(idx) ?: JSONObject()
                val id = o.optString("id", "—")
                val userUid = o.optString("uid", "—")
                val name = o.optString("name", o.optString("service_name", "—"))
                val qty = o.optInt("quantity", 0)
                val price = o.optDouble("price", 0.0)
                val link = o.optString("link", null)
                val requestedAt = o.optString("created_at", "—")

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Surface1)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ID: $id", fontWeight = FontWeight.SemiBold)
                        Text("UID: $userUid", color = Dim, fontSize = 12.sp)
                        Text("الطلب: $name × $qty | السعر: ${"%.2f".format(price)}$", color = Dim, fontSize = 12.sp)
                        link?.let { Text("الرابط: $it", color = Dim, fontSize = 12.sp) }
                        Text("الوقت: $requestedAt", color = Dim, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = { onApprove(id) }) { Text("تنفيذ") }
                            Spacer(Modifier.width(10.dp))
                            TextButton(onClick = { onReject(id) }) { Text("رفض") }
                            if (title == "الكارتات المعلقة") {
                                Spacer(Modifier.width(10.dp))
                                TextButton(onClick = { askAmountForCard = id }) { Text("قبول الكارت (مبلغ)") }
                            }
                        }
                    }
                }
            }
        }
    }

    // إدخال مبلغ الكارت
    askAmountForCard?.let { id ->
        AlertDialog(
            onDismissRequest = { askAmountForCard = null },
            title = { Text("أدخل مبلغ الشحن (بالدولار)") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) amountText = s },
                    placeholder = { Text("مثال: 5.0") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = amountText.toDoubleOrNull()
                    if (v != null && v > 0) {
                        onApproveCard(id, v)
                        amountText = ""
                        askAmountForCard = null
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { askAmountForCard = null }) { Text("إلغاء") } }
        )
    }
}

/* حوار معلومات سريع */
@Composable
private fun showInfoDialog(msg: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = { }) { Text("حسناً") }
        },
        title = { Text("نتيجة") },
        text = { Text(msg) }
    )
}

/* حوار إدخال رقم طلب */
@Composable
private fun askOrderDialog(onOk: (String) -> Unit) {
    var open by remember { mutableStateOf(true) }
    var id by remember { mutableStateOf("") }
    if (!open) return
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("أدخل رقم الطلب") },
        text = {
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                placeholder = { Text("123456") }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val s = id.trim()
                if (s.isNotEmpty()) onOk(s)
                open = false
            }) { Text("تحقق") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("إلغاء") } }
    )
}

/* =========================================
   الشريط السفلي
   ========================================= */
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
            selected = current == Tab.SERVICES,
            onClick = { onChange(Tab.SERVICES) },
            icon = Icons.Filled.List,
            label = "الخدمات"
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
            selected = current == Tab.SUPPORT,
            onClick = { onChange(Tab.SUPPORT) },
            icon = Icons.Filled.ChatBubble,
            label = "الدعم"
        )
        if (isOwner) {
            NavItem(
                selected = current == Tab.OWNER,
                onClick = { onChange(Tab.OWNER) },
                icon = Icons.Filled.AdminPanelSettings,
                label = "المالك"
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

/* =========================================
   تخزين محلي بسيط (UID/وضع المالك/التبويب الأخير فقط)
   ========================================= */
private fun loadOrCreateUid(ctx: Context): String {
    val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val existing = sp.getString("uid", null)
    if (existing != null) return existing
    val fresh = "U" + (100000..999999).random(Random(System.currentTimeMillis()))
    sp.edit().putString("uid", fresh).apply()
    return fresh
}
private fun saveOwnerMode(ctx: Context, enabled: Boolean) {
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("owner_mode", enabled).apply()
}
private fun loadOwnerMode(ctx: Context): Boolean =
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("owner_mode", false)

private fun saveLastTab(ctx: Context, tab: Tab) {
    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putString("last_tab", tab.name).apply()
}
private fun loadLastTab(ctx: Context, defaultTab: Tab): Tab {
    val name = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("last_tab", null)
    return runCatching { if (name != null) Tab.valueOf(name) else defaultTab }.getOrDefault(defaultTab)
}

/* =========================================
   الشبكة — API ROUTES (بدّل المسارات حسب باكندك إن لزم)
   ========================================= */
private suspend fun pingHealth(): Boolean? = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 4000; readTimeout = 4000
        }
        con.connect(); (con.responseCode in 200..299)
    } catch (_: Exception) { false }
}

private suspend fun tryUpsertUid(uid: String) = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/users/upsert").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 5000; readTimeout = 5000
        }
        val body = """{"uid":"$uid"}"""
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.inputStream.bufferedReader().use(BufferedReader::readText)
    } catch (_: Exception) {}
}

/* رصيد المستخدم */
private suspend fun fetchUserBalance(uid: String): Double? = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/users/balance?uid=$uid").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 7000; readTimeout = 7000
        }
        val raw = con.inputStream.bufferedReader().use(BufferedReader::readText)
        JSONObject(raw).optDouble("balance")
    } catch (_: Exception) { null }
}

/* إشعارات */
private suspend fun fetchNotices(uid: String, forOwner: Boolean): List<AppNotice>? =
    withContext(Dispatchers.IO) {
        try {
            val role = if (forOwner) "owner" else "user"
            val con = (URL("$API_BASE/api/notifications/list?uid=$uid&role=$role").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            }
            val raw = con.inputStream.bufferedReader().use(BufferedReader::readText)
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AppNotice(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    ts = o.optLong("ts"),
                    forOwner = o.optBoolean("forOwner", false),
                    read = o.optBoolean("read", false)
                )
            }
        } catch (_: Exception) { null }
    }
private suspend fun markAllRead(uid: String, forOwner: Boolean) = withContext(Dispatchers.IO) {
    try {
        val role = if (forOwner) "owner" else "user"
        val con = (URL("$API_BASE/api/notifications/mark_read").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = """{"uid":"$uid","role":"$role"}"""
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.inputStream.bufferedReader().use(BufferedReader::readText)
    } catch (_: Exception) { }
}

/* طلب خدمة مزوّد */
private suspend fun placeProviderOrder(
    uid: String,
    service: ServiceDef,
    link: String,
    quantity: Int
): Triple<Boolean, String?, String?> = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/provider/order").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val payload = JSONObject()
            .put("uid", uid)
            .put("service_id", service.serviceId)
            .put("quantity", quantity)
            .put("link", link)
            .toString()
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(payload) }
        val code = con.responseCode
        val txt = (if (code in 200..299) con.inputStream else con.errorStream).bufferedReader().use(BufferedReader::readText)
        if (code in 200..299) {
            val j = JSONObject(txt)
            Triple(true, j.optString("order_id", null), txt)
        } else Triple(false, null, txt)
    } catch (e: Exception) {
        Triple(false, null, e.message)
    }
}

/* طلبات المستخدم */
private suspend fun fetchUserOrders(uid: String): List<JSONObject>? = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/orders/list?uid=$uid").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
        }
        val raw = con.inputStream.bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getJSONObject(it) }
    } catch (_: Exception) { null }
}

/* شحن أسيا سيل (يرسل للمالك) */
private suspend fun submitAsiacellCard(uid: String, card: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/wallet/asiacell/submit").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject().put("uid", uid).put("card", card).toString()
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.responseCode in 200..299
    } catch (_: Exception) { false }
}

/* طلب يدوي (ايتونز/اتصالات/ببجي/لودو...) */
private suspend fun createManualRequest(uid: String, name: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/requests/manual").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 10_000; readTimeout = 10_000
        }
        val body = JSONObject().put("uid", uid).put("name", name).toString()
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.responseCode in 200..299
    } catch (_: Exception) { false }
}

/* ====== أدوات مزوّد: رصيد/حالة طلب ====== */
private suspend fun checkProviderBalance(): Triple<Boolean, JSONObject?, String?> = withContext(Dispatchers.IO) {
    val url = URL("$API_BASE/api/provider/balance")
    try {
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        val code = con.responseCode
        val stream = if (code in 200..299) con.inputStream else con.errorStream
        val raw = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        if (code in 200..299) Triple(true, JSONObject(raw), raw) else Triple(false, null, raw)
    } catch (e: Exception) { Triple(false, null, e.message) }
}
private suspend fun checkOrderStatus(orderId: String): Triple<Boolean, JSONObject?, String?> = withContext(Dispatchers.IO) {
    val url = URL("$API_BASE/api/provider/order/status?order_id=$orderId")
    try {
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        val code = con.responseCode
        val stream = if (code in 200..299) con.inputStream else con.errorStream
        val raw = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        if (code in 200..299) {
            val j = JSONObject(raw).optJSONObject("data") ?: JSONObject(raw)
            Triple(true, j, raw)
        } else Triple(false, null, raw)
    } catch (e: Exception) { Triple(false, null, e.message) }
}

/* ====== مسارات الأدمن ====== */
// جلب القوائم المعلّقة حسب النوع
private suspend fun adminFetchList(kind: String): JSONArray? = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/admin/pending/$kind").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
        }
        val raw = con.inputStream.bufferedReader().use(BufferedReader::readText)
        JSONArray(raw)
    } catch (_: Exception) { null }
}

private suspend fun adminApprove(kind: String, id: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/admin/pending/$kind/$id/approve").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000
        }
        con.responseCode in 200..299
    } catch (_: Exception) { false }
}

private suspend fun adminReject(kind: String, id: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/admin/pending/$kind/$id/reject").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000
        }
        con.responseCode in 200..299
    } catch (_: Exception) { false }
}

// قبول كارت أسيا سيل بمبلغ
private suspend fun adminApproveCard(id: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
    try {
        val con = (URL("$API_BASE/api/admin/pending/cards/$id/approve").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject().put("amount", amount).toString()
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        con.responseCode in 200..299
    } catch (_: Exception) { false }
}
