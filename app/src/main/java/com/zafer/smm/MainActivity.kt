package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.LocalCatalog
import com.zafer.smm.ui.ServiceListScreen
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class Section(val key: String, val title: String) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var screen by rememberSaveable { mutableStateOf<Screen>(Screen.Home) }
                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (screen) {
                                        is Screen.Home -> "خدمات راتلوزن"
                                        is Screen.Section -> "الخدمات"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                // زر دخول المالك (صغير ومكانه يمين أعلى)
                                if (screen is Screen.Home) {
                                    TextButton(onClick = {
                                        scope.launch { snackbar.showSnackbar("زر دخول المالك (لاحقاً ربط شاشة المالك)") }
                                    }) { Text("دخول المالك") }
                                }
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbar) }
                ) { inner ->
                    when (val s = screen) {
                        is Screen.Home -> HomeSections(
                            modifier = Modifier.padding(inner),
                            onOpenSection = { key, title ->
                                screen = Screen.Section(key, title)
                            }
                        )
                        is Screen.Section -> ServiceListScreen(
                            sectionKey = s.key,
                            title = s.title,
                            onBack = { screen = Screen.Home },
                            onOrderClicked = { /* نفّذ نموذج الطلب أو اتصال API هنا */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSections(
    modifier: Modifier = Modifier,
    onOpenSection: (String, String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "الخدمات",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(LocalCatalog.sections) { section ->
                ElevatedCard(
                    onClick = { onOpenSection(section.key, section.title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ادخل لعرض الخدمات وطلب الخدمة",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
