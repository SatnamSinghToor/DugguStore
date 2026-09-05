package com.duggustore.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.rememberNavController
import com.duggustore.app.data.remote.AuthDeepLinkParser
import com.duggustore.app.platform.withAppLanguage
import com.duggustore.app.ui.navigation.AppNavGraph
import com.duggustore.app.ui.theme.DugguStoreTheme
import com.duggustore.app.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    // Held by the activity so the deep link and the UI act on the same instance.
    private val authViewModel: AuthViewModel by viewModels()

    /**
     * Everything the activity resolves — including every string Compose reads
     * through stringResource — comes from this context, so applying the chosen
     * language here is what makes the whole app switch.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthDeepLink(intent)
        setContent {
            DugguStoreTheme {
                // enableEdgeToEdge() stops the window resizing itself for the
                // keyboard, so adjustResize in the manifest no longer moves
                // anything on its own: the IME inset is handed to Compose and
                // has to be consumed here, or whatever sits at the bottom of a
                // screen ends up underneath the keyboard.
                //
                // While the keyboard is up it covers the navigation bar too, so
                // the navigation-bar inset is consumed at the same time —
                // otherwise the screens that pad for it themselves would add
                // that height a second time and float above the keyboard.
                //
                // The raw inset shrinks and grows on every frame of the
                // keyboard's slide animation, and reading it directly here —
                // above the whole nav graph — recomposed the entire visible
                // screen on every one of those frames, which is what showed
                // up as jank each time the keyboard opened or closed,
                // anywhere in the app. derivedStateOf collapses that down to
                // the boolean actually flipping, so this only recomposes
                // once per open/close instead of once per animation frame.
                val density = LocalDensity.current
                // WindowInsets.ime is itself a @Composable getter, so it has
                // to be read here, in composable context — only the plain
                // getBottom() call on the result can live inside
                // derivedStateOf's (non-composable) calculation lambda.
                val imeInsets = WindowInsets.ime
                val imeVisible by remember(density, imeInsets) {
                    derivedStateOf { imeInsets.getBottom(density) > 0 }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .then(
                            if (imeVisible) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                            else Modifier
                        )
                ) {
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
