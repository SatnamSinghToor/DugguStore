package com.duggustore.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.duggustore.app.data.remote.AuthDeepLinkParser
import com.duggustore.app.ui.navigation.AppNavGraph
import com.duggustore.app.ui.theme.DugguStoreTheme
import com.duggustore.app.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    // Held by the activity so the deep link and the UI act on the same instance.
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthDeepLink(intent)
        setContent {
            DugguStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController, authViewModel = authViewModel)
                }
            }
        }
    }

    /** launchMode=singleTask means a link arriving while the app is open lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
    }

    private fun handleAuthDeepLink(intent: Intent?) {
        val link = AuthDeepLinkParser.parse(intent?.data) ?: return
        authViewModel.onAuthDeepLink(link)
        // Clear it so a configuration change does not replay the same link.
        intent?.data = null
    }
}
