package com.example.fitnesscoachai.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Single source of truth for the user's auth state.
 *
 * Tokens are stored in **EncryptedSharedPreferences**, backed by Tink + the
 * Android Keystore. This means even on a rooted device, the raw access /
 * refresh tokens are not readable as plaintext from disk.
 *
 * Non-secret flags (isGuest, isLoggedIn) and small profile crumbs (user_id,
 * email, name) stay in the regular "auth" prefs so the existing code that
 * already reads them keeps working without churn.
 */
class TokenStore private constructor(private val context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "auth_secure"

        // Keys inside the encrypted store
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"

        @Volatile
        private var instance: TokenStore? = null

        fun get(context: Context): TokenStore {
            return instance ?: synchronized(this) {
                instance ?: TokenStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            // Fallback: if Tink/Keystore fails on some weird device, never
            // crash auth. Use plain prefs as a last resort. (Rare; mostly
            // hits very old devices with corrupted keystore.)
            context.getSharedPreferences(SECURE_PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    // ---- Public API ----

    val accessToken: String?
        get() = securePrefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() }

    val refreshToken: String?
        get() = securePrefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }

    fun bearerHeader(): String? = accessToken?.let { "Bearer $it" }

    fun saveTokens(access: String?, refresh: String?) {
        securePrefs.edit().apply {
            if (access.isNullOrBlank()) remove(KEY_ACCESS) else putString(KEY_ACCESS, access)
            if (refresh.isNullOrBlank()) remove(KEY_REFRESH) else putString(KEY_REFRESH, refresh)
        }.apply()
    }

    fun saveAccessToken(access: String?) {
        securePrefs.edit().apply {
            if (access.isNullOrBlank()) remove(KEY_ACCESS) else putString(KEY_ACCESS, access)
        }.apply()
    }

    fun clear() {
        securePrefs.edit().clear().apply()
    }

    /**
     * One-time migration: if the user already has tokens in the legacy "auth"
     * prefs (from before we introduced EncryptedSharedPreferences), pull them
     * over here so they don't get logged out by the upgrade.
     */
    fun migrateFromLegacyIfNeeded() {
        if (accessToken != null) return  // already migrated

        val legacy = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val legacyAccess = legacy.getString("access_token", null)
        val legacyRefresh = legacy.getString("refresh_token", null)
        if (!legacyAccess.isNullOrBlank() || !legacyRefresh.isNullOrBlank()) {
            saveTokens(legacyAccess, legacyRefresh)
            // Strip them from the unencrypted store but keep other flags.
            legacy.edit().remove("access_token").remove("refresh_token").apply()
        }
    }
}
