package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.Prefs
import com.zafer.smm.data.model.LocalMappedService
import com.zafer.smm.data.model.OrderItem
import com.zafer.smm.data.remote.ApiService
import com.zafer.smm.ui.MainViewModel
import com.zafer.smm.util.DeviceIdProvider
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        // خزّن Android ID عند أول تشغيل
        if (Prefs.getDeviceId() == null) {
            val id = DeviceIdProvider.getAndroidId(this)
            Prefs.setDeviceId(id)
            vm.attachDevice(id)
        }

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot(vm)
                }
            }
        }
    }
}

@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("الخدمات", "طلباتي", "محفظتي", "المتصدرون", "المالك")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> ServicesTab(vm)
            1 -> OrdersTab(vm)
            2 -> WalletTab(vm)
            3 -> LeaderboardTab(vm)
            4 -> AdminTab(vm)
        }
    }
}

/*------------------------ تبويب: الخدمات/الطلب ------------------------*/
@Composable
fun ServicesTab(vm: MainViewModel) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val services by vm.services.collectAsState()
    val lastOrderId by vm.lastOrderId.collectAsState()
    val lastStatus by vm.lastStatus.collectAsState()

    var serviceIdText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("https://example.com") }
    var quantityText by remember { mutableStateOf("1000") }
    var search by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Backend: ${ApiService.BASE_URL} | KEY: ${ApiService.API_KEY.take(6)}********", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshServices() }) { Text("تحديث") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.fetchProviderBalance() }) { Text("رصيد المزود") }
            Spacer(Modifier.width(8.dp))
            Button(enabled = lastOrderId != null, onClick = { vm.checkOrderStatus() }) { Text("حالة آخر طلب") }
        }

        if (loading) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) { Spacer(Modifier.height(8.dp)); Text("خطأ: $error", color = MaterialTheme.colorScheme.error) }
        if (info != null) { Spacer(Modifier.height(8.dp)); Text(info!!, color = MaterialTheme.colorScheme.primary) }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = serviceIdText, onValueChange = { serviceIdText = it },
            label = { Text("Service ID") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = quantityText, onValueChange = { quantityText = it },
            label = { Text("Quantity") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val sid = serviceIdText.toIntOrNull()
            val qty = quantityText.toIntOrNull()
            info = null
            if (sid != null && qty != null && link.isNotBlank()) vm.placeOrder(sid, link, qty)
            else info = "تحقق من Service ID و Quantity والـ Link."
        }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء طلب") }

        Spacer(Modifier.height(10.dp))
        if (lastOrderId != null) Text("آخر Order ID: $lastOrderId")
        if (lastStatus != null) {
            val st = lastStatus!!
            Text("الحالة: ${st.status ?: "-"} | المتبقي: ${st.remains ?: "-"} | التكلفة: ${st.charge ?: "-"}")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("بحث عن خدمة…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val filtered = remember(services, search) { if (search.isBlank()) services else services.filter { it.displayName.contains(search, true) } }
        LazyColumn(Modifier.weight(1f)) {
            items(filtered) { s ->
                ServiceRow(s) {
                    if (s.serviceId == null) info = "خدمة عرض/يدوية (لا يوجد service_id)."
                    else {
                        serviceIdText = s.serviceId.toString()
                        quantityText = (s.quantityMultiplier ?: 1000).toString()
                        info = "تم تعبئة Service ID و Quantity."
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(item: LocalMappedService, onPick: (LocalMappedService) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPick(item) }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("السعر: ${item.priceUsd ?: "-"} USD")
            Text("ID: ${item.serviceId?.toString() ?: "—"} | الكمية النموذجية: ${item.quantityMultiplier?.toString() ?: "—"}")
        }
    }
}

/*------------------------ تبويب: طلباتي ------------------------*/
@Composable
fun OrdersTab(vm: MainViewModel) {
    val orders by vm.orders.collectAsState()
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("طلباتي (${orders.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(orders) { o -> OrderRow(o, fmt) }
        }
    }
}

@Composable
private fun OrderRow(o: OrderItem, fmt: SimpleDateFormat) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("${o.serviceName} (ID=${o.serviceId ?: "—"})")
            Text("الكمية: ${o.quantity} | السعر: ${"%.2f".format(o.price)} USD")
            Text("OrderId: ${o.orderId ?: "—"} | الحالة: ${o.status ?: "-"}")
            Text("link: ${o.link}")
            Text("التاريخ: ${fmt.format(Date(o.createdAt))}")
        }
    }
}

