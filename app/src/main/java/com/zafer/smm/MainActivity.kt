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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MainActivity
 * - يطلق شجرة Compose فقط داخل setContent
 * - لا يوجد أي استدعاء @Composable خارج السياق الصحيح
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

/* =========================
   Theme (يمكن استبداله بثيم مشروعك)
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
   Navigation Destinations
   ========================= */
private object Routes {
    const val USER_HOME = "user_home"
    const val USER_ORDERS = "user_orders"
    const val USER_WALLET = "user_wallet"
    const val USER_SUPPORT = "user_support"
    const val OWNER_DASHBOARD = "owner_dashboard"
}

/* =========================
   ViewModel و نماذج البيانات
   ========================= */
data class Service(
    val id: String,
    val category: String,      // TikTok / Instagram / Telegram / PUBG
    val name: String,          // اسم الخدمة الظاهر للمستخدم
    val basePrice: Double,     // سعر 1000 وحدة مثلاً أو السعر الأساسي
    val minQty: Int,
    val maxQty: Int,
    val step: Int = 100,
)

data class Order(
    val orderId: String,
    val serviceName: String,
    val qty: Int,
    val price: Double,
    val status: String
)

class AppViewModel : ViewModel() {
    // صلاحية المالك (يمكنك ربطها بتوثيق/إعداد حقيقي لاحقاً)
    private val _isOwner = MutableStateFlow(true)
    val isOwner: StateFlow<Boolean> = _isOwner

    // رصيد المستخدم
    private val _balance = MutableStateFlow(15.75)
    val balance: StateFlow<Double> = _balance

    // قائمة الخدمات (موسّعة ويمكن تعديلها حياً)
    private val _services = MutableStateFlow(
        listOf(
            Service("ttk_1", "TikTok", "متابعين تيكتوك 1k - سريع", 2.30, minQty = 100, maxQty = 100000, step = 100),
            Service("ttk_2", "TikTok", "مشاهدات تيكتوك 10k", 0.90, minQty = 1000, maxQty = 1000000, step = 1000),
            Service("ig_1", "Instagram", "متابعين انستغرام 1k - ثابت", 3.20, minQty = 100, maxQty = 100000, step = 100),
            Service("ig_2", "Instagram", "لايكات انستغرام 1k", 1.10, minQty = 100, maxQty = 500000, step = 100),
            Service("tg_1", "Telegram", "أعضاء قنوات 1k - حقيقي", 6.0, minQty = 100, maxQty = 200000, step = 100),
            Service("tg_2", "Telegram", "مشاهدات تيليجرام 10k", 0.45, minQty = 1000, maxQty = 2000000, step = 1000),
            Service("pubg_1", "PUBG", "UC شدات 60", 1.05, minQty = 1, maxQty = 100, step = 1),
            Service("pubg_2", "PUBG", "UC شدات 325", 4.90, minQty = 1, maxQty = 100, step = 1),
        )
    )
    val services: StateFlow<List<Service>> = _services

    // الطلبات (عينة)
    private val _orders = MutableStateFlow(
        listOf(
            Order("Z-10021", "متابعين تيكتوك 1k - سريع", 2000, 4.60, "مكتمل"),
            Order("Z-10022", "UC شدات 325", 1, 4.90, "قيد التنفيذ"),
            Order("Z-10023", "أعضاء قنوات 1k - حقيقي", 500, 3.0, "ملغي"),
        )
    )
    val orders: StateFlow<List<Order>> = _orders

    // تحديث السعر الأساسي لخدمة محددة (انعكاس فوري في الواجهة)
    fun updateServicePrice(id: String, newBasePrice: Double) {
        _services.value = _services.value.map {
            if (it.id == id) it.copy(basePrice = newBasePrice) else it
        }
    }

    // مثال: تعبئة رصيد
    fun addBalance(amount: Double) {
        _balance.value = (_balance.value + amount).coerceAtLeast(0.0)
    }

    // تبديل صلاحية المالك (لتجربة ظهور تبويب المالك)
    fun toggleOwner() { _isOwner.value = !_isOwner.value }
}

/* =========================
   App Root with Bottom Navigation
   ========================= */
@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val isOwner by viewModel.isOwner.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SMM App", fontWeight = FontWeight.SemiBold) },
                actions = {
                    // زر بسيط لتفعيل/إلغاء وضع المالك أثناء التطوير
                    TextButton(onClick = { viewModel.toggleOwner() }) {
                        Text(if (isOwner) "مالك: تشغيل" else "مالك: إيقاف")
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(navController = navController, isOwner = isOwner)
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AppNavHost(navController = navController, viewModel = viewModel)
        }
    }
}

/* =========================
   Bottom Bar
   ========================= */
@Composable
fun BottomBar(navController: NavHostController, isOwner: Boolean) {
    val items = remember(isOwner) {
        buildList {
            add(BottomItem("الرئيسية", Routes.USER_HOME, Icons.Filled.Home))
            add(BottomItem("الطلبات", Routes.USER_ORDERS, Icons.Filled.List))
            add(BottomItem("المحفظة", Routes.USER_WALLET, Icons.Filled.Payments))
            add(BottomItem("الدعم", Routes.USER_SUPPORT, Icons.Filled.Help))
            if (isOwner) add(BottomItem("لوحة المالك", Routes.OWNER_DASHBOARD, Icons.Filled.Dashboard))
        }
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
            )
        }
    }
}

data class BottomItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/* =========================
   NavHost
   ========================= */
