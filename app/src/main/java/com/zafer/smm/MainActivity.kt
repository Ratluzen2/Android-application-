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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
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
   نماذج وبيانات مساعدة
   ========================= */
data class AppNotice(val title: String, val body: String, val ts: Long = System.currentTimeMillis(), val forOwner: Boolean = false)

enum class Tab { HOME, SERVICES, WALLET, ORDERS, SUPPORT }

/* خدمات الـ API المربوطة حسب طلبك */
data class ServiceDef(
    val uiKey: String,            // يجب أن يطابق مفاتيح الباكند العربية
    val min: Int,
    val max: Int,
    val pricePerK: Double,        // السعر لكل 1000
    val category: String          // لعرضها ضمن القسم
)

private val servicesCatalog = listOf(
    // المتابعين
    ServiceDef("متابعين تيكتوك",   100, 1_000_000, 3.5, "المتابعين"),
    ServiceDef("متابعين انستغرام", 100, 1_000_000, 3.0, "المتابعين"),
    // اللايكات
    ServiceDef("لايكات تيكتوك",    100, 1_000_000, 1.0, "الايكات"),
    ServiceDef("لايكات انستغرام",  100, 1_000_000, 1.0, "الايكات"),
    // المشاهدات
    ServiceDef("مشاهدات تيكتوك",    100, 1_000_000, 0.1, "المشاهدات"),
    ServiceDef("مشاهدات انستغرام",  100, 1_000_000, 0.1, "المشاهدات"),
    // البث المباشر
    ServiceDef("مشاهدات بث تيكتوك", 100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    ServiceDef("مشاهدات بث انستا",  100, 1_000_000, 2.0, "مشاهدات البث المباشر"),
    // رفع سكور
    ServiceDef("رفع سكور البث",     100, 1_000_000, 2.0, "رفع سكور تيكتوك"),
    // تلجرام
    ServiceDef("اعضاء قنوات تلي",   100, 1_000_000, 3.0, "خدمات التليجرام"),
    ServiceDef("اعضاء كروبات تلي",  100, 1_000_000, 3.0, "خدمات التليجرام"),
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
   AppRoot
   ========================= */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // UID — يُنشأ تلقائياً ويُرسل للخادم مرة واحدة
    var uid by remember { mutableStateOf(loadOrCreateUid(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }

    // وضع المالك محفوظ
    var ownerMode by remember { mutableStateOf(loadOwnerMode(ctx)) }
    var ownerToken by remember { mutableStateOf(loadOwnerToken(ctx)) }

    // حالة السيرفر
    var online by remember { mutableStateOf<Boolean?>(null) }

    // إشعارات
    var notices by remember { mutableStateOf(loadNotices(ctx)) }
    var showNoticeCenter by remember { mutableStateOf(false) }
    val unreadCount = notices.count { !it.forOwner } // مؤشر بسيط للمستخدم

    // فحص السيرفر دوري + تسجيل UID
    LaunchedEffect(Unit) {
        scope.launch { tryUpsertUid(uid) }
        while (true) {
            online = pingHealth()
            delay(20_000)
        }
    }

    // شريط أسفل
    var current by remember { mutableStateOf(Tab.HOME) }

    /* رسائل “توست” خفيفة */
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2000)
            toast = null
        }
    }

    // الحاوية الرئيسية
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // محتوى كل تبويب
        when (current) {
            Tab.HOME -> {
                if (ownerMode) {
                    OwnerPanel(
                        onShowOwnerNotices = { showNoticeCenter = true },
                        onToast = { toast = it }
                    )
                } else {
                    HomeScreen()
                }
            }
            Tab.SERVICES -> ServicesScreen(
                uid = uid,
                onAddNotice = {
                    notices = (notices + it)
                    saveNotices(ctx, notices)
                },
                onToast = { toast = it },
                ctx = ctx
            )
            Tab.WALLET -> WalletScreen(
                uid = uid,
                onAddNotice = {
                    notices = (notices + it)
                    saveNotices(ctx, notices)
                },
                onToast = { toast = it },
                ctx = ctx
            )
            Tab.ORDERS -> OrdersScreen()
            Tab.SUPPORT -> SupportScreen()
        }

        // الشريط العلوي يمين: حالة السيرفر + جرس الإشعارات + إعدادات
        TopRightBar(
            online = online,
            unread = unreadCount,
            onOpenNotices = { showNoticeCenter = true },
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

        // توست
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

    if (settingsOpen) {
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
            onDismiss = { settingsOpen = false }
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
   شاشات بسيطة
   ========================= */
@Composable private fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Text("مرحباً بك 👋", color = OnBg)
    }
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

/* =========================
   الشريط العلوي يمين (حالة السيرفر + الجرس + الإعدادات)
   ========================= */
@Composable
private fun TopRightBar(
    online: Boolean?,
    unread: Int,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Surface1, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // جرس إشعارات
        BadgedBox(badge = {
            if (unread > 0) {
                Badge { Text(unread.toString()) }
            }
        }) {
            IconButton(onClick = onOpenNotices, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات", tint = OnBg)
            }
        }

        Spacer(Modifier.width(6.dp))

        // حالة السيرفر
        val (txt, clr) = when (online) {
            true  -> "الخادم: متصل" to Good
            false -> "الخادم: غير متصل" to Bad
            null  -> "الخادم: ..." to Dim
        }
        Box(
            modifier = Modifier
                .background(Surface1, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(clr, shape = MaterialTheme.shapes.small)
                )
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
    notices: List<AppNotice>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("مسح الإشعارات") }
        },
        title = { Text("الإشعارات") },
        text = {
            if (notices.isEmpty()) {
                Text("لا توجد إشعارات حالياً", color = Dim)
            } else {
                Column {
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

/* =========================
   تبويب الخدمات
   ========================= */
@Composable
private fun ServicesScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit,
    ctx: Context
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
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(cat, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    // شاشة الخدمات داخل القسم
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
        // أقسام مربوطة بالـ API
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
        // أقسام يدوية (يُراجعها المالك يدوياً لاحقاً)
        ManualSectionsScreen(
            title = selectedCategory!!,
            onBack = { selectedCategory = null },
            onToast = onToast,
            onAddNotice = onAddNotice,
            uid = uid
        )
    }

    selectedService?.let { svc ->
        ServiceOrderDialog(
            uid = uid,
            service = svc,
            onDismiss = { selectedService = null },
            onOrdered = { orderOk, msg ->
                onToast(msg)
                if (orderOk) {
                    onAddNotice(AppNotice("طلب جديد (${svc.uiKey})", "تم استلام طلبك وسيتم تنفيذه قريباً.", forOwner = false))
                    // إشعار للمالك
                    onAddNotice(AppNotice("طلب خدمات معلّق", "طلب ${svc.uiKey} من UID=$uid بانتظار المعالجة/التنفيذ", forOwner = true))
                }
            },
            ctx = ctx
        )
    }
}

/* طلب خدمة مربوطة بالـ API */
@Composable
private fun ServiceOrderDialog(
    uid: String,
    service: ServiceDef,
    onDismiss: () -> Unit,
    onOrdered: (Boolean, String) -> Unit,
    ctx: Context
) {
    var link by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf(service.min.toString()) }
    val qty = qtyText.toIntOrNull() ?: 0
    val price = ceil((qty / 1000.0) * service.pricePerK * 100) / 100.0

    val balance = remember { mutableStateOf(loadBalance(ctx)) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    if (link.isBlank()) {
                        onOrdered(false, "الرجاء إدخال الرابط")
                        return@TextButton
                    }
                    if (qty < service.min || qty > service.max) {
                        onOrdered(false, "الكمية يجب أن تكون بين ${service.min} و ${service.max}")
                        return@TextButton
                    }
                    if (balance.value < price) {
                        onOrdered(false, "رصيدك غير كافٍ. السعر: $price\$ | رصيدك: ${balance.value}\$")
                        return@TextButton
                    }
                    loading = true
                    scope.launch {
                        val ok = placeProviderOrder(service.uiKey, link, qty)
                        if (ok) {
                            val newBal = (balance.value - price).coerceAtLeast(0.0)
                            saveBalance(ctx, newBal)
                            balance.value = newBal
                            onOrdered(true, "تم إرسال الطلب بنجاح. رقم الطلب سيظهر بعد التنفيذ.")
                        } else {
                            onOrdered(false, "فشل إرسال الطلب إلى الخادم")
                        }
                        loading = false
                        onDismiss()
                    }
                }
            ) { Text(if (loading) "يرسل..." else "شراء") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
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
                Text("رصيدك الحالي: ${balance.value}\$", color = Dim, fontSize = 12.sp)
            }
        }
    )
}

