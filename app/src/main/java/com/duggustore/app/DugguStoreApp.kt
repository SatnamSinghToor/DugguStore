package com.duggustore.app

import android.app.Application
import com.duggustore.app.data.remote.SessionManager

class DugguStoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
