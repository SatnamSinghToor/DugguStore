package com.duggustore.app.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Wires the mic to the device's speech recogniser.
 *
 * Returns null when no app on the device can handle a speech request, so the
 * caller can leave the mic out rather than show a button that does nothing.
 */
@Composable
fun rememberVoiceSearch(onResult: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current

    val available = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .resolveActivity(context.packageManager) != null
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (!spoken.isNullOrEmpty()) onResult(spoken)
    }

    if (!available) return null

    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a product name")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // resolveActivity said yes but the handler went away in between.
        }
    }
}