/* أقسام يدوية: تُنشئ إشعارات فقط الآن (لأن تنفيذها يدوي من المالك) */
@Composable
private fun ManualSectionsScreen(
    title: String,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    onAddNotice: (AppNotice) -> Unit,
    uid: String
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = OnBg)
            }
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
                        onToast("تم استلام طلبك ($name). سيُراجع من المالك.")
                        // إشعار للمستخدم
                        onAddNotice(AppNotice("طلب معلّق", "تم إرسال طلب $name للمراجعة.", forOwner = false))
                        // إشعار للمالك
                        onAddNotice(AppNotice("طلب يدوي جديد", "طلب $name من UID=$uid يحتاج مراجعة.", forOwner = true))
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

/* =========================
   تبويب رصيدي
   ========================= */
@Composable
private fun WalletScreen(
    uid: String,
    onAddNotice: (AppNotice) -> Unit,
    onToast: (String) -> Unit,
    ctx: Context
) {
    var balance by remember { mutableStateOf(loadBalance(ctx)) }
    var askAsiacell by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رصيدي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("الرصيد الحالي: ${"%.2f".format(balance)}$", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text("طرق الشحن:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // 1: أسيا سيل (يطلب رقم كارت)
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

        // 2..6: باقي الطرق — توجيه للدعم
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
            confirmButton = {
                TextButton(onClick = {
                    val digitsOnly = cardNumber.filter { it.isDigit() }
                    if (digitsOnly.length != 14 && digitsOnly.length != 16) return@TextButton
                    val now = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
                    // إشعار للمالك
                    onAddNotice(
                        AppNotice(
                            "كارت أسيا سيل جديد",
                            "رقم الكارت: $cardNumber | UID=$uid | الوقت: $now",
                            forOwner = true
                        )
                    )
                    // إشعار للمستخدم
                    onAddNotice(
                        AppNotice(
                            "تم استلام كارتك",
                            "تم إرسال كارت أسيا سيل إلى المالك للمراجعة.",
                            forOwner = false
                        )
                    )
                    cardNumber = ""
                    askAsiacell = false
                    onToast("تم إرسال الكارت للمراجعة.")
                }) { Text("إرسال") }
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

/* تبويب طلباتي — عرض بسيط (Placeholder) */
@Composable private fun OrdersScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("ستظهر الطلبات هنا عندما نربط التخزين الكامل في الخادم.", color = Dim, fontSize = 12.sp)
    }
}

/* =========================
   لوحة تحكم المالك (تظهر عند تفعيل وضع المالك)
   ========================= */
@Composable
private fun OwnerPanel(
    onShowOwnerNotices: () -> Unit,
    onToast: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("لوحة تحكم المالك", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // جرس إشعارات المالك أعلى يمين
            IconButton(onClick = onShowOwnerNotices) {
                Icon(Icons.Filled.Notifications, contentDescription = "إشعارات المالك", tint = OnBg)
            }
        }
        Spacer(Modifier.height(12.dp))

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

        buttons.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { title ->
                    ElevatedButton(
                        onClick = {
                            when (title) {
                                "فحص رصيد API" -> onToast("استخدم زر فحص الرصيد في الإعدادات القديمة (موجود).")
                                "فحص حالة طلب API" -> onToast("استعمل ميزة فحص حالة الطلب من القائمة المضافة سابقاً.")
                                else -> onToast("$title — قريباً")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Surface1,
                            contentColor = OnBg
                        )
                    ) { Text(title, fontSize = 12.sp) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* =========================
   نافذة الإعدادات — UID + تسجيل المالك
   ========================= */
@Composable
private fun SettingsDialog(
    uid: String,
    ownerMode: Boolean,
    onOwnerLogin: (String) -> Unit,
    onOwnerLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val clip: ClipboardManager = LocalClipboardManager.current
    var askPass by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
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
                Spacer(Modifier.height(12.dp))
                Divider(color = Surface1)
                Spacer(Modifier.height(12.dp))
                if (ownerMode) {
                    Text("وضع المالك: مفعل", color = Good, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onOwnerLogout) { Text("تسجيل خروج المالك") }
                } else {
                    Text("تسجيل المالك (خاص بالمالك فقط):", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { askPass = true }) { Text("تسجيل المالك") }
                }
                err?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Bad, fontSize = 12.sp)
                }
            }
        }
    )

    if (askPass) {
        AlertDialog(
            onDismissRequest = { askPass = false },
            confirmButton = {
                TextButton(onClick = {
                    val pw = pass.trim()
                    if (pw.isEmpty()) return@TextButton
                    scope.launch {
                        val token = apiAdminLogin(pw)
                        if (token != null) {
                            onOwnerLogin(token) // نخزن كلمة المرور كتوكن
                            askPass = false
                            err = null
                        } else {
                            err = "بيانات غير صحيحة"
                        }
                    }
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

/* =========================
   أدوات تخزين محلي (رصيد + إشعارات + وضع المالك)
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

private fun loadBalance(ctx: Context): Double = java.lang.Double.longBitsToDouble(
    prefs(ctx).getLong("user_balance_bits", java.lang.Double.doubleToLongBits(0.0))
)
private fun saveBalance(ctx: Context, v: Double) {
    prefs(ctx).edit().putLong("user_balance_bits", java.lang.Double.doubleToLongBits(v)).apply()
}

private fun loadNotices(ctx: Context): List<AppNotice> {
    val raw = prefs(ctx).getString("notices_json", "[]") ?: "[]"
    return try {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AppNotice(
                title = o.optString("title"),
                body = o.optString("body"),
                ts = o.optLong("ts"),
                forOwner = o.optBoolean("forOwner")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
private fun saveNotices(ctx: Context, notices: List<AppNotice>) {
    val arr = org.json.JSONArray()
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

/* =========================
   الشبكة: حالة الخادم + طلبات المزود
   ========================= */
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
        // تجاهل — لا يؤثر على البناء
    }
}

/* إرسال طلب إلى مزود الخدمات عبر الباكند */
private suspend fun placeProviderOrder(serviceKey: String, link: String, quantity: Int): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/api/provider/order")
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = JSONObject()
                .put("service_key", serviceKey)
                .put("link", link)
                .put("quantity", quantity)
                .toString()
            OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(payload) }
            val code = con.responseCode
            val txt = (if (code in 200..299) con.inputStream else con.errorStream)
                .bufferedReader().use(BufferedReader::readText)
            code in 200..299 && txt.contains("ok", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

/* =========================
   شريط سفلي
   ========================= */
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
   نقاط تكامل الأدمن (المسارات + دوال الشبكة)
   ========================= */
private object AdminEndpoints {
    val pendingServices   = "$API_BASE/api/admin/pending/services"
    val pendingItunes     = "$API_BASE/api/admin/pending/itunes"
    val pendingCards      = "$API_BASE/api/admin/pending/cards"
    val pendingPubg       = "$API_BASE/api/admin/pending/pubg"
    val pendingLudo       = "$API_BASE/api/admin/pending/ludo"

    val topup            = "$API_BASE/api/admin/wallet/topup"
    val deduct           = "$API_BASE/api/admin/wallet/deduct"

    val usersCount       = "$API_BASE/api/admin/users/count"
    val usersBalances    = "$API_BASE/api/admin/users/balances"
    val providerBalance  = "$API_BASE/api/admin/provider/balance"
}

/* == تعديل مهم: التحقق من كلمة مرور المالك عبر هيدر x-admin-pass بدلاً من login == */
private suspend fun apiAdminLogin(password: String): String? {
    // نتحقق من كلمة المرور عبر استدعاء أي مسار أدمن محمي بالهيدر x-admin-pass
    return try {
        val (code, txt) = httpGet(AdminEndpoints.usersCount, headers = mapOf("x-admin-pass" to password))
        if (code in 200..299) password else null
    } catch (_: Exception) {
        null
    }
}

/* أمثلة دوال شبكية للأدمن (كلها ترسل x-admin-pass) */
private suspend fun apiAdminGetPending(token: String, type: String): Pair<Int, String?> {
    val url = when (type) {
        "services" -> AdminEndpoints.pendingServices
        "itunes"   -> AdminEndpoints.pendingItunes
        "cards"    -> AdminEndpoints.pendingCards
        "pubg"     -> AdminEndpoints.pendingPubg
        "ludo"     -> AdminEndpoints.pendingLudo
        else       -> AdminEndpoints.pendingServices
    }
    return httpGet(url, headers= mapOf("x-admin-pass" to token))
}

private suspend fun apiAdminTopup(token: String, uid: String, amount: Double): Pair<Int, String?> {
    val body = JSONObject().put("uid", uid).put("amount", amount).toString()
    return httpPost(AdminEndpoints.topup, body, headers = mapOf("x-admin-pass" to token))
}

private suspend fun apiAdminDeduct(token: String, uid: String, amount: Double): Pair<Int, String?> {
    val body = JSONObject().put("uid", uid).put("amount", amount).toString()
    return httpPost(AdminEndpoints.deduct, body, headers = mapOf("x-admin-pass" to token))
}

private suspend fun apiAdminUsersCount(token: String): Pair<Int, String?> {
    return httpGet(AdminEndpoints.usersCount, headers= mapOf("x-admin-pass" to token))
}

private suspend fun apiAdminUsersBalances(token: String): Pair<Int, String?> {
    return httpGet(AdminEndpoints.usersBalances, headers= mapOf("x-admin-pass" to token))
}

private suspend fun apiAdminProviderBalance(token: String): Pair<Int, String?> {
    return httpGet(AdminEndpoints.providerBalance, headers= mapOf("x-admin-pass" to token))
}

/* =========================
   أدوات HTTP عامة
   ========================= */
private suspend fun httpGet(urlStr: String, headers: Map<String, String>? = null): Pair<Int, String?> =
    withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            headers?.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        val code = con.responseCode
        val txt = (if (code in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.use(BufferedReader::readText)
        code to txt
    }

private suspend fun httpPost(urlStr: String, jsonBody: String, headers: Map<String, String>? = null): Pair<Int, String?> =
    withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers?.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
        val code = con.responseCode
        val txt = (if (code in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.use(BufferedReader::readText)
        code to txt
    }
