package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.remote.ApiConfig
import com.zafer.smm.ui.AppViewModel
import com.zafer.smm.ui.UiService

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { HomeScreen(vm) }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val balance by vm.balance.collectAsState()
    val services by vm.services.collectAsState()
    val cats by vm.categories.collectAsState()
    val selected by vm.selectedCat.collectAsState()
    val profile by vm.profile.collectAsState()

    var link by remember { mutableStateOf("https://example.com") }
    var qty by remember { mutableStateOf("100") }

    val servicesInCat = remember(services, selected) {
        services.filter { (it.raw.category ?: "Other") == (selected ?: "Other") }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMM App — ${profile.role.name}", style = MaterialTheme.typography.headlineSmall)
        Text("Backend: ${ApiConfig.BASE_URL}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) { Spacer(Modifier.height(6.dp)); Text("خطأ: $error", color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.initialLoad() }) { Text("تحديث") }
            Button(onClick = { vm.refreshBalance() }) { Text("الرصيد: ${balance?.balance ?: 0.0}") }
            Button(onClick = { /* TODO: طلباتي */ }) { Text("طلباتي") }
            Button(onClick = { /* TODO: المتصدرين */ }) { Text("المتصدرين") }
        }

        Spacer(Modifier.height(12.dp))
        Text("التصنيفات", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(cats) { c ->
                FilterChip(
                    selected = (selected ?: cats.firstOrNull()) == c,
                    onClick = { vm.setCategory(c) },
                    label = { Text(c) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("الخدمات", style = MaterialTheme.typography.titleMedium)

        LazyColumn(Modifier.weight(1f)) {
            items(servicesInCat) { s ->
                ServiceCard(s, link, qty,
                    onLinkChange = { link = it },
                    onQtyChange = { qty = it },
                    onOrder = {
                        val q = qty.toIntOrNull() ?: 0
                        if (q > 0 && link.isNotBlank()) vm.placeOrder(s, link, q)
                    }
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    s: UiService,
    link: String,
    qty: String,
    onLinkChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onOrder: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(s.label, style = MaterialTheme.typography.titleMedium)
            Text("ID:${s.raw.service}  •  Min:${s.raw.min}  •  Max:${s.raw.max}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = link, onValueChange = onLinkChange,
                label = { Text("Link / معرف") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = qty, onValueChange = onQtyChange,
                label = { Text("Quantity") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = onOrder, modifier = Modifier.align(Alignment.End)) { Text("إنشاء طلب") }
        }
    }
}
