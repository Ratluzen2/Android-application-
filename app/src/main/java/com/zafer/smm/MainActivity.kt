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
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.remote.ApiService
import com.zafer.smm.ui.MainViewModel
import com.zafer.smm.data.model.LocalMappedService

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
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val services by vm.services.collectAsState()
    val lastOrderId by vm.lastOrderId.collectAsState()
    val lastStatus by vm.lastStatus.collectAsState()
    val balance by vm.balance.collectAsState()

    var serviceIdText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("https://example.com") }
    var quantityText by remember { mutableStateOf("1000") }
    var search by remember { mutableStateOf("") }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("SMM App (نسخة تطبيق للبوت)", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Backend: ${ApiService.BASE_URL}", style = MaterialTheme.typography.bodyMedium)
        Text("API KEY: ${ApiService.API_KEY.take(6)}********** (مضمّن)")

        Spacer(Modifier.height(12.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        if (error != null) {
            Text("خطأ: $error", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        if (infoMsg != null) {
            Text(infoMsg!!, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshServices() }) { Text("تحديث الخدمات") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.fetchBalance() }) { Text("الرصيد") }
            Spacer(Modifier.width(8.dp))
            Button(enabled = lastOrderId != null, onClick = { vm.checkOrderStatus() }) {
                Text("حالة آخر طلب")
            }
        }

        Spacer(Modifier.height(12.dp))

        // مدخلات الطلب (تُملأ تلقائيًا عند اختيار خدمة من القائمة)
        OutlinedTextField(
            value = serviceIdText,
            onValueChange = { serviceIdText = it },
            label = { Text("Service ID (من القائمة بالأسفل)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("Link (رابط الحساب/المنشور)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("Quantity (مطابق لمضاعف الخدمة)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val sid = serviceIdText.toIntOrNull()
                val qty = quantityText.toIntOrNull()
                infoMsg = null
                if (sid != null && qty != null && link.isNotBlank()) {
                    vm.placeOrder(sid, link, qty)
                } else {
                    infoMsg = "تحقق من Service ID و Quantity والـ Link."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("إنشاء طلب عبر kd1s") }

        Spacer(Modifier.height(12.dp))

        if (balance != null) {
            Text("الرصيد: ${balance?.balance ?: "-"} ${balance?.currency ?: ""}")
            Spacer(Modifier.height(8.dp))
        }

        if (lastOrderId != null) {
            Text("آخر Order ID: $lastOrderId")
            Spacer(Modifier.height(8.dp))
        }

        if (lastStatus != null) {
            val st = lastStatus!!
            Text("الحالة: ${st.status ?: "-"} / المتبقي: ${st.remains ?: "-"} / التكلفة: ${st.charge ?: "-"}")
            Spacer(Modifier.height(8.dp))
        }

        // بحث + اختيار خدمة يملأ الحقول تلقائيًا
        Text("ابحث واختر خدمة (يتم تعبئة Service ID و Quantity تلقائيًا):",
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("بحث بالاسم… مثال: تيكتوك، انستغرام، تلي…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))

        val filtered = remember(services, search) {
            if (search.isBlank()) services
            else services.filter {
                it.displayName.contains(search, ignoreCase = true)
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(filtered) { item ->
                ServiceRow(
                    item = item,
                    onPick = { svc ->
                        if (svc.serviceId == null) {
                            infoMsg = "هذه خدمة عرض/يدوية (لا يوجد service_id). لا يمكن إنشاء طلب تلقائي."
                        } else {
                            serviceIdText = svc.serviceId.toString()
                            quantityText = (svc.quantityMultiplier ?: 1000).toString()
                            infoMsg = "تم تعبئة Service ID و Quantity من الخدمة المختارة."
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(
    item: LocalMappedService,
    onPick: (LocalMappedService) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onPick(item) } // الضغط يختار الخدمة ويملأ الحقول
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("السعر: ${item.priceUsd ?: "-"} USD")
            Text("Service ID: ${item.serviceId?.toString() ?: "—"} | الكمية النموذجية: ${item.quantityMultiplier?.toString() ?: "—"}")
            if (item.serviceId == null) {
                Text("ملاحظة: خدمة عرض/يدوية (لا يوجد service_id).", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
