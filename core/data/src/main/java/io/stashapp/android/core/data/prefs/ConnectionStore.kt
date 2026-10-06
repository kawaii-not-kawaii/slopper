package io.stashapp.android.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.model.StashServer
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the active server credentials in AES-GCM encrypted SharedPreferences.
 * Uses Jetpack Security's MasterKey (AndroidKeyStore backed) so the API key is
 * never stored in plaintext on disk.
 */
@Singleton
class ConnectionStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = openPrefs(context)

        @Suppress("TooGenericExceptionCaught")
        private fun openPrefs(context: Context): SharedPreferences =
            try {
                createPrefs(context)
            } catch (e: Exception) {
                // Keystore entry and encrypted file out of sync (restore, OS upgrade, key
                // invalidation): decrypting throws forever and would crash every launch.
                // The stored credentials are unrecoverable, so drop them and let the user
                // reconnect rather than leaving the app unusable.
                Log.w(TAG, "Encrypted connection store unreadable; resetting", e)
                runCatching {
                    KeyStore.getInstance("AndroidKeyStore").apply {
                        load(null)
                        deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    }
                }
                context.deleteSharedPreferences(FILE_NAME)
                createPrefs(context)
            }

        private fun createPrefs(context: Context): SharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        @Suppress("TooGenericExceptionCaught")
        fun currentServer(): StashServer? =
            try {
                readServer()
            } catch (e: Exception) {
                // Individual values can fail to decrypt even when the file opens.
                Log.w(TAG, "Stored connection undecryptable; clearing", e)
                prefs.edit().clear().apply()
                null
            }

        private fun readServer(): StashServer? {
            val url = prefs.getString(KEY_URL, null)?.takeIf { it.isNotBlank() } ?: return null
            return StashServer(
                baseUrl = url,
                apiKey = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() },
                displayName = prefs.getString(KEY_NAME, null) ?: url,
            )
        }

        fun save(server: StashServer) {
            prefs
                .edit()
                .putString(KEY_URL, server.baseUrl)
                .putString(KEY_API_KEY, server.apiKey)
                .putString(KEY_NAME, server.displayName)
                .apply()
        }

        fun clear() {
            prefs.edit().clear().apply()
        }

        private companion object {
            const val TAG = "ConnectionStore"
            const val FILE_NAME = "stash_connection"
            const val KEY_URL = "url"
            const val KEY_API_KEY = "api_key"
            const val KEY_NAME = "name"
        }
    }
