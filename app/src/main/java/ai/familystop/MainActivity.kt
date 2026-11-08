package ai.familystop

import ai.familystop.core.designsystem.AppTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as FamilyStopApp).graph
        setContent { AppTheme { AppRoot(graph) } }
    }
}
