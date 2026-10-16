package se.pettersoft.steplord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import se.pettersoft.steplord.notifications.StepNotifier
import se.pettersoft.steplord.ui.navigation.StepLordNavHost
import se.pettersoft.steplord.ui.theme.StepLordTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var stepNotifier: StepNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The activity is recreated when the app language changes; keep channel names in sync
        stepNotifier.createChannels()
        enableEdgeToEdge()
        setContent {
            StepLordTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StepLordNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
