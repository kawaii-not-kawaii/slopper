package io.stashapp.android.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.domain.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore-backed player preferences. */
private val Context.playerDataStore by preferencesDataStore(name = "player_prefs")

@Singleton
class PlayerPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PlayerSettings {
        // ---- Gesture sensitivity ------------------------------------------------

        override val seekMsPerPx: Flow<Float> = flow(KEY_SEEK_MS_PER_PX, DEFAULT_SEEK_MS_PER_PX)
        override val doubleTapSeekSeconds: Flow<Int> = flow(KEY_DOUBLE_TAP_SEEK_SEC, DEFAULT_DOUBLE_TAP_SEEK_SEC)

        override suspend fun setSeekMsPerPx(value: Float) = put(KEY_SEEK_MS_PER_PX, value)

        override suspend fun setDoubleTapSeekSeconds(value: Int) = put(KEY_DOUBLE_TAP_SEEK_SEC, value)


        // ---- Helpers (reduce boilerplate) ----------------------------------------

        private fun <T> flow(
            key: androidx.datastore.preferences.core.Preferences.Key<T>,
            default: T,
        ): Flow<T> = context.playerDataStore.data.map { it[key] ?: default }

        private suspend fun <T> put(
            key: androidx.datastore.preferences.core.Preferences.Key<T>,
            value: T,
        ) {
            context.playerDataStore.edit { it[key] = value }
        }

        companion object {
            // Gesture
            const val DEFAULT_SEEK_MS_PER_PX: Float = 120f
            const val DEFAULT_DOUBLE_TAP_SEEK_SEC: Int = 10
            const val SEEK_MS_PER_PX_MIN: Float = 20f
            const val SEEK_MS_PER_PX_MAX: Float = 500f
            const val DOUBLE_TAP_SEEK_MIN: Int = 5
            const val DOUBLE_TAP_SEEK_MAX: Int = 60

            // Keys
            private val KEY_SEEK_MS_PER_PX = floatPreferencesKey("seek_ms_per_px")
            private val KEY_DOUBLE_TAP_SEEK_SEC = intPreferencesKey("double_tap_seek_sec")
        }
    }
