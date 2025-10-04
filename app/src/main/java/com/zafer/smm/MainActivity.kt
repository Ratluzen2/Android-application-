@file:Suppress("UnusedImport")

package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.round

/** تطبيق SMM Mobile — ملف واحد، بدون Navigation Compose. */

// PIN للمالك — غيّره قبل الإصدار
private const val OWNER_PIN = "123456"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

/* =========================
   Theme
   ========================= */
@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography()
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppRoot()
        }
    }
}

/* =========================
   Screens (بدون Navigation)
   ========================= */
enum class Screen {
    USER_HOME, USER_SERVICES, USER_ORDERS, USER_WALLET, USER_SUPPORT, OWNER_DASHBOARD
}

/* =========================
   نماذج البيانات
   ========================= */
data class Order(
    val id: Int,
    val userId: Int,
    val category: String,
    val serviceName: String,
    val qty: Int,
    val price: Double,
    val status: String,
    val link: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CardSubmission(
    val userId: Int,
    val digits: String,
    val ts: Long = System.currentTimeMillis()
)

/* =========================
   ViewModel — تخزين الحالة والعمليات
   ========================= */
class AppViewModel : ViewModel() {

    // --------- إعدادات عامة ---------
    val currentUserId = 1

    // افتراضياً: وضع المالك غير مُفعّل (مخفي عن المستخدمين العاديين)
    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner
    fun enableOwner() { _isOwner.value = true }
    fun disableOwner() { _isOwner.value = false }

    private val _moderators = MutableStateFlow<Set<Int>>(emptySet())
    val moderators: StateFlow<Set<Int>> = _moderators

    private val _balance = MutableStateFlow(15.75)
    val balance: StateFlow<Double> = _balance

    // خرائط الخدمات (الأسماء والسعر الأساسي)
    val servicesTikIgViewsLikesScore = linkedMapOf(
        "متابعين تيكتوك 1k" to 3.50,
        "متابعين تيكتوك 2k" to 7.0,
               "متابعين تيكتوك 3k" to 10.50,
        "متابعين تيكتوك 4k" to 14.0,
        "مشاهدات تيكتوك 1k" to 0.10,
        "مشاهدات تيكتوك 10k" to 0.80,
        "مشاهدات تيكتوك 20k" to 1.60,
        "مشاهدات تيكتوك 30k" to 2.40,
        "مشاهدات تيكتوك 50k" to 3.20,
        "متابعين انستغرام 1k" to 3.0,
        "متابعين انستغرام 2k" to 6.0,
        "متابعين انستغرام 3k" to 9.0,
        "متابعين انستغرام 4k" to 12.0,
        "لايكات تيكتوك 1k" to 1.0,
        "لايكات تيكتوك 2k" to 2.0,
        "لايكات تيكتوك 3k" to 3.0,
        "لايكات تيكتوك 4k" to 4.0,
        "لايكات انستغرام 1k" to 1.0,
        "لايكات انستغرام 2k" to 2.0,
        "لايكات انستغرام 3k" to 3.0,
        "لايكات انستغرام 4k" to 4.0,
        "مشاهدات انستغرام 10k" to 0.80,
        "مشاهدات انستغرام 20k" to 1.60,
        "مشاهدات انستغرام 30k" to 2.40,
        "مشاهدات انستغرام 50k" to 3.20,
        "مشاهدات بث تيكتوك 1k" to 2.0,
        "مشاهدات بث تيكتوك 2k" to 4.0,
        "مشاهدات بث تيكتوك 3k" to 6.0,
        "مشاهدات بث تيكتوك 4k" to 8.0,
        "مشاهدات بث انستغرام 1k" to 2.0,
        "مشاهدات بث انستغرام 2k" to 4.0,
        "مشاهدات بث انستغرام 3k" to 6.0,
        "مشاهدات بث انستغرام 4k" to 8.0,
        "رفع سكور بثك1k" to 2.0,
        "رفع سكور بثك2k" to 4.0,
        "رفع سكور بثك3k" to 6.0,
        "رفع سكور بثك10k" to 20.0,
    )

    val servicesTelegram = linkedMapOf(
        "اعضاء قنوات تلي 1k" to 3.0,
        "اعضاء قنوات تلي 2k" to 6.0,
        "اعضاء قنوات تلي 3k" to 9.0,
        "اعضاء قنوات تلي 4k" to 12.0,
        "اعضاء قنوات تلي 5k" to 15.0,
        "اعضاء كروبات تلي 1k" to 3.0,
        "اعضاء كروبات تلي 2k" to 6.0,
        "اعضاء كروبات تلي 3k" to 9.0,
        "اعضاء كروبات تلي 4k" to 12.0,
        "اعضاء كروبات تلي 5k" to 15.0,
    )

    val servicesPubg = linkedMapOf(
        "ببجي 60 شدة" to 2.0,
        "ببجي 120 شده" to 4.0,
        "ببجي 180 شدة" to 6.0,
        "ببجي 240 شدة" to 8.0,
        "ببجي 325 شدة" to 9.0,
        "ببجي 660 شدة" to 15.0,
        "ببجي 1800 شدة" to 40.0,
    )

    val servicesItunes = linkedMapOf(
        "شراء رصيد 5 ايتونز" to 9.0,
        "شراء رصيد 10 ايتونز" to 18.0,
        "شراء رصيد 15 ايتونز" to 27.0,
        "شراء رصيد 20 ايتونز" to 36.0,
        "شراء رصيد 25 ايتونز" to 45.0,
        "شراء رصيد 30 ايتونز" to 54.0,
        "شراء رصيد 35 ايتونز" to 63.0,
        "شراء رصيد 40 ايتونز" to 72.0,
        "شراء رصيد 45 ايتونز" to 81.0,
        "شراء رصيد 50 ايتونز" to 90.0,
    )

    val servicesMobile = linkedMapOf(
        "شراء رصيد 2دولار اثير" to 2.0,
        "شراء رصيد 5دولار اثير" to 5.0,
        "شراء رصيد 10دولار اثير" to 10.0,
        "شراء رصيد 15دولار اثير" to 15.0,
        "شراء رصيد 40دولار اثير" to 40.0,
        "شراء رصيد 2دولار اسيا" to 2.0,
        "شراء رصيد 5دولار اسيا" to 5.0,
        "شراء رصيد 10دولار اسيا" to 10.0,
        "شراء رصيد 15دولار اسيا" to 15.0,
        "شراء رصيد 40دولار اسيا" to 40.0,
        "شراء رصيد 2دولار كورك" to 2.0,
        "شراء رصيد 5دولار كورك" to 5.0,
        "شراء رصيد 10دولار كورك" to 10.0,
        "شراء رصيد 15دولار كورك" to 15.0,
        "شراء رصيد 40دولار كورك" to 40.0,
    )

    val servicesLudo = linkedMapOf(
        "لودو 810 الماسة" to 3.0,
        "لودو 2280 الماسة" to 7.0,
        "لودو 5080 الماسة" to 13.0,
        "لودو 12750 الماسة" to 28.0,
        "لودو 66680 ذهب" to 3.0,
        "لودو 219500 ذهب" to 7.0,
        "لودو 1443000 ذهب" to 13.0,
        "لودو 3627000 ذهب" to 28.0,
    )

    // Overrides
    private val _priceOverrides = MutableStateFlow<Map<String, Double>>(emptyMap())
    val priceOverrides: StateFlow<Map<String, Double>> = _priceOverrides

    private val _qtyOverrides = MutableStateFlow<Map<String, Int>>(emptyMap()) // لكل خدمة، كمية افتراضية مخصصة
    val qtyOverrides: StateFlow<Map<String, Int>> = _qtyOverrides

    // طلبات + إرسال كروت
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders
    private var orderAutoId = 10000

    private val cardSubmissions = mutableListOf<CardSubmission>()
    private val CARD_DUP_LIMIT = 2
    private val CARD_SPAM_COUNT = 5
    private val CARD_SPAM_WINDOW_MS = 120_000L

    // --------- عمليات ---------
    fun isModerator(userId: Int): Boolean = _moderators.value.contains(userId)
    fun addModerator(userId: Int) { _moderators.value = _moderators.value + userId }
    fun removeModerator(userId: Int) { _moderators.value = _moderators.value - userId }

    fun setPriceOverride(serviceName: String, price: Double) {
        _priceOverrides.value = _priceOverrides.value.toMutableMap().apply { this[serviceName] = price }
    }

    fun setQtyOverride(serviceName: String, qty: Int) {
        _qtyOverrides.value = _qtyOverrides.value.toMutableMap().apply { this[serviceName] = qty }
    }

    fun effectiveBasePrice(userId: Int, serviceName: String, defaultPrice: Double): Double {
        val base = _priceOverrides.value[serviceName] ?: defaultPrice
        return if (isModerator(userId)) round2(base * 0.9) else base
    }

    fun addOrder(
        userId: Int,
        category: String,
        serviceName: String,
        qty: Int,
        price: Double,
        link: String
    ) {
        val o = Order(
            id = ++orderAutoId,
            userId = userId,
            category = category,
            serviceName = serviceName,
            qty = qty,
            price = round2(price),
            status = "pending",
            link = link
        )
        _orders.value = listOf(o) + _orders.value
    }

    fun addBalance(amount: Double) {
        _balance.value = (_balance.value + amount).coerceAtLeast(0.0)
    }

    fun withdrawBalance(amount: Double): Boolean {
        return if (amount <= _balance.value) {
            _balance.value -= amount
            true
        } else false
    }

    fun submitCard(userId: Int, digits: String): Pair<Boolean, String> {
        val now = System.currentTimeMillis()
        // تكرار لنفس الرقم
        val dupCount = cardSubmissions.count { it.userId == userId && it.digits == digits }
        if (dupCount > CARD_DUP_LIMIT) return false to "مرفوض: تكرار لنفس رقم الكارت"

        // سبام خلال نافذة زمنية
        val recentCount = cardSubmissions.count { it.userId == userId && it.ts >= now - CARD_SPAM_WINDOW_MS }
        if (recentCount > CARD_SPAM_COUNT) return false to "مرفوض: محاولات كثيرة خلال وقت قصير"

        cardSubmissions += CardSubmission(userId, digits, now)
        return true to "تم إرسال الكارت للمراجعة"
    }

    // أدوات عامة
    private fun round2(v: Double) = kotlin.math.round(v * 100.0) / 100.0
}

/* =========================
   Helpers: التسعير + تنسيق أسماء التليجرام + استخراج الكمية
   ========================= */
private fun stripK(s: String): String = s.replace("k", "", ignoreCase = true)

private fun cleanedTitleWithoutQty(name: String): String {
    val patterns = listOf(
        "\\s*\\d+\\s*k\\b",
        "\\s*\\d+k\\b",
        "\\s*\\d+\\s*شدة",
        "\\s*\\d+\\s*ايتونز",
        "\\س*\\d+\\s*دولار\\s*(?:اثير|اسيا|كورك)",
        "\\s*\\d+\\s*(?:الماسة|ذهب)",
        "بثك\\s*\\d+\\s*k\\b",
    )
    var base = name
    patterns.forEach { pat ->
        base = Regex(pat, RegexOption.IGNORE_CASE).replace(base, "").trim()
    }
    return stripK(base).trim()
}

private fun extractQtyFromName(name: String): Int {
    val kMatch = Regex("(\\d+)\\s*k\\b", RegexOption.IGNORE_CASE).find(name) ?:
    Regex("(\\d+)k\\b", RegexOption.IGNORE_CASE).find(name) ?:
    Regex("(\\d+)k", RegexOption.IGNORE_CASE).find(name)
    if (kMatch != null) return kMatch.groupValues[1].toInt() * 1000
    val num = Regex("(\\d+)").findAll(name).lastOrNull()?.groupValues?.getOrNull(1)
    return num?.toIntOrNull() ?: 1000
}

private fun labelForTelegram(baseName: String, qty: Int): String {
    val title = cleanedTitleWithoutQty(baseName)
    return "$title - ($qty)" // بدون k
}

private fun priceFor(serviceName: String, qty: Int, basePrice: Double): Double {
    val div = when {
        serviceName.contains("10k", ignoreCase = true) -> 10000.0
        serviceName.contains("ببجي") || serviceName.contains("شدة") -> 1.0
        else -> 1000.0
    }
    val raw = basePrice * (qty.toDouble() / div)
    return (round(raw * 100.0) / 100.0)
}

private fun stepFor(serviceName: String): Int {
    return when {
        serviceName.contains("ببجي") || serviceName.contains("شدة") -> 1
        serviceName.contains("10k", ignoreCase = true) || serviceName.contains("k", ignoreCase = true) -> 1000
        else -> 1000
    }
}

/* =========================
   App Root + Drawer + BottomBar + PIN (بالنقر 5 مرات على العنوان)
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    var current by rememberSaveable { mutableStateOf(Screen.USER_HOME) }
    val isOwner by viewModel.isOwner.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }

    // عداد نقرات العنوان السرّي
    var taps by remember { mutableStateOf(0) }
    var lastTapTs by remember { mutableStateOf(0L) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = current,
                isOwner = isOwner,
                onSelect = {
                    current = it
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "القائمة",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { scope.launch { drawerState.open() } }
                        )
                    },
                    title = {
                        Text(
                            "SMM App",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTs > 2000) { // إعادة ضبط إذا مرّ وقت طويل
                                    taps = 0
                                }
                                taps += 1
                                lastTapTs = now
                                if (taps >= 5) {
                                    taps = 0
                                    showPinDialog = true
                                }
                            }
                        )
                    },
                    actions = {
                        // لا نعرض أيقونة للمالك هنا (كل شيء عبر الحركة السرّية)
                        if (isOwner) {
                            Icon(imageVector = Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                        }
                    }
                )
            },
            bottomBar = {
                BottomBar(current = current, isOwner = isOwner) { current = it }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (current) {
                    Screen.USER_HOME       -> UserHomeScreen(viewModel) { current = it }
                    Screen.USER_SERVICES   -> UserServicesScreen(viewModel)
                    Screen.USER_ORDERS     -> UserOrdersScreen(viewModel)
                    Screen.USER_WALLET     -> UserWalletScreen(viewModel)
                    Screen.USER_SUPPORT    -> UserSupportScreen()
                    Screen.OWNER_DASHBOARD -> OwnerDashboardScreen(viewModel)
                }
            }
        }
    }

    if (showPinDialog) {
        OwnerPinDialog(
            isOwner = isOwner,
            onDismiss = { showPinDialog = false },
            onEnable = { pin ->
                if (pin == OWNER_PIN) viewModel.enableOwner()
            },
            onDisable = { viewModel.disableOwner() }
        )
    }
}

@Composable
private fun OwnerPinDialog(
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onEnable: (String) -> Unit,
    onDisable: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isOwner) "وضع المالك (مفعّل)" else "تفعيل وضع المالك") },
        text = {
            Column {
                if (!isOwner) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; err = null },
                        label = { Text("أدخل PIN") }
                    )
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Text("يمكنك تعطيل وضع المالك من هنا.")
                }
            }
        },
        confirmButton = {
            if (!isOwner) {
                TextButton(onClick = {
                    if (pin.isBlank()) return@TextButton
                    onEnable(pin)
                    onDismiss()
                }) { Text("تفعيل") }
            } else {
                TextButton(onClick = {
                    onDisable()
                    onDismiss()
                }) { Text("تعطيل") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun DrawerContent(
    current: Screen,
    isOwner: Boolean,
    onSelect: (Screen) -> Unit
) {
    Column(Modifier.padding(12.dp)) {
        Text("القوائم", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        NavigationDrawerItem(
            label = { Text("الرئيسية") },
            selected = current == Screen.USER_HOME,
            onClick = { onSelect(Screen.USER_HOME) },
            icon = { Icon(Icons.Filled.Home, null) }
        )
        NavigationDrawerItem(
            label = { Text("الأقسام / الخدمات") },
            selected = current == Screen.USER_SERVICES,
            onClick = { onSelect(Screen.USER_SERVICES) },
            icon = { Icon(Icons.Filled.List, null) }
        )
        NavigationDrawerItem(
            label = { Text("الطلبات") },
            selected = current == Screen.USER_ORDERS,
            onClick = { onSelect(Screen.USER_ORDERS) },
            icon = { Icon(Icons.Filled.ShoppingCart, null) }
        )
        NavigationDrawerItem(
            label = { Text("المحفظة") },
            selected = current == Screen.USER_WALLET,
            onClick = { onSelect(Screen.USER_WALLET) },
            icon = { Icon(Icons.Filled.AccountBalanceWallet, null) }
        )
        NavigationDrawerItem(
            label = { Text("الدعم") },
            selected = current == Screen.USER_SUPPORT,
            onClick = { onSelect(Screen.USER_SUPPORT) },
            icon = { Icon(Icons.Filled.Help, null) }
        )
        if (isOwner) {
            NavigationDrawerItem(
                label = { Text("لوحة المالك") },
                selected = current == Screen.OWNER_DASHBOARD,
                onClick = { onSelect(Screen.OWNER_DASHBOARD) },
                icon = { Icon(Icons.Filled.Dashboard, null) }
            )
        }
    }
}

@Composable
fun BottomBar(
    current: Screen,
    isOwner: Boolean,
    onSelect: (Screen) -> Unit
) {
    val items = remember(isOwner) {
        buildList {
            add(Triple("الرئيسية", Screen.USER_HOME, Icons.Filled.Home))
            add(Triple("الخدمات", Screen.USER_SERVICES, Icons.Filled.List))
            add(Triple("الطلبات", Screen.USER_ORDERS, Icons.Filled.ShoppingCart))
            add(Triple("المحفظة", Screen.USER_WALLET, Icons.Filled.AccountBalanceWallet))
            if (isOwner) add(Triple("المالك", Screen.OWNER_DASHBOARD, Icons.Filled.Dashboard))
            add(Triple("الدعم", Screen.USER_SUPPORT, Icons.Filled.Help))
        }
    }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        items.forEach { (title, screen, icon) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onSelect(screen) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) }
            )
        }
    }
}

/* =========================
   Home — أزرار سريعة + لمحة خدمات
   ========================= */
@Composable
fun UserHomeScreen(viewModel: AppViewModel, onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("مرحباً 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(modifier = Modifier.weight(1f), title = "قوائم الخدمات", icon = Icons.Filled.List) { onOpen(Screen.USER_SERVICES) }
            QuickAction(modifier = Modifier.weight(1f), title = "طلباتي", icon = Icons.Filled.ShoppingCart) { onOpen(Screen.USER_ORDERS) }
            QuickAction(modifier = Modifier.weight(1f), title = "المحفظة", icon = Icons.Filled.AccountBalanceWallet) { onOpen(Screen.USER_WALLET) }
            QuickAction(modifier = Modifier.weight(1f), title = "الدعم", icon = Icons.Filled.Help) { onOpen(Screen.USER_SUPPORT) }
        }
        Spacer(Modifier.height(16.dp))
        Text("أبرز الخدمات", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val sample = listOf(
            viewModel.servicesTikIgViewsLikesScore.keys.first(),
            viewModel.servicesTelegram.keys.first(),
            viewModel.servicesPubg.keys.first(),
            viewModel.servicesItunes.keys.first()
        )
        sample.forEach { name ->
            ServiceCardPreview(name, price = 0.0)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title)
            Spacer(Modifier.height(6.dp))
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ServiceCardPreview(name: String, price: Double) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(name, fontWeight = FontWeight.Medium)
        if (price > 0) Text("السعر الأساسي: $price $", fontSize = 12.sp)
    }
}

/* =========================
   Services — بحث/فلترة/سعر/كمية/شراء/تعديل
   ========================= */
data class BuyInfo(val service: String, val qty: Int, val price: Double)

@Composable
fun UserServicesScreen(viewModel: AppViewModel) {
    val qtyOverrides by viewModel.qtyOverrides.collectAsState()
    val priceOverrides by viewModel.priceOverrides.collectAsState()

    var query by remember { mutableStateOf("") }
    val categories = listOf("TikTok/Instagram/Views/Likes/Score", "Telegram", "PUBG", "iTunes", "Mobile", "Ludo")
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPriceEditor by remember { mutableStateOf<String?>(null) }

    // حالة حوار الشراء
    var buyInfo by remember { mutableStateOf<BuyInfo?>(null) }

    // حفظ كميات لكل خدمة
    val qtyMap = rememberSaveable { mutableStateMapOf<String, Int>() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("الخدمات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث عن خدمة") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { selectedCategory = null }, label = { Text("الكل") }, leadingIcon = { Icon(Icons.Filled.Home, null) })
            categories.forEach { cat ->
                AssistChip(onClick = { selectedCategory = cat }, label = { Text(cat) })
            }
        }

        Spacer(Modifier.height(8.dp))

        val blocks = listOf(
            "TikTok/Instagram/Views/Likes/Score" to viewModel.servicesTikIgViewsLikesScore,
            "Telegram" to viewModel.servicesTelegram,
            "PUBG" to viewModel.servicesPubg,
            "iTunes" to viewModel.servicesItunes,
            "Mobile" to viewModel.servicesMobile,
            "Ludo" to viewModel.servicesLudo,
        )

        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            blocks.forEach { (groupName, data) ->
                if (selectedCategory != null && selectedCategory != groupName) return@forEach
                // عنوان القسم
                item { Text(groupName, fontWeight = FontWeight.SemiBold) }
                items(data.toList(), key = { it.first }) { (svc, base) ->
                    if (query.isNotBlank() && !svc.contains(query, ignoreCase = true)) return@items

                    val step = stepFor(svc)
                    val overrideQty = qtyOverrides[svc]
                    val defaultQty = overrideQty ?: extractQtyFromName(svc)
                    val selectedQty = qtyMap[svc] ?: defaultQty

                    val basePrice = viewModel.effectiveBasePrice(viewModel.currentUserId, svc, base)
                    val currentPrice = priceFor(svc, selectedQty, basePrice)

                    ServiceCard(
                        serviceName = if (groupName == "Telegram") labelForTelegram(svc, selectedQty) else svc,
                        basePrice = basePrice,
                        currentPrice = currentPrice,
                        qty = selectedQty,
                        onDec = { qtyMap[svc] = max(step, selectedQty - step) },
                        onInc = { qtyMap[svc] = selectedQty + step },
                        onBuy = {
                            buyInfo = BuyInfo(svc, selectedQty, currentPrice) // <-- نفتح الحوار عبر الحالة
                        },
                        onEditPrice = { showPriceEditor = svc }
                    )
                }
            }
        }
    }

    // محرر السعر
    if (showPriceEditor != null) {
        var priceTxt by remember { mutableStateOf((priceOverrides[showPriceEditor!!] ?: 0.0).takeIf { it > 0 }?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showPriceEditor = null },
            title = { Text("تعديل سعر: ${showPriceEditor!!}") },
            text = {
                Column {
                    OutlinedTextField(value = priceTxt, onValueChange = { priceTxt = it }, label = { Text("السعر بالدولار") })
                    Spacer(Modifier.height(6.dp))
                    Text("يتم تطبيق خصم 10% تلقائيًا لحسابات المشرفين.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    priceTxt.toDoubleOrNull()?.let {
                        viewModel.setPriceOverride(showPriceEditor!!, it)
                        showPriceEditor = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showPriceEditor = null }) { Text("إلغاء") } }
        )
    }

    // حوار الشراء
    buyInfo?.let { info ->
        var open by remember { mutableStateOf(true) }
        var link by remember { mutableStateOf("") }
        if (open) {
            AlertDialog(
                onDismissRequest = { open = false; buyInfo = null },
                title = { Text("شراء: ${info.service}") },
                text = {
                    Column {
                        Text("الكمية: ${info.qty} — السعر: ${"%.2f".format(info.price)} $")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("الرابط/المعرف") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addOrder(
                            userId = viewModel.currentUserId,
                            category = categories.firstOrNull { info.service.contains("تيكتوك") || info.service.contains("انستغرام") } ?: "smm",
                            serviceName = info.service,
                            qty = info.qty,
                            price = info.price,
                            link = link
                        )
                        open = false
                        buyInfo = null
                    }) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { open = false; buyInfo = null }) { Text("إلغاء") } }
            )
        }
    }
}

