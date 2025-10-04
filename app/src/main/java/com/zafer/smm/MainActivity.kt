package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

/* ====================================================================== */
/*                ⬇️ كل البيانات (الأقسام + الخدمات) هنا ⬇️              */
/* ====================================================================== */

data class LocalService(val id: Int, val name: String, val price: Double)
data class LocalSection(val key: String, val title: String, val services: List<LocalService>)

object LocalCatalog {
    val sections: List<LocalSection> = listOf(
        LocalSection(key = "followers", title = "قسم المتابعين", services = listOf(
            LocalService(id = 1, name = "متابعين تيكتوك 100", price = 1.0),
            LocalService(id = 2, name = "متابعين تيكتوك 200", price = 2.0),
            LocalService(id = 3, name = "متابعين تيكتوك 300", price = 3.0),
            LocalService(id = 4, name = "متابعين تيكتوك 400", price = 4.0),
            LocalService(id = 5, name = "متابعين تيكتوك 500", price = 5.0),
            LocalService(id = 6, name = "متابعين تيكتوك 1000", price = 9.0),
            LocalService(id = 7, name = "متابعين تيكتوك 2000", price = 18.0),
            LocalService(id = 8, name = "متابعين تيكتوك 3000", price = 27.0),
            LocalService(id = 9, name = "متابعين تيكتوك 4000", price = 36.0),
            LocalService(id = 10, name = "متابعين تيكتوك 5000", price = 45.0)
        )),
        LocalSection(key = "likes", title = "قسم الإعجابات", services = listOf(
            LocalService(id = 1, name = "لايكات 1k", price = 2.5),
            LocalService(id = 2, name = "لايكات 2k", price = 5.0),
            LocalService(id = 3, name = "لايكات 3k", price = 7.5),
            LocalService(id = 4, name = "لايكات 4k", price = 10.0),
            LocalService(id = 5, name = "لايكات 5k", price = 12.5)
        )),
        LocalSection(key = "views", title = "قسم المشاهدات", services = listOf(
            LocalService(id = 1, name = "مشاهدات تيكتوك 1k", price = 0.5),
            LocalService(id = 2, name = "مشاهدات تيكتوك 2k", price = 1.0),
            LocalService(id = 3, name = "مشاهدات تيكتوك 3k", price = 1.5),
            LocalService(id = 4, name = "مشاهدات تيكتوك 4k", price = 2.0),
            LocalService(id = 5, name = "مشاهدات تيكتوك 5k", price = 2.5),
            LocalService(id = 6, name = "مشاهدات تيكتوك 10k", price = 4.5)
        )),
        LocalSection(key = "live_views", title = "قسم مشاهدات البث المباشر", services = listOf(
            LocalService(id = 1, name = "مشاهدات بث مباشر 1k", price = 3.0),
            LocalService(id = 2, name = "مشاهدات بث مباشر 2k", price = 6.0),
            LocalService(id = 3, name = "مشاهدات بث مباشر 3k", price = 9.0),
            LocalService(id = 4, name = "مشاهدات بث مباشر 4k", price = 12.0),
            LocalService(id = 5, name = "مشاهدات بث مباشر 5k", price = 15.0)
        )),
        LocalSection(key = "pubg", title = "قسم شحن شدات ببجي", services = listOf(
            LocalService(id = 1, name = "ببجي 60 UC", price = 1.2),
            LocalService(id = 2, name = "ببجي 120 UC", price = 2.3),
            LocalService(id = 3, name = "ببجي 180 UC", price = 3.5),
            LocalService(id = 4, name = "ببجي 240 UC", price = 4.7),
            LocalService(id = 5, name = "ببجي 325 UC", price = 6.0),
            LocalService(id = 6, name = "ببجي 660 UC", price = 11.5),
            LocalService(id = 7, name = "ببجي 1800 UC", price = 30.0)
        )),
        LocalSection(key = "itunes", title = "قسم شراء رصيد ايتونز", services = listOf(
            LocalService(id = 1, name = "بطاقة iTunes $5", price = 4.9),
            LocalService(id = 2, name = "بطاقة iTunes $10", price = 9.7),
            LocalService(id = 3, name = "بطاقة iTunes $15", price = 14.4),
            LocalService(id = 4, name = "بطاقة iTunes $20", price = 19.0),
            LocalService(id = 5, name = "بطاقة iTunes $25", price = 23.7),
            LocalService(id = 6, name = "بطاقة iTunes $50", price = 47.0)
        )),
        LocalSection(key = "telegram", title = "قسم خدمات التليجرام", services = listOf(
            LocalService(id = 1, name = "أعضاء قناة 1k", price = 9.0),
            LocalService(id = 2, name = "أعضاء قناة 2k", price = 17.5),
            LocalService(id = 3, name = "أعضاء قناة 3k", price = 25.0),
            LocalService(id = 4, name = "أعضاء كروب 1k", price = 10.0),
            LocalService(id = 5, name = "أعضاء كروب 2k", price = 19.0)
        )),
        LocalSection(key = "ludo", title = "قسم خدمات اللودو", services = listOf(
            LocalService(id = 1, name = "لودو 100 ألماسة", price = 0.9),
            LocalService(id = 2, name = "لودو 200 ألماسة", price = 1.7),
            LocalService(id = 3, name = "لودو 500 ألماسة", price = 4.1),
            LocalService(id = 4, name = "لودو 1000 ألماسة", price = 8.0),
            LocalService(id = 5, name = "لودو 2000 ألماسة", price = 15.5)
        )),
        LocalSection(key = "mobile_recharge", title = "قسم شراء رصيد الهاتف", services = listOf(
            LocalService(id = 1, name = "شراء رصيد 2دولار اثير", price = 2.0),
            LocalService(id = 2, name = "شراء رصيد 5دولار اثير", price = 5.0),
            LocalService(id = 3, name = "شراء رصيد 10دولار اثير", price = 10.0),
            LocalService(id = 4, name = "شراء رصيد 20دولار اثير", price = 20.0),
            LocalService(id = 5, name = "شراء رصيد 40دولار اثير", price = 40.0),
            LocalService(id = 6, name = "شراء رصيد 2دولار اسيا", price = 2.0),
            LocalService(id = 7, name = "شراء رصيد 5دولار اسيا", price = 5.0),
            LocalService(id = 8, name = "شراء رصيد 10دولار اسيا", price = 10.0),
            LocalService(id = 9, name = "شراء رصيد 20دولار اسيا", price = 20.0),
            LocalService(id = 10, name = "شراء رصيد 40دولار اسيا", price = 40.0),
            LocalService(id = 11, name = "شراء رصيد 2دولار كورك", price = 2.0),
            LocalService(id = 12, name = "شراء رصيد 5دولار كورك", price = 5.0),
            LocalService(id = 13, name = "شراء رصيد 10دولار كورك", price = 10.0),
            LocalService(id = 14, name = "شراء رصيد 20دولار كورك", price = 20.0),
            LocalService(id = 15, name = "شراء رصيد 40دولار كورك", price = 40.0)
        ))
    )
}

