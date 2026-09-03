package com.duggustore.app

import android.app.Application
import com.duggustore.app.data.local.AppPrefs
import com.duggustore.app.data.remote.SessionManager
import org.osmdroid.config.Configuration

class DugguStoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        AppPrefs.init(this)

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
}