@Composable
private fun ServiceCard(
    serviceName: String,
    basePrice: Double,
    currentPrice: Double,
    qty: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onBuy: () -> Unit,
    onEditPrice: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(serviceName, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("السعر الأساسي: ${"%.2f".format(basePrice)} $", fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text("سعر الطلب الحالي: ${"%.2f".format(currentPrice)} $", fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(8.dp))
        QuantityStepper(value = qty, stepLabel = stepFor(serviceName).toString(), onDec = onDec, onInc = onInc)

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedButton(onClick = onBuy, modifier = Modifier.weight(1f)) { Text("شراء") }
            OutlinedButton(onClick = onEditPrice, modifier = Modifier.weight(1f)) { Text("تعديل السعر") }
        }
    }
}

@Composable
private fun QuantityStepper(
    value: Int,
    stepLabel: String,
    onDec: () -> Unit,
    onInc: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDec) { Text("-$stepLabel") }
        Text("$value", fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onInc) { Text("+$stepLabel") }
    }
}

/* =========================
   Orders
   ========================= */
@Composable
fun UserOrdersScreen(viewModel: AppViewModel) {
    val orders by viewModel.orders.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("طلباتي", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Text("لا توجد طلبات بعد.", modifier = Modifier.padding(8.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orders, key = { it.id }) { o ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text("#${o.id} • ${o.serviceName}", fontWeight = FontWeight.SemiBold)
                        Text("${o.category} • ${o.qty} • ${"%.2f".format(o.price)}$")
                        Text("الحالة: ${o.status}")
                    }
                }
            }
        }
    }
}

