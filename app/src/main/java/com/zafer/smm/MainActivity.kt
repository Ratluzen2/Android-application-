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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * كل استدعاءات @Composable داخل setContent/Compose فقط.
 * لا توجد أي تبعيات Navigation Compose.
 */
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
   Screens enum (بدون Navigation)
   ========================= */
private enum class Screen {
    USER_HOME, USER_ORDERS, USER_WALLET, USER_SUPPORT, OWNER_DASHBOARD
}

/* =========================
   Models & ViewModel
   ========================= */
data class Service(
    val id: String,
    val category: String,      // TikTok / Instagram / Telegram / PUBG
    val name: String,          // اسم الخدمة
    val basePrice: Double,     // السعر الأساسي
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
    // صلاحية المالك (للتجربة)
    private val _isOwner = MutableStateFlow(true)
    val isOwner: StateFlow<Boolean> = _isOwner

    // رصيد المستخدم
    private val _balance = MutableStateFlow(15.75)
    val balance: StateFlow<Double> = _balance

    // الخدمات (عينات قابلة للتعديل)
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

    // الطلبات (عينات)
    private val _orders = MutableStateFlow(
        listOf(
            Order("Z-10021", "متابعين تيكتوك 1k - سريع", 2000, 4.60, "مكتمل"),
            Order("Z-10022", "UC شدات 325", 1, 4.90, "قيد التنفيذ"),
            Order("Z-10023", "أعضاء قنوات 1k - حقيقي", 500, 3.0, "ملغي"),
        )
    )
    val orders: StateFlow<List<Order>> = _orders

    fun updateServicePrice(id: String, newBasePrice: Double) {
        _services.value = _services.value.map {
            if (it.id == id) it.copy(basePrice = newBasePrice) else it
        }
    }

    fun addBalance(amount: Double) {
        _balance.value = (_balance.value + amount).coerceAtLeast(0.0)
    }

    fun toggleOwner() { _isOwner.value = !_isOwner.value }
}

/* =========================
   App Root + Bottom Navigation (بدون NavHost)
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    var current by remember { mutableStateOf(Screen.USER_HOME) }
    val isOwner by viewModel.isOwner.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SMM App", fontWeight = FontWeight.SemiBold) },
                actions = {
                    // مفتاح تبديل وضع المالك أثناء التطوير
                    TextButton(onClick = { viewModel.toggleOwner() }) {
                        Text(if (isOwner) "مالك: تشغيل" else "مالك: إيقاف")
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                current = current,
                isOwner = isOwner,
                onSelect = { current = it }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (current) {
                Screen.USER_HOME       -> UserHomeScreen(viewModel)
                Screen.USER_ORDERS     -> UserOrdersScreen(viewModel)
                Screen.USER_WALLET     -> UserWalletScreen(viewModel)
                Screen.USER_SUPPORT    -> UserSupportScreen()
                Screen.OWNER_DASHBOARD -> OwnerDashboardScreen(viewModel)
            }
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
            add(Triple("الطلبات", Screen.USER_ORDERS, Icons.Filled.List))
            add(Triple("المحفظة", Screen.USER_WALLET, Icons.Filled.AccountCircle))
            add(Triple("الدعم", Screen.USER_SUPPORT, Icons.Filled.Help))
            if (isOwner) add(Triple("لوحة المالك", Screen.OWNER_DASHBOARD, Icons.Filled.Dashboard))
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

        // بحث
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث عن خدمة") },
            modifier = Modifier.fillMaxWidth()
        )

        // فلاتر تصنيف
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
                onClick = { /* إعدادات متقدمة لاحقاً */ },
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
                        "تلميح: هذا هو السعر الأساسي الذي تُحسب عليه حزم 1k/10k… أو القطع في PUBG.",
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
