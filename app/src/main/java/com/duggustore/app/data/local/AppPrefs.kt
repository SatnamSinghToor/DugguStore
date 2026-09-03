package com.duggustore.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * App settings that outlive a session. Deliberately a separate store from the
 * auth session, whose clearSession() wipes everything in its own file — the
 * chosen language should survive signing out.
 */
object AppPrefs {
    private const val PREF_NAME = "duggu_store_prefs"
    private const val KEY_LANGUAGE = "language"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Null means "follow the device", which is the default.
     *
     * Takes a context rather than using the cached instance because
     * attachBaseContext runs before Application.onCreate, so init() has not
     * happened yet at the point the locale has to be applied.
     */
    fun language(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)

    fun setLanguage(context: Context, tag: String?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().apply {
            if (tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, tag)
            apply()
        }
    }
}
