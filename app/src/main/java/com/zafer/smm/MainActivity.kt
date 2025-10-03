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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.remote.ApiConfig
import com.zafer.smm.data.model.ServiceItem
import com.zafer.smm.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    MainScreen(vm, deviceId)
                }
            }
        }

        // تسجيل أولي
        vm.register(deviceId, fullName = null, username = null)
    }
}

@Composable
fun MainScreen(vm: MainViewModel, deviceId: String) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val services by vm.services.collectAsState()
    val balance by vm.balance.collectAsState()
    val lastOrderId by vm.lastOrderId.collectAsState()
    val lastStatus by vm.lastStatus.collectAsState()

    var serviceIdText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("https://example.com") }
    var qtyText by remember { mutableStateOf("100") }
    var statusOrderIdText by remember { mutableStateOf("") }
    var depositText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMM App", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Backend: ${ApiConfig.BASE_URL}", style = MaterialTheme.typography.bodyMedium)

        if (loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text("خطأ: $it", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshServices() }) { Text("تحديث الخدمات") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.getUserBalance(deviceId) }) { Text("الرصيد") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.loadLeaderboard() }) { Text("المتصدرين") }
        }

        Spacer(Modifier.height(16.dp))
        Text("إنشاء طلب", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = serviceIdText, onValueChange = { serviceIdText = it },
            label = { Text("Service ID") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = link, onValueChange = { link = it },
            label = { Text("Link") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = qtyText, onValueChange = { qtyText = it },
            label = { Text("Quantity") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val sid = serviceIdText.toIntOrNull()
                val q = qtyText.toIntOrNull()
                if (sid != null && q != null && link.isNotBlank()) {
                    vm.placeOrder(deviceId, sid, link, q)
                }
            }
        ) { Text("إنشاء طلب") }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = statusOrderIdText, onValueChange = { statusOrderIdText = it },
                label = { Text("Provider Order ID") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { statusOrderIdText.toLongOrNull()?.let { vm.getOrderStatus(it) } }) {
                Text("حالة الطلب")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = depositText, onValueChange = { depositText = it },
                label = { Text("إيداع (+)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { depositText.toDoubleOrNull()?.let { vm.walletDeposit(deviceId, it) } }) {
                Text("تنفيذ")
            }
        }

        Spacer(Modifier.height(12.dp))
        balance?.let { Text("الرصيد: ${it.balance} ${it.currency}") }
        lastOrderId?.let { Text("آخر رقم طلب لدى المزوّد: $it") }
        lastStatus?.let { s ->
            Text("الحالة: ${s.status ?: "-"} | المتبقي: ${s.remains ?: "-"} | التكلفة: ${s.charge ?: "-"}")
        }

        Spacer(Modifier.height(12.dp))
        Text("الخدمات (${services.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(services) { s -> ServiceRow(s) }
        }
    }
}

@Composable
private fun ServiceRow(s: ServiceItem) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("ID: ${s.service}  |  ${s.name}")
            Text("Category: ${s.category ?: "-"}")
            Text("Rate: ${s.rate}  |  Min: ${s.min}  |  Max: ${s.max}")
        }
    }
}
