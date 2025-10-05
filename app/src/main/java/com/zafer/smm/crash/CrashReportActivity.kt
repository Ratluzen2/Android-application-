package com.zafer.smm.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zafer.smm.MainActivity

class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                CrashScreen { restartApp() }
            }
        }
    }

    private fun restartApp() {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        finish()
    }
}

@Composable
private fun CrashScreen(onRestart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("حدث خطأ غير متوقع.")
        Button(onClick = onRestart, modifier = Modifier.padding(top = 12.dp)) {
            Text("إعادة فتح التطبيق")
        }
    }
}
