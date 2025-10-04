package com.zafer.smm

import android.content.Context
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* ==============================
   1) إعدادات واجهة/مفاتيح (ظاهرة كما طلبت)
   ============================== */
object ApiConfig {
    const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com/"
    const val API_KEY = "SAMPLE_VISIBLE_KEY_12345"
}

/* ==============================
   2) نماذج البيانات + كتالوج محلي
   ============================== */
data class LocalService(val id: Int, val name: String, val price: Double)
data class LocalSection(val key: String, val title: String, val services: List<LocalService>)

object LocalCatalog {
    val sections: List<LocalSection> = listOf(
        LocalSection(
            key = "followers", title = "قسم المتابعين",
            services = listOf(
                LocalService(1, "متابعين تيكتوك 100", 1.0),
                LocalService(2, "متابعين تيكتوك 200", 2.0),
                LocalService(3, "متابعين تيكتوك 300", 3.0),
                LocalService(4, "متابعين تيكتوك 400", 4.0),
                LocalService(5, "متابعين تيكتوك 500", 5.0),
                LocalService(6, "متابعين تيكتوك 1000", 9.0),
                LocalService(7, "متابعين تيكتوك 2000", 18.0),
                LocalService(8, "متابعين تيكتوك 3000", 27.0),
                LocalService(9, "متابعين تيكتوك 4000", 36.0),
                LocalService(10, "متابعين تيكتوك 5000", 45.0)
            )
        ),
        LocalSection(
            key = "likes", title = "قسم الإعجابات",
            services = listOf(
                LocalService(1, "لايكات 1k", 2.5),
                LocalService(2, "لايكات 2k", 5.0),
                LocalService(3, "لايكات 3k", 7.5),
                LocalService(4, "لايكات 4k", 10.0),
                LocalService(5, "لايكات 5k", 12.5),
            )
        ),
        LocalSection(
            key = "views", title = "قسم المشاهدات",
            services = listOf(
                LocalService(1, "مشاهدات تيكتوك 1k", 0.5),
                LocalService(2, "مشاهدات تيكتوك 2k", 1.0),
                LocalService(3, "مشاهدات تيكتوك 3k", 1.5),
                LocalService(4, "مشاهدات تيكتوك 4k", 2.0),
                LocalService(5, "مشاهدات تيكتوك 5k", 2.5),
                LocalService(6, "مشاهدات تيكتوك 10k", 4.5),
            )
        ),
        LocalSection(
            key = "live_views", title = "قسم مشاهدات البث المباشر",
            services = listOf(
                LocalService(1, "مشاهدات بث مباشر 1k", 3.0),
                LocalService(2, "مشاهدات بث مباشر 2k", 6.0),
                LocalService(3, "مشاهدات بث مباشر 3k", 9.0),
                LocalService(4, "مشاهدات بث مباشر 4k", 12.0),
                LocalService(5, "مشاهدات بث مباشر 5k", 15.0),
            )
        ),
        LocalSection(
            key = "pubg", title = "قسم شحن شدات ببجي",
            services = listOf(
                LocalService(1, "ببجي 60 UC", 1.2),
                LocalService(2, "ببجي 120 UC", 2.3),
                LocalService(3, "ببجي 180 UC", 3.5),
                LocalService(4, "ببجي 240 UC", 4.7),
                LocalService(5, "ببجي 325 UC", 6.0),
                LocalService(6, "ببجي 660 UC", 11.5),
                LocalService(7, "ببجي 1800 UC", 30.0),
            )
        ),
        LocalSection(
            key = "itunes", title = "قسم شراء رصيد ايتونز",
            services = listOf(
                LocalService(1, "بطاقة iTunes \$5", 4.9),
                LocalService(2, "بطاقة iTunes \$10", 9.7),
                LocalService(3, "بطاقة iTunes \$15", 14.4),
                LocalService(4, "بطاقة iTunes \$20", 19.0),
                LocalService(5, "بطاقة iTunes \$25", 23.7),
                LocalService(6, "بطاقة iTunes \$50", 47.0),
            )
        ),
        LocalSection(
            key = "telegram", title = "قسم خدمات التليجرام",
            services = listOf(
                LocalService(1, "أعضاء قناة 1k", 9.0),
                LocalService(2, "أعضاء قناة 2k", 17.5),
                LocalService(3, "أعضاء قناة 3k", 25.0),
                LocalService(4, "أعضاء كروب 1k", 10.0),
                LocalService(5, "أعضاء كروب 2k", 19.0),
            )
        ),
        LocalSection(
            key = "ludo", title = "قسم خدمات اللودو",
            services = listOf(
                LocalService(1, "لودو 100 ألماسة", 0.9),
                LocalService(2, "لودو 200 ألماسة", 1.7),
                LocalService(3, "لودو 500 ألماسة", 4.1),
                LocalService(4, "لودو 1000 ألماسة", 8.0),
                LocalService(5, "لودو 2000 ألماسة", 15.5),
            )
        ),
        LocalSection(
            key = "mobile_recharge", title = "قسم شراء رصيد الهاتف",
            services = listOf(
                LocalService(1, "شراء رصيد 2 دولار أثير", 2.0),
                LocalService(2, "شراء رصيد 5 دولار أثير", 5.0),
                LocalService(3, "شراء رصيد 10 دولار أثير", 10.0),
                LocalService(4, "شراء رصيد 20 دولار أثير", 20.0),
                LocalService(5, "شراء رصيد 40 دولار أثير", 40.0),
                LocalService(6, "شراء رصيد 2 دولار آسيا", 2.0),
                LocalService(7, "شراء رصيد 5 دولار آسيا", 5.0),
                LocalService(8, "شراء رصيد 10 دولار آسيا", 10.0),
                LocalService(9, "شراء رصيد 20 دولار آسيا", 20.0),
                LocalService(10, "شراء رصيد 40 دولار آسيا", 40.0),
                LocalService(11, "شراء رصيد 2 دولار كورك", 2.0),
                LocalService(12, "شراء رصيد 5 دولار كورك", 5.0),
                LocalService(13, "شراء رصيد 10 دولار كورك", 10.0),
                LocalService(14, "شراء رصيد 20 دولار كورك", 20.0),
                LocalService(15, "شراء رصيد 40 دولار كورك", 40.0),
            )
        ),
    )
}

