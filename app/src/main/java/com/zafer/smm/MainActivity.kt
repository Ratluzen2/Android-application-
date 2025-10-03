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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.zafer.smm.ui.MainViewModel
import com.zafer.smm.data.remote.Network
import com.zafer.smm.data.remote.RegisterBody

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    MainScreen(vm)
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val deviceId = remember {
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"
    }

    // تسجيل الجهاز مرة واحدة
    LaunchedEffect(deviceId) {
        try { Network.api.register(RegisterBody(device_id = deviceId, username = null)) } catch (_: Exception) {}
    }

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val services by vm.services.collectAsState()
    val lastOrderId by vm.lastOrderId.collectAsState()
    val lastStatus by vm.lastStatus.collectAsState()
    val balance by vm.balance.collectAsState()

    var serviceIdText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("100") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMM App", style = MaterialTheme.typography.headlineMedium)
        Text("Backend: ${Network.BASE_URL}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)) }
        if (error != null) { Text("خطأ: $error", color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshServices() }) { Text("تحديث الخدمات") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.fetchBalance(deviceId) }) { Text("الرصيد") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (lastOrderId != null) vm.checkOrderStatus() }) { Text("حالة آخر طلب") }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = serviceIdText,
            onValueChange = { serviceIdText = it },
            label = { Text("Service ID") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("Link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("Quantity") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("إنشاء طلب") }

        Spacer(Modifier.height(12.dp))

        if (balance != null) {
            Text("الرصيد: ${balance?.balance} ${balance?.currency}")
            Spacer(Modifier.height(8.dp))
        }
        if (lastOrderId != null) { Text("آخر Order ID: $lastOrderId"); Spacer(Modifier.height(8.dp)) }
        if (lastStatus != null) {
            val st = lastStatus!!
            Text("الحالة: ${st.status ?: "-"} / المتبقي: ${st.remains ?: "-"} / التكلفة: ${st.charge ?: "-"}")
            Spacer(Modifier.height(8.dp))
        }

        Text("قائمة الخدمات (${services.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(services) { s ->
                ServiceRow(s.service, s.name, s.rate, s.min, s.max, s.category)
            }
        }
    }
}

@Composable
private fun ServiceRow(
    id: Int?, name: String?, rate: Double?, min: Int?, max: Int?, category: String?
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("ID: ${id ?: "-"}  |  ${name ?: "-"}")
            Text("Category: ${category ?: "-"}")
            Text("Rate: ${rate ?: "-"}  |  Min: ${min ?: "-"}  |  Max: ${max ?: "-"}")
        }
    }
}
