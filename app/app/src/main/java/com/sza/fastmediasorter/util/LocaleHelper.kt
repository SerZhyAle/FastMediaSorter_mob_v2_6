package com.sza.fastmediasorter.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import timber.log.Timber
import java.util.Locale

/**
 * Utility class for handling app-wide locale changes.
 * 
 * Supports per-app language preferences on Android 13+ and
 * provides fallback for older Android versions.
 */
object LocaleHelper {

    /**
     * Set the app locale.
     * 
     * On Android 13+ (API 33), uses the built-in per-app language API.
     * On older versions, stores preference and requires activity recreation.
     * 
     * @param languageCode ISO 639-1 language code (e.g., "en", "ru", "uk") or "system"
     */
    fun setLocale(languageCode: String) {
        Timber.d("Setting locale to: $languageCode")
        
        if (languageCode == "system") {
            // Use system default
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            // Set specific locale
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
        
        Timber.d("Locale set successfully to: $languageCode")
    }

    /**
     * Get current app locale.
     * 
     * @return Current locale code or "system" if using system default
     */
    fun getCurrentLocale(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            "system"
        } else {
            locales.get(0)?.language ?: "system"
        }
    }

    /**
     * Wrap context with locale configuration.
     * Used for attachBaseContext in Activities.
     * 
     * @param context Base context
     * @param languageCode ISO 639-1 language code or "system"
     * @return Context with updated locale configuration
     */
    fun wrapContext(context: Context, languageCode: String): Context {
        if (languageCode == "system") {
            return context
        }
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return context.createConfigurationContext(config)
    }

    /**
     * Get list of supported locales.
     * 
     * @return List of pairs (code, displayName)
     */
    fun getSupportedLocales(): List<Pair<String, String>> {
        return listOf(
            "system" to "System Default",
            "en" to "English",
            "ru" to "Русский",
            "uk" to "Українська"
        )
    }

    /**
     * Get display name for a locale code.
     * 
     * @param languageCode ISO 639-1 language code or "system"
     * @return Human-readable display name
     */
    fun getDisplayName(languageCode: String): String {
        return when (languageCode) {
            "system" -> "System Default"
            "en" -> "English"
            "ru" -> "Русский"
            "uk" -> "Українська"
            else -> Locale(languageCode).displayName
        }
    }
}
