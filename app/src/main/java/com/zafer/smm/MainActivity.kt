package com.zafer.smm

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.zafer.smm.ui.MainViewModel
import com.zafer.smm.data.remote.Network

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { MainScreen(vm) }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device" }

    LaunchedEffect(deviceId) {
        vm.autoRegister(deviceId, null)
        vm.refreshServices(false)
        vm.fetchBalance(deviceId)
        vm.loadLeaders()
    }

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val services by vm.services.collectAsState()
    val lastOrderId by vm.lastOrderId.collectAsState()
    val lastStatus by vm.lastStatus.collectAsState()
    val balance by vm.balance.collectAsState()
    val orders by vm.orders.collectAsState()
    val leaders by vm.leaders.collectAsState()

    var serviceIdText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("100") }
    var depositText by remember { mutableStateOf("0") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMM App", style = MaterialTheme.typography.headlineMedium)
        Text("Backend: ${Network.BASE_URL}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)) }
        if (error != null) { Text("خطأ: $error", color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshServices(true) }) { Text("تحديث الخدمات") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.fetchBalance(deviceId) }) { Text("الرصيد") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.loadOrders(deviceId) }) { Text("طلباتي") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (lastOrderId != null) vm.checkOrderStatus() }) { Text("حالة آخر طلب") }
        }

        Spacer(Modifier.height(12.dp))

        // إنشاء طلب
        Text("إنشاء طلب", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = serviceIdText, onValueChange = { serviceIdText = it }, label = { Text("Service ID") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = quantityText, onValueChange = { quantityText = it }, label = { Text("Quantity") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val sid = serviceIdText.toIntOrNull()
                val qty = quantityText.toIntOrNull()
                if (sid != null && qty != null && link.isNotBlank()) {
                    vm.placeOrder(deviceId, sid, link, qty)
                }
            }, modifier = Modifier.fillMaxWidth()
        ) { Text("إنشاء طلب") }

        Spacer(Modifier.height(12.dp))

        // الرصيد والمحفظة
        if (balance != null) {
            Text("الرصيد: ${balance?.balance} ${balance?.currency}")
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = depositText, onValueChange = { depositText = it },
                label = { Text("إيداع (+)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val amt = depositText.toDoubleOrNull() ?: 0.0
                if (amt > 0) vm.deposit(deviceId, amt, "manual")
            }) { Text("تنفيذ") }
        }

        Spacer(Modifier.height(12.dp))

        if (lastOrderId != null) {
            Text("آخر Order ID: $lastOrderId"); Spacer(Modifier.height(8.dp))
        }
        if (lastStatus != null) {
            val st = lastStatus!!
            Text("الحالة: ${st.status ?: "-"} / المتبقي: ${st.remains ?: "-"} / التكلفة: ${st.charge ?: "-"}")
            Spacer(Modifier.height(8.dp))
        }

        // قائمة الخدمات مختصرة
        Text("الخدمات (${services.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.height(160.dp)) {
            items(services.take(12)) { s ->
                Text("• ${s.service} - ${s.name} [${s.rate}]")
            }
        }

        Spacer(Modifier.height(12.dp))
        // المتصدرون
        Text("المتصدرون (شهريًا)", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.height(120.dp)) {
            items(leaders) { l ->
                Text("• ${l.name ?: "مستخدم"} - ${l.spent}")
            }
        }

        Spacer(Modifier.height(12.dp))
        // طلباتي
        Text("طلباتي", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(orders) { o ->
                Text("• ${o.order} | S:${o.service} | ${o.status} | ${o.charge ?: 0.0}")
            }
        }
    }
}
