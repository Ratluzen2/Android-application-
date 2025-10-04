package com.zafer.smm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.LocalSection
import com.zafer.smm.data.local.LocalService
import java.util.Locale

@Composable
fun ServiceListScreen(
    section: LocalSection,
    onBack: () -> Unit,
    onOrderClick: (LocalService) -> Unit
) {
    var query by remember { mutableStateOf("") }

    // تصفية بسيطة بالاسم
    val filtered = remember(section, query) {
        if (query.isBlank()) section.services
        else section.services.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // رأس الشاشة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            OutlinedButton(onClick = onBack) {
                Text("رجوع")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // بحث
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث عن خدمة") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // قائمة الخدمات
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { service ->
                ServiceCard(service = service, onOrderClick = { onOrderClick(service) })
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: LocalService,
    onOrderClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val priceText = String.format(Locale.US, "%.2f", service.price)
            Text(text = "السعر: $priceText \$")

            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onOrderClick) {
                Text("طلب الخدمة")
            }
        }
    }
}
