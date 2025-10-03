package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.zafer.smm.data.SmmRepository
import com.zafer.smm.data.remote.ApiService
import com.zafer.smm.data.remote.ServiceDto
import com.zafer.smm.ui.MainViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrofit + ApiService
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://kd1s.com/api/v2/") // قاعدة الـAPI
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        val repo = SmmRepository(api, BuildConfig.API_KEY)
        val vm = MainViewModel(repo)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ServicesScreen(vm)
                }
            }
        }
    }
}

@Composable
fun ServicesScreen(vm: MainViewModel) {
    val services = vm.services
    val loading = vm.loading
    val error = vm.error
    val placing = vm.placing
    val orderMessage = vm.orderMessage

    var showOrderDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<ServiceDto?>(null) }
    var link by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("100") }

    LaunchedEffect(Unit) { vm.loadServices() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMM App", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text("Backend: https://kd1s.com/api/v2", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("خطأ: $error")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.loadServices() }) { Text("إعادة المحاولة") }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(services) { s ->
                    ServiceRow(s) {
                        selectedService = s
                        link = ""
                        qtyText = s.min?.toString() ?: "100"
                        showOrderDialog = true
                    }
                    Divider()
                }
            }
        }

        if (orderMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(orderMessage, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showOrderDialog && selectedService != null) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            confirmButton = {
                Button(
                    enabled = !placing,
                    onClick = {
                        val qty = qtyText.toIntOrNull() ?: 0
                        if (qty > 0 && link.isNotBlank()) {
                            vm.placeOrder(
                                serviceId = selectedService!!.service ?: 0L,
                                link = link.trim(),
                                qty = qty
                            )
                            // أغلق الحوار بعد الإرسال
                            showOrderDialog = false
                        }
                    }
                ) { Text(if (placing) "جارٍ الإرسال..." else "إرسال الطلب") }
            },
            dismissButton = {
                TextButton(onClick = { showOrderDialog = false }) { Text("إلغاء") }
            },
            title = { Text(selectedService!!.name ?: "Service") },
            text = {
                Column {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("الرابط (link)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("الكمية (quantity)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("السعر لكل 1000: ${selectedService!!.rate ?: 0.0}")
                    Text("الحد الأدنى: ${selectedService!!.min ?: 0} | الحد الأقصى: ${selectedService!!.max ?: 0}")
                }
            }
        )
    }
}

@Composable
fun ServiceRow(s: ServiceDto, onOrderClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(s.name ?: "Service", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("ID: ${s.service ?: 0} | النوع: ${s.type ?: "-"}")
        Text("السعر/1000: ${s.rate ?: 0.0} | Min: ${s.min ?: 0} | Max: ${s.max ?: 0}")
        Spacer(Modifier.height(6.dp))
        Row {
            Button(onClick = onOrderClick) { Text("طلب") }
        }
    }
}
