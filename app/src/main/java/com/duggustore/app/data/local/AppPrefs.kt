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
    private const val KEY_RECENT_SEARCHES = "recent_searches"
    // Newline rather than a comma — a search term could contain one, but the
    // single-line search field it comes from can never produce a newline.
    private const val RECENT_SEARCH_SEPARATOR = "\n"
    private const val MAX_RECENT_SEARCHES = 8

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

    /** Most recent search first, capped at [MAX_RECENT_SEARCHES]. */
    fun recentSearches(context: Context): List<String> =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_SEARCHES, null)
            ?.split(RECENT_SEARCH_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /** Moves [term] to the front, de-duplicating case-insensitively. */
    fun addRecentSearch(context: Context, term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + recentSearches(context).filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_RECENT_SEARCHES, updated.joinToString(RECENT_SEARCH_SEPARATOR))
            .apply()
    }

    fun clearRecentSearches(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_RECENT_SEARCHES)
            .apply()
    }
}
