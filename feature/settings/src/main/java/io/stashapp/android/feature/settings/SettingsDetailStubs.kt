package io.stashapp.android.feature.settings

import androidx.compose.runtime.Composable

/**
 * Stub composables for settings detail pages.
 * Plan 6.2 replaces each of these with a real implementation.
 * Tasks 1-2 complete: Playback, Codecs, Display, Library replaced.
 * Task 3 replaces Server and About.
 */

@Composable
fun SettingsServerScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
) {}

@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {}