/* ====================================================================== */
/*                           الحالة و الشاشات                             */
/* ====================================================================== */

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ratluzen_prefs", Context.MODE_PRIVATE)

    fun isAdmin(): Boolean = sp.getBoolean("admin_logged_in", false)
    fun setAdmin(logged: Boolean) { sp.edit().putBoolean("admin_logged_in", logged).apply() }

    fun getDeviceId(): String {
        val key = "device_id"
        val saved = sp.getString(key, null)
        if (saved != null) return saved
        val newId = "dev-" + UUID.randomUUID().toString()
        sp.edit().putString(key, newId).apply()
        return newId
    }
}

data class Order(val id: Int, val title: String, val price: Double)

sealed class Screen {
    object HOME : Screen()
    object SERVICES : Screen()
    data class SERVICE_LIST(val section: LocalSection) : Screen()
    object ORDERS : Screen()
    object BALANCE : Screen()
    object REFERRAL : Screen()
    object LEADERBOARD : Screen()
    object ADMIN_LOGIN : Screen()
    object ADMIN_DASHBOARD : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val snackbarHost = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                fun showSnack(msg: String) {
                    scope.launch { snackbarHost.showSnackbar(msg) }
                }

                Surface(Modifier.fillMaxSize()) {
                    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            MainApp(prefs = prefs, showSnack = ::showSnack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(prefs: Prefs, showSnack: (String) -> Unit) {
    var current by rememberSaveable {
        mutableStateOf<Screen>(if (prefs.isAdmin()) Screen.ADMIN_DASHBOARD else Screen.HOME)
    }
    val isAdmin = remember { mutableStateOf(prefs.isAdmin()) }
    val orders = remember { mutableStateListOf<Order>() }
    val deviceId = remember { prefs.getDeviceId() }

    when (val s = current) {
        Screen.HOME -> HomeScreen(
            onServices = { current = Screen.SERVICES },
            onOrders = { current = Screen.ORDERS },
            onBalance = { current = Screen.BALANCE },
            onReferral = { current = Screen.REFERRAL },
            onLeaderboard = { current = Screen.LEADERBOARD },
            onAdminClick = { current = Screen.ADMIN_LOGIN },
            showAdmin = !isAdmin.value
        )

        Screen.SERVICES -> ServicesScreen(
            sections = LocalCatalog.sections,
            onBack = { current = Screen.HOME },
            onOpenSection = { sec -> current = Screen.SERVICE_LIST(sec) }
        )

        is Screen.SERVICE_LIST -> SectionScreen(
            section = s.section,
            onBack = { current = Screen.SERVICES },
            onOrderClick = { service ->
                val newId = (orders.maxOfOrNull { it.id } ?: 0) + 1
                orders.add(Order(newId, service.name, service.price))
                showSnack("تم إنشاء طلب: ${service.name} - ${service.price}$")
            }
        )

        Screen.ORDERS -> OrdersScreen(
            orders = orders,
            onBack = { current = if (isAdmin.value) Screen.ADMIN_DASHBOARD else Screen.HOME }
        )

        Screen.BALANCE -> SimpleInfoScreen(
            title = "رصيدي",
            lines = listOf("المعرف: $deviceId", "هذه شاشة تجريبية لعرض الرصيد.", "يمكن ربطها بالخلفية لاحقًا."),
            onBack = { current = Screen.HOME }
        )

        Screen.REFERRAL -> SimpleInfoScreen(
            title = "نظام الإحالة",
            lines = listOf("شارك رابط الدعوة لربح عمولة عند أول تمويل.", "تجريبية الآن — اربطها بالخلفية لاحقًا."),
            onBack = { current = Screen.HOME }
        )

        Screen.LEADERBOARD -> SimpleInfoScreen(
            title = "المتصدرون 🎉",
            lines = listOf("أعلى المستخدمين إنفاقًا ستظهر هنا.", "تجريبية الآن — اربطها بالخلفية لاحقًا."),
            onBack = { current = Screen.HOME }
        )

        Screen.ADMIN_LOGIN -> AdminLoginScreen(
            onBack = { current = Screen.HOME },
            onSuccess = {
                prefs.setAdmin(true); isAdmin.value = true; current = Screen.ADMIN_DASHBOARD
                showSnack("تم تسجيل دخول المالك")
            },
            showSnack = showSnack
        )

        Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
            onBackToHome = {
                prefs.setAdmin(false); isAdmin.value = false; current = Screen.HOME
            },
            onOpen = { key ->
                // تنقل تجريبي — اربطه بشاشات فعلية لاحقًا
                showSnack("فتح: $key (تجريبي)")
            }
        )
    }
}

/* ============================== الشاشات ============================== */

@Composable
fun HomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaderboard: () -> Unit,
    onAdminClick: () -> Unit,
    showAdmin: Boolean
) {
    Box(Modifier.fillMaxSize()) {
        if (showAdmin) {
            OutlinedButton(
                onClick = onAdminClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .height(36.dp)
            ) { Text("دخول المالك") }
        }

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                MainButton("الخدمات", onServices)
                MainButton("طلباتي", onOrders)
                MainButton("رصيدي", onBalance)
                MainButton("الإحالة", onReferral)
                MainButton("المتصدرين 🎉", onLeaderboard)
            }
        }
    }
}

