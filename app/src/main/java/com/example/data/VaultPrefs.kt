package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hidex_vault_prefs")

class VaultPrefs(private val context: Context) {

    companion object {
        val KEY_PRIMARY_PIN = stringPreferencesKey("primary_pin")
        val KEY_DECOY_PIN = stringPreferencesKey("decoy_pin")
        val KEY_IS_PIN_SET = booleanPreferencesKey("is_pin_set")
        val KEY_SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val KEY_AUTOLOCK_TIMEOUT = intPreferencesKey("autolock_timeout") // in seconds, 0 for off
        val KEY_STEALTH_NOTIFICATIONS = booleanPreferencesKey("stealth_notifications")
        val KEY_INTRUDER_THRESHOLD = intPreferencesKey("intruder_threshold")
        val KEY_THEME_SELECTION = stringPreferencesKey("theme_selection") // LIGHT, DARK, AMOLED, SYSTEM
        val KEY_DISGUISED_LAUNCHER = booleanPreferencesKey("disguised_launcher")
        val KEY_BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val KEY_GOOGLE_DRIVE_CONNECTED = booleanPreferencesKey("google_drive_connected")
        val KEY_GOOGLE_ACCOUNT_NAME = stringPreferencesKey("google_account_name")
        val KEY_GOOGLE_DRIVE_ACCESS_TOKEN = stringPreferencesKey("google_drive_access_token")
    }

    val primaryPin: Flow<String?> = context.dataStore.data.map { it[KEY_PRIMARY_PIN] }
    val decoyPin: Flow<String?> = context.dataStore.data.map { it[KEY_DECOY_PIN] }
    val isPinSet: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_PIN_SET] ?: false }
    val screenshotProtection: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCREENSHOT_PROTECTION] ?: true }
    val autolockTimeout: Flow<Int> = context.dataStore.data.map { it[KEY_AUTOLOCK_TIMEOUT] ?: 60 } // Default 1 min
    val stealthNotifications: Flow<Boolean> = context.dataStore.data.map { it[KEY_STEALTH_NOTIFICATIONS] ?: false }
    val intruderThreshold: Flow<Int> = context.dataStore.data.map { it[KEY_INTRUDER_THRESHOLD] ?: 3 } // 3 failed attempts
    val themeSelection: Flow<String> = context.dataStore.data.map { it[KEY_THEME_SELECTION] ?: "SYSTEM" }
    val disguisedLauncher: Flow<Boolean> = context.dataStore.data.map { it[KEY_DISGUISED_LAUNCHER] ?: false }
    val backupWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_BACKUP_WIFI_ONLY] ?: true }
    val googleDriveConnected: Flow<Boolean> = context.dataStore.data.map { it[KEY_GOOGLE_DRIVE_CONNECTED] ?: false }
    val googleAccountName: Flow<String> = context.dataStore.data.map { it[KEY_GOOGLE_ACCOUNT_NAME] ?: "" }
    val googleDriveAccessToken: Flow<String> = context.dataStore.data.map { it[KEY_GOOGLE_DRIVE_ACCESS_TOKEN] ?: "" }

    suspend fun setPrimaryPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRIMARY_PIN] = pin
            prefs[KEY_IS_PIN_SET] = true
        }
    }

    suspend fun setDecoyPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DECOY_PIN] = pin
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setAutolockTimeout(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOLOCK_TIMEOUT] = seconds
        }
    }

    suspend fun setStealthNotifications(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STEALTH_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setIntruderThreshold(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INTRUDER_THRESHOLD] = count
        }
    }

    suspend fun setThemeSelection(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_SELECTION] = theme
        }
    }

    suspend fun setDisguisedLauncher(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISGUISED_LAUNCHER] = enabled
        }
    }

    suspend fun setBackupWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKUP_WIFI_ONLY] = enabled
        }
    }

    suspend fun setGoogleDriveConnected(connected: Boolean, accountName: String = "", accessToken: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_DRIVE_CONNECTED] = connected
            prefs[KEY_GOOGLE_ACCOUNT_NAME] = accountName
            prefs[KEY_GOOGLE_DRIVE_ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun clearPinSetup() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PRIMARY_PIN)
            prefs.remove(KEY_DECOY_PIN)
            prefs[KEY_IS_PIN_SET] = false
        }
    }
}