/*------------------------ تبويب: محفظتي ------------------------*/
@Composable
fun WalletTab(vm: MainViewModel) {
    val wallet by vm.wallet.collectAsState()
    val provider by vm.balanceProvider.collectAsState()
    val deviceId by vm.deviceId.collectAsState()

    var addAmount by remember { mutableStateOf("5.0") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("محفظتي", style = MaterialTheme.typography.titleMedium)
        Text("معرف الجهاز: $deviceId", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Text("الرصيد الحالي: ${"%.2f".format(wallet)} USD")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = addAmount, onValueChange = { addAmount = it },
            label = { Text("إضافة رصيد (تجريبية محليًا)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = { vm.addFunds(addAmount.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth()) {
            Text("إضافة للمحفظة (محلياً)")
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.fetchProviderBalance() }, modifier = Modifier.fillMaxWidth()) {
            Text("عرض رصيد مزود الخدمة (kd1s)")
        }
        Spacer(Modifier.height(6.dp))
        if (provider != null) {
            Text("رصيد المزود: ${provider?.balance ?: "-"} ${provider?.currency ?: ""}")
        }
    }
}

/*------------------------ تبويب: المتصدرون ------------------------*/
@Composable
fun LeaderboardTab(vm: MainViewModel) {
    // حالياً محلي فقط (بدون سيرفر): متصدر الجهاز ذاته بناءً على مجموع ما دُفع
    val orders by vm.orders.collectAsState()
    val totalSpent = remember(orders) { orders.sumOf { it.price } }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("المتصدرون (محلي)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("أنت: مجموع إنفاق ${"%.2f".format(totalSpent)} USD")
        Spacer(Modifier.height(8.dp))
        Text("**ملاحظة:** المتصدرون الحقيقيون لجميع المستخدمين يتطلبون سيرفر مشترك.", color = MaterialTheme.colorScheme.secondary)
    }
}

/*------------------------ تبويب: لوحة المالك ------------------------*/
@Composable
fun AdminTab(vm: MainViewModel) {
    val isAdmin by vm.isAdmin.collectAsState()
    val wallet by vm.wallet.collectAsState()
    val deviceId by vm.deviceId.collectAsState()

    var pin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var setWallet by remember { mutableStateOf(wallet.toString()) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("لوحة المالك", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (!isAdmin) {
            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN") }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { vm.toggleAdminWithPin(pin) }) { Text("تفعيل وضع المالك") }
        } else {
            Text("✅ وضع المالك مفعّل")
            Spacer(Modifier.height(8.dp))
            Text("Device ID: $deviceId", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = setWallet, onValueChange = { setWallet = it },
                label = { Text("تعديل رصيد هذا الجهاز") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                val v = setWallet.toDoubleOrNull()
                if (v != null) {
                    Prefs.setWallet(v)
                    info = "تم ضبط رصيد الجهاز على ${"%.2f".format(v)}"
                } else info = "قيمة غير صالحة"
            }) { Text("حفظ الرصيد") }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("تغيير PIN المالك") }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                if (newPin.length >= 4) { vm.changeOwnerPin(newPin); info = "تم تغيير PIN" }
                else info = "الـPIN يجب أن يكون 4 أرقام أو أكثر"
            }) { Text("تغيير PIN") }

            Spacer(Modifier.height(8.dp))
            if (info != null) { Text(info!!) }
        }
    }
}