/* ==============================
   3) تفضيلات بسيطة (تخزين حالة المالك + الرصيد)
   ============================== */
object Prefs {
    private const val FILE = "app_prefs"
    private const val KEY_OWNER = "owner_logged_in"
    private const val KEY_BALANCE = "user_balance"

    fun isOwner(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_OWNER, false)

    fun setOwner(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OWNER, v).apply()
    }

    fun getBalance(ctx: Context): Double =
        java.lang.Double.longBitsToDouble(
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getLong(KEY_BALANCE, java.lang.Double.doubleToRawLongBits(0.0))
        )

    fun setBalance(ctx: Context, value: Double) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong(KEY_BALANCE, java.lang.Double.doubleToRawLongBits(value))
            .apply()
    }
}

/* ==============================
   4) شاشة واحدة لكل شيء
   ============================== */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        setContent {
            val colorScheme = lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                val snackHost = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // حالة التنقّل البسيطة
                enum class Route { HOME, SERVICES, SERVICE_LIST, ORDERS, BALANCE, REFERRAL, LEADERBOARD, ADMIN_LOGIN, ADMIN_DASHBOARD }

                var current by rememberSaveable { mutableStateOf(Route.HOME) }
                var owner by rememberSaveable { mutableStateOf(Prefs.isOwner(this)) }
                var selectedSectionKey by rememberSaveable { mutableStateOf<String?>(null) }

                // رصيد المستخدم من التفضيلات
                var balance by rememberSaveable { mutableStateOf(Prefs.getBalance(this)) }

                // طلبات محفوظة بالذاكرة مؤقتًا
                data class Order(val title: String, val price: Double)
                val orders = remember { mutableStateListOf<Order>() }

                fun showSnack(msg: String) = scope.launch { snackHost.showSnackbar(msg) }

                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = { Text(text = "خدمات راتلوزن", fontSize = 18.sp) },
                            actions = {
                                // زر دخول المالك (أعلى اليمين وبحجم أصغر)
                                if (!owner) {
                                    TextButton(onClick = { current = Route.ADMIN_LOGIN }) {
                                        Text("دخول المالك")
                                    }
                                } else {
                                    TextButton(onClick = {
                                        current = Route.ADMIN_DASHBOARD
                                    }) { Text("لوحة المالك") }
                                }
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackHost) }
                ) { inner ->
                    Box(Modifier.fillMaxSize().padding(inner)) {
                        when (current) {
                            Route.HOME -> HomeScreen(
                                baseUrl = ApiConfig.BASE_URL,
                                apiKey = ApiConfig.API_KEY,
                                deviceId = deviceId,
                                onServices = { current = Route.SERVICES },
                                onOrders = { current = Route.ORDERS },
                                onBalance = { current = Route.BALANCE },
                                onReferral = { current = Route.REFERRAL },
                                onLeaderboard = { current = Route.LEADERBOARD },
                                onOwnerLogin = { current = Route.ADMIN_LOGIN },
                                owner = owner
                            )

                            Route.SERVICES -> ServicesScreen(
                                onBack = { current = Route.HOME },
                                onOpenSection = { key ->
                                    selectedSectionKey = key
                                    current = Route.SERVICE_LIST
                                }
                            )

                            Route.SERVICE_LIST -> {
                                val section = LocalCatalog.sections.firstOrNull { it.key == selectedSectionKey }
                                if (section == null) {
                                    Text("لا توجد بيانات لهذا القسم", modifier = Modifier.align(Alignment.Center))
                                } else {
                                    ServiceListScreen(
                                        section = section,
                                        onBack = { current = Route.SERVICES },
                                        onOrderClick = { svc ->
                                            if (balance >= svc.price) {
                                                balance -= svc.price
                                                Prefs.setBalance(this@MainActivity, balance)
                                                orders.add(Order(svc.name, svc.price))
                                                showSnack("تم إنشاء الطلب: ${svc.name}")
                                            } else {
                                                showSnack("رصيد غير كافٍ")
                                            }
                                        }
                                    )
                                }
                            }

                            Route.ORDERS -> OrdersScreen(
                                orders = orders,
                                onBack = { current = Route.HOME }
                            )

                            Route.BALANCE -> BalanceScreen(
                                balance = balance,
                                onAdd = {
                                    balance += 5.0
                                    Prefs.setBalance(this@MainActivity, balance)
                                    showSnack("تمت إضافة 5.0$")
                                },
                                onDeduct = {
                                    if (balance >= 1.0) {
                                        balance -= 1.0
                                        Prefs.setBalance(this@MainActivity, balance)
                                        showSnack("تم خصم 1.0$")
                                    } else showSnack("رصيدك لا يسمح")
                                },
                                onBack = { current = Route.HOME }
                            )

                            Route.REFERRAL -> ReferralScreen(
                                deviceId = deviceId,
                                onBack = { current = Route.HOME }
                            )

                            Route.LEADERBOARD -> LeaderboardScreen(
                                onBack = { current = Route.HOME }
                            )

                            Route.ADMIN_LOGIN -> OwnerLoginScreen(
                                onBack = { current = Route.HOME },
                                onLogin = { pass ->
                                    if (pass == "2000") {
                                        owner = true
                                        Prefs.setOwner(this@MainActivity, true)
                                        current = Route.ADMIN_DASHBOARD
                                        showSnack("تم تسجيل دخول المالك")
                                    } else {
                                        showSnack("كلمة مرور غير صحيحة")
                                    }
                                }
                            )

                            Route.ADMIN_DASHBOARD -> AdminDashboardScreen(
                                onBack = { current = Route.HOME },
                                onAction = { title ->
                                    showSnack("قريباً: $title")
                                },
                                onLogout = {
                                    owner = false
                                    Prefs.setOwner(this@MainActivity, false)
                                    current = Route.HOME
                                    showSnack("تم تسجيل الخروج من لوحة المالك")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==============================
   5) الشاشات (كلها في هذا الملف)
   ============================== */

@Composable
fun HomeScreen(
    baseUrl: String,
    apiKey: String,
    deviceId: String,
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaderboard: () -> Unit,
    onOwnerLogin: () -> Unit,
    owner: Boolean
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "أهلاً وسهلاً بكم في تطبيق خدمات راتلوزن",
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        // معلومات صغيرة للمطوّر
        Text("Backend: $baseUrl", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text("API KEY (ظاهرة للتجربة): $apiKey", fontSize = 12.sp)
        Text("Device ID: $deviceId", fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        // شبكة أزرار رئيسية
        ButtonRow(
            listOf(
                "الخدمات" to onServices,
                "طلباتي" to onOrders,
                "رصيدي" to onBalance
            )
        )
        Spacer(Modifier.height(8.dp))
        ButtonRow(
            listOf(
                "الإحالة" to onReferral,
                "المتصدرين 🎉" to onLeaderboard
            )
        )
        Spacer(Modifier.height(16.dp))
        // زر دخول المالك (مكرر هنا اختصاراً)
        if (!owner) {
            OutlinedButton(onClick = onOwnerLogin) { Text("دخول المالك") }
        } else {
            Text(
                "وضع المالك مُفعّل (من شريط الأعلى يمكنك فتح لوحة المالك).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ButtonRow(items: List<Pair<String, () -> Unit>>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        items.forEach { (label, action) ->
            Button(onClick = action, modifier = Modifier.weight(1f)) { Text(label) }
        }
    }
}

@Composable
fun ServicesScreen(onBack: () -> Unit, onOpenSection: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("الأقسام", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LocalCatalog.sections) { section ->
                ElevatedCard(
                    onClick = { onOpenSection(section.key) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(section.title, fontSize = 16.sp)
                        Text("عدد الخدمات: ${section.services.size}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceListScreen(
    section: LocalSection,
    onBack: () -> Unit,
    onOrderClick: (LocalService) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text(section.title, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(section.services) { svc ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(svc.name, fontSize = 16.sp)
                            Text("السعر: ${svc.price} $", fontSize = 12.sp)
                        }
                        Button(onClick = { onOrderClick(svc) }) {
                            Text("طلب الخدمة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(orders: List<Any>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("طلباتي", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Text("لا توجد طلبات بعد.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { any ->
                    val o = any as? (Any) ?: any
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            val order = any as? (com.zafer.smm.MainActivity).Nothing? // placeholder to keep file single; ignore
                        }
                    }
                }
            }
        }
    }
}

/* نسخة مبسطة لعرض الطلبات الفعلية المخزّنة */
@Composable
fun OrdersScreen(orders: List<MainActivity.Order>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("طلباتي", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Text("لا توجد طلبات بعد.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { o ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(o.title, fontSize = 16.sp)
                            Text("السعر: ${o.price} $", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceScreen(balance: Double, onAdd: () -> Unit, onDeduct: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("رصيدي", fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text("${"%.2f".format(balance)} $", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAdd) { Text("إضافة 5\$ (تجريبي)") }
            OutlinedButton(onClick = onDeduct) { Text("خصم 1\$") }
        }
    }
}

@Composable
fun ReferralScreen(deviceId: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("نظام الإحالة", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("رابط/كود دعوتك (مثال):")
        Text("RATL-INV-$deviceId", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("عند أول تمويل لمدعوّك تُضاف عمولة ثابتة لحسابك (منطق كامل لاحقاً).")
    }
}

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("المتصدرين 🎉", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        // عرض تجريبي
        val demo = listOf(
            "المستخدم 1 — إنفاق 120$",
            "المستخدم 2 — إنفاق 95$",
            "المستخدم 3 — إنفاق 80$"
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(demo) { row ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Text(row, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
fun OwnerLoginScreen(onBack: () -> Unit, onLogin: (String) -> Unit) {
    var pass by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("رجوع") }
        Spacer(Modifier.height(8.dp))
        Text("تسجيل دخول المالك", fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("كلمة المرور (2000)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onLogin(pass) }, modifier = Modifier.fillMaxWidth()) {
            Text("دخول")
        }
    }
}

@Composable
fun AdminDashboardScreen(onBack: () -> Unit, onAction: (String) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("رجوع") }
            TextButton(onClick = onLogout) { Text("خروج المالك") }
        }
        Spacer(Modifier.height(8.dp))
        Text("لوحة تحكم المالك", fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        // شبكة أزرار مرتبة
        fun row(vararg items: String) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { title ->
                    ElevatedButton(
                        onClick = { onAction(title) },
                        modifier = Modifier.weight(1f)
                    ) { Text(title, textAlign = TextAlign.Center) }
                }
            }
        }

        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            row("تعديل الاسعار والكميات", "الطلبات المعلّقة (الخدمات)", "الكارتات المعلّقة")
            row("طلبات شدات ببجي", "طلبات شحن الايتونز", "طلبات الأرصدة المعلّقة")
            row("طلبات لودو المعلّقة", "خصم الرصيد", "إضافة رصيد")
            row("فحص حالة طلب API", "فحص رصيد API", "رصيد المستخدمين")
            row("عدد المستخدمين", "إدارة المشرفين", "إلغاء حظر المستخدم")
            row("حظر المستخدم", "إعلان التطبيق", "أكواد خدمات API")
            row("نظام الإحالة", "شرح الخصومات", "المتصدرين")
        }
    }
}
