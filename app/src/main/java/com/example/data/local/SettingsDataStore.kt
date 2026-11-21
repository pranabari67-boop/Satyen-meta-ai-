package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.satyenPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "satyen_meta_ai_prefs"
)

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    BN("bn", "বাংলা");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class LocalAppPreferences(
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val onboardingSeen: Boolean = false,
    val biometricLockEnabled: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("preferred_language")
    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyOnboarding = booleanPreferencesKey("onboarding_seen")
    private val keyBiometric = booleanPreferencesKey("biometric_lock_enabled")

    val preferences: Flow<LocalAppPreferences> = context.satyenPrefsDataStore.data.map { prefs ->
        LocalAppPreferences(
            language = AppLanguage.fromCode(prefs[keyLanguage]),
            themeMode = AppThemeMode.fromName(prefs[keyTheme]),
            onboardingSeen = prefs[keyOnboarding] ?: false,
            biometricLockEnabled = prefs[keyBiometric] ?: false
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.satyenPrefsDataStore.edit { it[keyLanguage] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.satyenPrefsDataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.satyenPrefsDataStore.edit { it[keyOnboarding] = seen }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.satyenPrefsDataStore.edit { it[keyBiometric] = enabled }
    }
}
