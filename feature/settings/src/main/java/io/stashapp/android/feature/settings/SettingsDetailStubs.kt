package io.stashapp.android.feature.settings

import androidx.compose.runtime.Composable

/**
 * Stub composables for settings detail pages.
 * Plan 6.2 replaces each of these with a real implementation.
 */

@Composable
fun SettingsPlaybackScreen(onBack: () -> Unit) {}

@Composable
fun SettingsCodecsScreen(onBack: () -> Unit) {}

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