/* =========================
   Wallet + Anti-Spam Cards
   ========================= */
@Composable
fun UserWalletScreen(viewModel: AppViewModel) {
    val balance by viewModel.balance.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }

    var cardDigits by remember { mutableStateOf("") }
    var cardMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("المحفظة", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الرصيد الحالي", fontSize = 14.sp)
                Text(String.format("%.2f $", balance), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedButton(onClick = { showAdd = true }) { Text("إيداع") }
                    OutlinedButton(onClick = { showWithdraw = true }) { Text("سحب") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("إرسال كارت آسياسيل (حماية ضد التكرار/السبام):")
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = cardDigits,
            onValueChange = { cardDigits = it },
            label = { Text("رقم الكارت") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        ElevatedButton(onClick = {
            val (ok, msg) = viewModel.submitCard(viewModel.currentUserId, cardDigits.trim())
            cardMsg = msg
            if (ok) cardDigits = ""
        }) { Text("إرسال الكارت") }
        cardMsg?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
    }

    if (showAdd) {
        var amount by remember { mutableStateOf("5.00") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("إضافة رصيد") },
            text = {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("القيمة بالدولار") })
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toDoubleOrNull()?.let { viewModel.addBalance(it); showAdd = false }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } }
        )
    }

    if (showWithdraw) {
        var amount by remember { mutableStateOf("2.00") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showWithdraw = false },
            title = { Text("سحب رصيد") },
            text = {
                Column {
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("القيمة بالدولار") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toDoubleOrNull()?.let { v ->
                        if (!viewModel.withdrawBalance(v)) error = "الرصيد غير كافٍ"
                        else showWithdraw = false
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showWithdraw = false }) { Text("إلغاء") } }
        )
    }
}

