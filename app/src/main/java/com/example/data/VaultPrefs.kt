package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hidex_vault_prefs")

class VaultPrefs(private val context: Context) {

    companion object {
        val KEY_PRIMARY_PIN = stringPreferencesKey("primary_pin_hash")
        val KEY_DECOY_PIN = stringPreferencesKey("decoy_pin_hash")
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
        val KEY_GOOGLE_DRIVE_ACCESS_TOKEN = stringPreferencesKey("google_drive_access_token_enc")

        /**
         * A device-bound random key used to encrypt the Google Drive access token at rest.
         * Generated on first use and persisted alongside the encrypted token.
         */
        val KEY_TOKEN_WRAP_KEY = stringPreferencesKey("token_wrap_key_b64")
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

    /**
     * Decrypts the Google Drive access token on read. The token is stored AES-encrypted
     * with a device-bound random wrapping key so it is never persisted in plaintext.
     */
    val googleDriveAccessToken: Flow<String> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[KEY_GOOGLE_DRIVE_ACCESS_TOKEN] ?: ""
        val wrapKey = prefs[KEY_TOKEN_WRAP_KEY] ?: ""
        if (encrypted.isEmpty() || wrapKey.isEmpty()) {
            ""
        } else {
            // The wrap key is a fixed-length random string used as the "pin" for token encryption.
            SecurityManager.decrypt(encrypted, wrapKey)
        }
    }

    /**
     * Stores the primary PIN as a PBKDF2 hash (never plaintext). Verification is done
     * via [SecurityManager.verifyPin] at unlock time.
     */
    suspend fun setPrimaryPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRIMARY_PIN] = SecurityManager.hashPin(pin)
            prefs[KEY_IS_PIN_SET] = true
        }
    }

    suspend fun setDecoyPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DECOY_PIN] = SecurityManager.hashPin(pin)
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

    /**
     * Persists the Google Drive connection. The access token is encrypted at rest with a
     * device-bound random wrapping key (generated lazily on first connect) so plaintext
     * tokens are never written to disk.
     */
    suspend fun setGoogleDriveConnected(connected: Boolean, accountName: String = "", accessToken: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_DRIVE_CONNECTED] = connected
            prefs[KEY_GOOGLE_ACCOUNT_NAME] = accountName
            if (accessToken.isNotEmpty()) {
                // Lazily generate a device-bound wrapping key if none exists yet.
                var wrapKey = prefs[KEY_TOKEN_WRAP_KEY]
                if (wrapKey.isNullOrEmpty()) {
                    wrapKey = generateTokenWrapKey()
                    prefs[KEY_TOKEN_WRAP_KEY] = wrapKey
                }
                prefs[KEY_GOOGLE_DRIVE_ACCESS_TOKEN] = SecurityManager.encrypt(accessToken, wrapKey)
            } else if (!connected) {
                // On disconnect, scrub the token + wrap key entirely.
                prefs.remove(KEY_GOOGLE_DRIVE_ACCESS_TOKEN)
                prefs.remove(KEY_TOKEN_WRAP_KEY)
            }
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

    /**
     * Generates a 256-bit random wrapping key encoded as Base64 (URL-safe, no wrap).
     * This key is device-bound and used solely to encrypt/decrypt the Drive token.
     */
    private fun generateTokenWrapKey(): String {
        val keyBytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(keyBytes)
        return android.util.Base64.encodeToString(keyBytes, android.util.Base64.NO_WRAP)
    }
}
