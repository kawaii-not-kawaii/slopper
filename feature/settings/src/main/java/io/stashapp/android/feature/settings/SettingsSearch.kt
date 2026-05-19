package io.stashapp.android.feature.settings

import io.stashapp.android.core.ui.nav.Routes

data class SettingsSearchEntry(
    val label: String,
    val hint: String,
    val breadcrumb: String,
    val route: String,
)

/**
 * Static settings search index. One entry per meaningful setting label.
 * Filtering: (label + " " + hint).contains(query, ignoreCase = true)
 * Updated manually when new settings are added.
 */
val SettingsSearchIndex: List<SettingsSearchEntry> = listOf(
    // --- Playback ---
    SettingsSearchEntry("Default speed", "Playback speed applied when a video starts", "Playback · Defaults", Routes.SettingsPlayback),
    SettingsSearchEntry("Aspect ratio", "Fit, crop, or stretch video to fill screen", "Playback · Defaults", Routes.SettingsPlayback),
    SettingsSearchEntry("Auto-play next", "Automatically start the next scene in queue", "Playback · Defaults", Routes.SettingsPlayback),
    SettingsSearchEntry("Auto-rotate on play", "Lock landscape orientation when video starts", "Playback · Defaults", Routes.SettingsPlayback),
    SettingsSearchEntry("Double-tap seek", "Seconds to skip forward or back on double-tap", "Playback · Seeking", Routes.SettingsPlayback),
    SettingsSearchEntry("Scrub sensitivity", "Milliseconds of video per pixel dragged on timeline", "Playback · Seeking", Routes.SettingsPlayback),
    SettingsSearchEntry("Chapter thumbnails", "Show chapter thumbnail when scrubbing the timeline", "Playback · Seeking", Routes.SettingsPlayback),
    SettingsSearchEntry("Resume threshold", "Minimum watch time before position is saved for resume", "Playback · Resume & skip", Routes.SettingsPlayback),
    SettingsSearchEntry("Completion threshold", "Watch percentage to mark a scene as complete", "Playback · Resume & skip", Routes.SettingsPlayback),
    SettingsSearchEntry("Skip intro", "Seconds to skip automatically at video start (0 = off)", "Playback · Resume & skip", Routes.SettingsPlayback),
    SettingsSearchEntry("Lock controls", "Hide player controls after a period of inactivity", "Playback · Player chrome", Routes.SettingsPlayback),
    SettingsSearchEntry("Codec badge", "Show hardware or software codec indicator in player", "Playback · Player chrome", Routes.SettingsPlayback),
    SettingsSearchEntry("Queue position", "Show current position and total count in player top bar", "Playback · Player chrome", Routes.SettingsPlayback),
    SettingsSearchEntry("Haptics on seek", "Vibrate on chapter marks and seek tap events", "Playback · Player chrome", Routes.SettingsPlayback),
    // --- Quality & Codecs ---
    SettingsSearchEntry("Decoder preference", "Choose Auto, Prefer Hardware, or Prefer Software decoder", "Quality & Codecs · Decoder", Routes.SettingsCodecs),
    SettingsSearchEntry("Fallback on failure", "Try software decoder if hardware decoding fails", "Quality & Codecs · Decoder", Routes.SettingsCodecs),
    SettingsSearchEntry("Tunneling", "Enable tunneled video rendering for compatible hardware", "Quality & Codecs · Decoder", Routes.SettingsCodecs),
    SettingsSearchEntry("Buffer size", "ExoPlayer preload buffer: Small 15s / Medium 50s / Large 2min", "Quality & Codecs · Buffer", Routes.SettingsCodecs),
    SettingsSearchEntry("Pre-buffer on hover", "Begin loading video before play is tapped", "Quality & Codecs · Buffer", Routes.SettingsCodecs),
    SettingsSearchEntry("HDR passthrough", "Pass HDR signal to the display panel", "Quality & Codecs · Display & HDR", Routes.SettingsCodecs),
    SettingsSearchEntry("Match refresh rate", "Switch display refresh rate to match video frame rate", "Quality & Codecs · Display & HDR", Routes.SettingsCodecs),
    SettingsSearchEntry("Match resolution", "Adjust display resolution to match video resolution", "Quality & Codecs · Display & HDR", Routes.SettingsCodecs),
    // --- Display ---
    SettingsSearchEntry("Accent color", "App accent palette: Sage, Ember, or Signal", "Display · Theme", Routes.SettingsDisplay),
    SettingsSearchEntry("AMOLED black", "True black surfaces for power savings on OLED panels", "Display · Theme", Routes.SettingsDisplay),
    SettingsSearchEntry("Reduce motion", "Minimize transition animations throughout the app", "Display · Theme", Routes.SettingsDisplay),
    SettingsSearchEntry("Grid columns", "Number of columns in the scene library grid", "Display · Library layout", Routes.SettingsDisplay),
    SettingsSearchEntry("Card density", "Spacing between cards: Compact, Comfortable, or Spacious", "Display · Library layout", Routes.SettingsDisplay),
    SettingsSearchEntry("Long-press behavior", "Action on long-pressing a scene card", "Display · Library layout", Routes.SettingsDisplay),
    SettingsSearchEntry("Rating on cards", "Show star rating overlay on scene thumbnails", "Display · Card chrome", Routes.SettingsDisplay),
    SettingsSearchEntry("Play count on cards", "Show view count badge on scene thumbnails", "Display · Card chrome", Routes.SettingsDisplay),
    SettingsSearchEntry("Resume bar", "Show progress bar at the bottom of watched scene cards", "Display · Card chrome", Routes.SettingsDisplay),
    SettingsSearchEntry("Studio caption", "Show studio name below scene thumbnails", "Display · Card chrome", Routes.SettingsDisplay),
    SettingsSearchEntry("Chapter strip", "Show proportional chapter markers above the player timeline", "Display · Player", Routes.SettingsDisplay),
    SettingsSearchEntry("Tap to peek info", "Single tap reveals scene info overlay in player", "Display · Player", Routes.SettingsDisplay),
    // --- Library ---
    SettingsSearchEntry("Activity tracking", "Send play, resume, and finish events to Stash server", "Library · Sync", Routes.SettingsLibrary),
    SettingsSearchEntry("Sync ratings", "Upload star rating changes to the Stash server", "Library · Sync", Routes.SettingsLibrary),
    SettingsSearchEntry("Sync O-counter", "Upload O-counter increments to the Stash server", "Library · Sync", Routes.SettingsLibrary),
    SettingsSearchEntry("Sync markers", "Upload chapter and marker edits to the Stash server", "Library · Sync", Routes.SettingsLibrary),
    SettingsSearchEntry("Image cache size", "Size of the thumbnail disk cache in megabytes", "Library · Cache", Routes.SettingsLibrary),
    SettingsSearchEntry("Cache duration", "How long cached thumbnails are retained on device", "Library · Cache", Routes.SettingsLibrary),
    SettingsSearchEntry("Keep watch history", "Record scenes you have watched in local history", "Library · History", Routes.SettingsLibrary),
    SettingsSearchEntry("History on Home", "Show Recently Watched rail on the Home screen", "Library · History", Routes.SettingsLibrary),
    SettingsSearchEntry("Smart rails", "AI-curated content suggestions based on watch history", "Library · History", Routes.SettingsLibrary),
    // --- Server ---
    SettingsSearchEntry("Server connection", "View and edit the active server URL and API key", "Server", Routes.SettingsServer),
    SettingsSearchEntry("Disconnect server", "Sign out and return to the connection setup screen", "Server · Danger zone", Routes.SettingsServer),
)