/* =========================
   Support
   ========================= */
@Composable
fun UserSupportScreen() {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("تواصل معنا:\n- تيليجرام: @your_channel\n- البريد: support@example.com")
    }
}

/* =========================
   Owner Dashboard — أسعار/كميات/مشرفون
   ========================= */
@Composable
fun OwnerDashboardScreen(viewModel: AppViewModel) {
    val moderators by viewModel.moderators.collectAsState()
    val priceOverrides by viewModel.priceOverrides.collectAsState()
    val qtyOverrides by viewModel.qtyOverrides.collectAsState()

    var svcForPrice by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var svcForQty by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("") }
    var modIdTxt by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("لوحة تحكم المالك", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("تعديل الأسعار والكميات + إدارة المشرفين. أي تعديل ينعكس فورًا على واجهة المستخدم.", fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("تعديل الأسعار", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = svcForPrice, onValueChange = { svcForPrice = it }, label = { Text("اسم الخدمة (بالضبط)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("سعر جديد بالدولار") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = {
                        newPrice.toDoubleOrNull()?.let { p ->
                            if (svcForPrice.isNotBlank()) viewModel.setPriceOverride(svcForPrice.trim(), p)
                        }
                    }) { Text("حفظ السعر") }
                    OutlinedButton(onClick = { svcForPrice = ""; newPrice = "" }) { Text("تفريغ") }
                }
                Spacer(Modifier.height(10.dp))
                Text("Overrides الحالية:")
                priceOverrides.forEach { (k, v) -> Text("- $k = ${"%.2f".format(v)}$") }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("تعديل الكمية الافتراضية", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = svcForQty, onValueChange = { svcForQty = it }, label = { Text("اسم الخدمة (بالضبط)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newQty, onValueChange = { newQty = it }, label = { Text("الكمية (عدد صحيح)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = {
                        newQty.toIntOrNull()?.let { q ->
                            if (svcForQty.isNotBlank()) viewModel.setQtyOverride(svcForQty.trim(), q)
                        }
                    }) { Text("حفظ الكمية") }
                    OutlinedButton(onClick = { svcForQty = ""; newQty = "" }) { Text("تفريغ") }
                }
                Spacer(Modifier.height(10.dp))
                Text("كميات افتراضية مخصصة:")
                qtyOverrides.forEach { (k, v) -> Text("- $k = $v") }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("إدارة المشرفين (خصم 10%)", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = modIdTxt, onValueChange = { modIdTxt = it }, label = { Text("User ID رقمي") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = {
                        modIdTxt.toIntOrNull()?.let { viewModel.addModerator(it) }
                    }) { Text("إضافة مشرف") }
                    OutlinedButton(onClick = {
                        modIdTxt.toIntOrNull()?.let { viewModel.removeModerator(it) }
                    }) { Text("حذف مشرف") }
                }
                Spacer(Modifier.height(10.dp))
                Text("قائمة المشرفين:")
                if (moderators.isEmpty()) Text("- لا يوجد")
                else moderators.sorted().forEach { Text("- $it") }
            }
        }
    }
}