@Composable
fun AppNavHost(navController: NavHostController, viewModel: AppViewModel) {
    NavHost(navController = navController, startDestination = Routes.USER_HOME) {

        composable(Routes.USER_HOME) {
            UserHomeScreen(viewModel)
        }

        composable(Routes.USER_ORDERS) {
            UserOrdersScreen(viewModel)
        }

        composable(Routes.USER_WALLET) {
            UserWalletScreen(viewModel)
        }

        composable(Routes.USER_SUPPORT) {
            UserSupportScreen()
        }

        composable(Routes.OWNER_DASHBOARD) {
            OwnerDashboardScreen(viewModel)
        }
    }
}

/* =========================
   User Screens
   ========================= */

@Composable
fun UserHomeScreen(viewModel: AppViewModel) {
    val services by viewModel.services.collectAsState()
    var query by remember { mutableStateOf("") }
    val categories = remember(services) { services.map { it.category }.toSet().toList() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPriceEditor by remember { mutableStateOf<Service?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("الخدمات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // بحث بسيط
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث عن خدمة") },
            modifier = Modifier.fillMaxWidth()
        )

        // فلاتر تصنيفات
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { selectedCategory = null },
                label = { Text("الكل") },
                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) }
            )
            categories.forEach { cat ->
                AssistChip(
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val filtered = services.filter {
            (selectedCategory == null || it.category == selectedCategory) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { svc ->
                ServiceCard(
                    service = svc,
                    onEditPrice = { showPriceEditor = svc }
                )
            }
        }
    }

    if (showPriceEditor != null) {
        EditPriceDialog(
            service = showPriceEditor!!,
            onDismiss = { showPriceEditor = null },
            onSave = { id, newPrice ->
                viewModel.updateServicePrice(id, newPrice)
                showPriceEditor = null
            }
        )
    }
}

@Composable
fun ServiceCard(service: Service, onEditPrice: (Service) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(service.category, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(service.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        Text("السعر الأساسي: ${service.basePrice} $", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("الكمية: من ${service.minQty} إلى ${service.maxQty} (خطوة ${service.step})", fontSize = 12.sp)

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedButton(
                onClick = { /* تنفيذ طلب شراء */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("شراء")
            }
            OutlinedButton(
                onClick = { onEditPrice(service) },
                modifier = Modifier.weight(1f)
            ) {
                Text("تعديل السعر")
            }
        }
    }
}

@Composable
fun EditPriceDialog(
    service: Service,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var priceText by remember { mutableStateOf(service.basePrice.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                priceText.toDoubleOrNull()?.let { onSave(service.id, it) }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("تعديل سعر الخدمة") },
        text = {
            Column {
                Text(service.name, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر الأساسي بالدولار") }
                )
            }
        }
    )
}

@Composable
fun UserOrdersScreen(viewModel: AppViewModel) {
    val orders by viewModel.orders.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("طلباتي", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orders, key = { it.orderId }) { o ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text("رقم الطلب: ${o.orderId}", fontWeight = FontWeight.SemiBold)
                    Text("الخدمة: ${o.serviceName}")
                    Text("الكمية: ${o.qty}")
                    Text("السعر: ${o.price} $")
                    Text("الحالة: ${o.status}", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun UserWalletScreen(viewModel: AppViewModel) {
    val balance by viewModel.balance.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

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
                    OutlinedButton(onClick = { /* سحب */ }) { Text("سحب") }
                }
            }
        }
    }

    if (showAdd) {
        var amount by remember { mutableStateOf("5.00") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("إضافة رصيد") },
            text = {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("القيمة بالدولار") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toDoubleOrNull()?.let {
                        viewModel.addBalance(it)
                        showAdd = false
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun UserSupportScreen() {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "تواصل معنا عبر التليجرام أو البريد.\n" +
            "- تيليجرام: @your_channel\n- البريد: support@example.com"
        )
    }
}

/* =========================
   Owner Dashboard
   ========================= */
@Composable
fun OwnerDashboardScreen(viewModel: AppViewModel) {
    val services by viewModel.services.collectAsState()
    var expanded by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("لوحة تحكم المالك", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.Dashboard, contentDescription = null)
        }
        Spacer(Modifier.height(8.dp))

        Text(
            "إدارة الخدمات والأسعار والكميات بشكل مباشر.\n" +
            "أي تعديل ينعكس فوراً على واجهة المستخدم.",
            fontSize = 12.sp
        )

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الخدمات (${services.size})", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (expanded) "إخفاء" else "إظهار",
                        modifier = Modifier.clickable { expanded = !expanded },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(services, key = { it.id }) { svc ->
                            OwnerServiceRow(svc, onChangePrice = { newPrice ->
                                viewModel.updateServicePrice(svc.id, newPrice)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerServiceRow(service: Service, onChangePrice: (Double) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf(service.basePrice.toString()) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text("${service.category} • ${service.name}", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("السعر: ${service.basePrice} $")
        Text("الكمية: ${service.minQty}..${service.maxQty} (خطوة ${service.step})", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { editing = true }) {
                Text("تعديل السعر")
            }
            AssistChip(
                onClick = { /* فتح مزيد من الإعدادات لاحقاً */ },
                label = { Text("إعدادات متقدمة") },
                leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
            )
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("تعديل سعر: ${service.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("السعر بالدولار") }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "تلميح: السعر هنا هو الأساس الذي يحسب عليه التطبيق حِزم 1k/10k… أو القطع في PUBG.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    priceText.toDoubleOrNull()?.let {
                        onChangePrice(it)
                        editing = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("إلغاء") }
            }
        )
    }
}
