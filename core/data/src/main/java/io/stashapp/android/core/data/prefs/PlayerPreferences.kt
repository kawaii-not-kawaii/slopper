package io.stashapp.android.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

        // ---- Playback defaults --------------------------------------------------

        /** Persistent default speed (1.0 = normal). Applied when a new scene starts. */
        override val defaultPlaybackSpeed: Flow<Float> = flow(KEY_DEFAULT_SPEED, DEFAULT_SPEED)

        override suspend fun setDefaultPlaybackSpeed(value: Float) = put(KEY_DEFAULT_SPEED, value)

        /** Auto-advance to the next item in the queue when a scene ends. */
        override val autoPlayNext: Flow<Boolean> = flow(KEY_AUTO_PLAY_NEXT, DEFAULT_AUTO_PLAY_NEXT)

        override suspend fun setAutoPlayNext(value: Boolean) = put(KEY_AUTO_PLAY_NEXT, value)

        /** Seconds below which resume position is ignored ("start from beginning"). */
        override val resumeThresholdSeconds: Flow<Int> = flow(KEY_RESUME_THRESHOLD, DEFAULT_RESUME_THRESHOLD)

        override suspend fun setResumeThresholdSeconds(value: Int) = put(KEY_RESUME_THRESHOLD, value)

        /** Percentage of duration watched before a "completed play" is recorded. */
        override val completionThresholdPercent: Flow<Int> = flow(KEY_COMPLETION_THRESHOLD, DEFAULT_COMPLETION_THRESHOLD)

        override suspend fun setCompletionThresholdPercent(value: Int) = put(KEY_COMPLETION_THRESHOLD, value)

        /** Seconds to auto-skip at the start of every scene (intro skip). 0 = off. */
        override val skipIntroSeconds: Flow<Int> = flow(KEY_SKIP_INTRO, DEFAULT_SKIP_INTRO)

        override suspend fun setSkipIntroSeconds(value: Int) = put(KEY_SKIP_INTRO, value)

        /** Video buffer preset — controls ExoPlayer's min/max buffer. */
        override val videoBufferPreset: Flow<String> = flow(KEY_BUFFER_PRESET, DEFAULT_BUFFER_PRESET)

        override suspend fun setVideoBufferPreset(value: String) = put(KEY_BUFFER_PRESET, value)

        /** Default aspect-ratio resize mode. */
        override val defaultAspectRatio: Flow<String> = flow(KEY_ASPECT_RATIO, DEFAULT_ASPECT_RATIO)

        override suspend fun setDefaultAspectRatio(value: String) = put(KEY_ASPECT_RATIO, value)

        /** Decoder preference: "auto", "prefer_hw", "prefer_sw". */
        override val decoderPreference: Flow<String> = flow(KEY_DECODER_PREF, DEFAULT_DECODER_PREF)

        override suspend fun setDecoderPreference(value: String) = put(KEY_DECODER_PREF, value)

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

            // Playback
            const val DEFAULT_SPEED: Float = 1.0f
            const val DEFAULT_AUTO_PLAY_NEXT: Boolean = true
            const val DEFAULT_RESUME_THRESHOLD: Int = 2
            const val DEFAULT_COMPLETION_THRESHOLD: Int = 85
            const val DEFAULT_SKIP_INTRO: Int = 0
            const val DEFAULT_BUFFER_PRESET: String = "medium" // small | medium | large
            const val DEFAULT_ASPECT_RATIO: String = "fit" // fit | crop | stretch
            const val DEFAULT_DECODER_PREF: String = "auto" // auto | prefer_hw | prefer_sw

            // Keys
            private val KEY_SEEK_MS_PER_PX = floatPreferencesKey("seek_ms_per_px")
            private val KEY_DOUBLE_TAP_SEEK_SEC = intPreferencesKey("double_tap_seek_sec")
            private val KEY_DEFAULT_SPEED = floatPreferencesKey("default_speed")
            private val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
            private val KEY_RESUME_THRESHOLD = intPreferencesKey("resume_threshold_sec")
            private val KEY_COMPLETION_THRESHOLD = intPreferencesKey("completion_threshold_pct")
            private val KEY_SKIP_INTRO = intPreferencesKey("skip_intro_sec")
            private val KEY_BUFFER_PRESET = stringPreferencesKey("buffer_preset")
            private val KEY_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
            private val KEY_DECODER_PREF = stringPreferencesKey("decoder_preference")
        }
    }