@Composable
fun MainButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(54.dp)
    ) { Text(text) }
}

@Composable
fun ServicesScreen(
    sections: List<LocalSection>,
    onBack: () -> Unit,
    onOpenSection: (LocalSection) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الأقسام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(sections) { sec ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(sec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${sec.services.size} خدمة")
                        }
                        Button(onClick = { onOpenSection(sec) }) { Text("فتح") }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionScreen(
    section: LocalSection,
    onBack: () -> Unit,
    onOrderClick: (LocalService) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(section.services) { svc ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(svc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("السعر: ${svc.price} $")
                            Button(onClick = { onOrderClick(svc) }) { Text("طلب الخدمة") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(orders: List<Order>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("طلباتي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))

        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد طلبات بعد") }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(orders) { o ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(o.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("السعر: ${o.price} $")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleInfoScreen(title: String, lines: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                lines.forEachIndexed { i, line ->
                    Text(line)
                    if (i != lines.lastIndex) Divider(Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
fun AdminLoginScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    showSnack: (String) -> Unit
) {
    var pass by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // بدون KeyboardOptions لتفادي أي أخطاء/عدم توافق
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("كلمة المرور") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onBack) { Text("رجوع") }
            Button(onClick = {
                if (pass == "2000") onSuccess()
                else showSnack("كلمة المرور غير صحيحة")
            }) { Text("دخول") }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    onBackToHome: () -> Unit,
    onOpen: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBackToHome) { Text("خروج المالك") }
        }
        Spacer(Modifier.height(12.dp))

        val buttons = listOf(
            "prices" to "تعديل الأسعار والكميات",
            "pending_orders" to "الطلبات المعلقة (الخدمات)",
            "pending_cards" to "الكارتات المعلقة",
            "pubg" to "طلبات شدات ببجي",
            "itunes" to "طلبات شحن الايتونز",
            "mobile_recharge" to "طلبات الأرصدة المعلقة",
            "ludo" to "طلبات لودو المعلقة",
            "balance_minus" to "خصم الرصيد",
            "balance_plus" to "إضافة رصيد",
            "api_status" to "فحص حالة طلب API",
            "api_balance" to "فحص رصيد API",
            "users_balance" to "رصيد المستخدمين",
            "users_count" to "عدد المستخدمين",
            "moderators" to "إدارة المشرفين",
            "unblock" to "إلغاء حظر المستخدم",
            "block" to "حظر المستخدم",
            "broadcast" to "إعلان التطبيق",
            "api_codes" to "أكواد خدمات API",
            "referrals" to "نظام الإحالة",
            "discounts" to "شرح الخصومات",
            "leaderboard" to "المتصدرين"
        )

        LazyColumn(Modifier.fillMaxSize()) {
            items(buttons) { (key, label) ->
                Button(
                    onClick = { onOpen(key) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(52.dp)
                ) { Text(label) }
            }
        }
    }
}
