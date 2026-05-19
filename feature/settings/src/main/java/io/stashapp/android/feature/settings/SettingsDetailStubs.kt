package io.stashapp.android.feature.settings

import androidx.compose.runtime.Composable

/**
 * Stub composables for settings detail pages.
 * Plan 6.2 replaces each of these with a real implementation.
 * SettingsPlaybackScreen and SettingsCodecsScreen already replaced (Task 1).
 * SettingsDisplayScreen and SettingsLibraryScreen replaced in Task 2.
 * SettingsServerScreen and SettingsAboutScreen replaced in Task 3.
 */

@Composable
fun SettingsDisplayScreen(onBack: () -> Unit) {}

@Composable
fun SettingsLibraryScreen(onBack: () -> Unit) {}

@Composable
fun SettingsServerScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
) {}

@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {}
