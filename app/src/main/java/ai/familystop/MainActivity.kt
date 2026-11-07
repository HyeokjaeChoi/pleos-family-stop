package ai.familystop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var soc by remember { mutableStateOf<Int?>(null) }
            DisposableEffect(Unit) {
                AppGraph.energy.start { soc = it }
                onDispose { AppGraph.energy.stop() }
            }
            MaterialTheme { Text("Family Stop · 배터리 ${soc?.let { "$it%" } ?: "확인 안 됨"}") }
        }
    }
}
