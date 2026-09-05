package com.duggustore.app

import android.app.Application
import com.duggustore.app.data.local.AppPrefs
import com.duggustore.app.data.remote.SessionManager
import org.osmdroid.config.Configuration
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DugguStoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        AppPrefs.init(this)
        installCrashLogger()

        // osmdroid's tile server rejects requests with no identifying user
        // agent, and defaults to writing its cache somewhere that needs a
        // storage permission this app doesn't otherwise ask for — the app's
        // own private cache dir needs neither.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid/tiles").apply { mkdirs() }
        }
    }

    /**
     * A "sometimes the screen just goes blank, has to force-close" report is
     * otherwise unreproducible — there's no log to say what actually threw.
     * This writes the stack trace of any uncaught exception to a file under
     * app-specific external storage (no permission needed, and browsable
     * from a phone's Files app under Android/data/<package>/files/crash_logs)
     * before handing off to the previous handler, so a crash still behaves
     * exactly as it did — it's just no longer silent.
     */
    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = File(getExternalFilesDir(null), "crash_logs").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
                File(dir, "crash_$stamp.txt").writeText(
                    "Thread: ${thread.name}\n\n$trace"
                )
            } catch (_: Throwable) {
                // A failure while trying to log the original crash must never
                // replace it — fall through to the previous handler either way.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
