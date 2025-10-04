package com.zafer.smm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.LocalCatalog
import com.zafer.smm.data.local.LocalService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    sectionKey: String,
    title: String,
    onBack: () -> Unit,
    onOrderClicked: (LocalService) -> Unit
) {
    val section = LocalCatalog.sections.firstOrNull { it.key == sectionKey }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { inner ->
        if (section == null) {
            Box(Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
                Text("لا توجد خدمات لهذا القسم.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(section.services) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(service.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("السعر: ${service.price} $", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                onOrderClicked(service)   // هنا تنفّذ فتح نموذج الطلب أو الاتصال بالباكند
                                scope.launch { snackbar.showSnackbar("تم اختيار: ${service.name}") }
                            }) {
                                Text("طلب الخدمة")
                            }
                        }
                    }
                }
            }
        }
    }
}
