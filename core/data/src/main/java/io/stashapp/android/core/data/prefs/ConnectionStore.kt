package io.stashapp.android.core.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.model.StashServer
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
        private val prefs =
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

        fun currentServer(): StashServer? {
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
            const val FILE_NAME = "stash_connection"
            const val KEY_URL = "url"
            const val KEY_API_KEY = "api_key"
            const val KEY_NAME = "name"
        }
    }
