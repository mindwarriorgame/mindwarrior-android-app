package com.mindwarrior.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "mindwarrior_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    data class LanguageOption(
        val tag: String,
        val labelResId: Int
    )

    val supportedLanguages = listOf("en", "ru", "fr", "de", "es")

    val languageOptions = listOf(
        LanguageOption("en", R.string.language_label_english),
        LanguageOption("ru", R.string.language_label_russian),
        LanguageOption("fr", R.string.language_label_french),
        LanguageOption("de", R.string.language_label_german),
        LanguageOption("es", R.string.language_label_spanish)
    )

    fun init(context: Context) {
        val saved = getSavedLanguageTag(context)
        if (saved != null) {
            applyLanguage(saved)
            return
        }
        val systemLang = Locale.getDefault().language
        if (supportedLanguages.contains(systemLang) && systemLang != "en") {
            saveLanguageTag(context, systemLang)
            applyLanguage(systemLang)
        }
    }

    fun shouldShowLanguagePrompt(context: Context): Boolean {
        val saved = getSavedLanguageTag(context)
        if (saved != null) return false
        val systemLang = Locale.getDefault().language
        return systemLang == "en" || !supportedLanguages.contains(systemLang)
    }

    fun setLanguage(context: Context, tag: String) {
        if (!supportedLanguages.contains(tag)) return
        saveLanguageTag(context, tag)
        applyLanguage(tag)
    }

    fun getSavedLanguageTag(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_TAG, null)
    }

    fun getCurrentLanguageTag(context: Context): String {
        return getSavedLanguageTag(context) ?: Locale.getDefault().language
    }

    fun getLanguageLabel(context: Context, tag: String): String {
        val option = languageOptions.firstOrNull { it.tag == tag }
            ?: languageOptions.first()
        return context.getString(option.labelResId)
    }

    private fun saveLanguageTag(context: Context, tag: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE_TAG, tag).apply()
    }

    private fun applyLanguage(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
