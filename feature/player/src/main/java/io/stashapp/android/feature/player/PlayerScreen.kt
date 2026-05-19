package io.stashapp.android.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.domain.PlayerSettings
import io.stashapp.android.core.model.RepeatMode
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Native fullscreen player — MX Player-inspired utility-dense layout.
 *
 * Two rows of icon controls around the video:
 *  - Top: close, title/studio, queue position, codec badge, speed pill, PiP
 *  - Second row: rotation-lock, screenshot, aspect-ratio, shuffle, repeat
 *  - Bottom: progress bar (with tap-toggle remaining/total on the right label)
 *  - Bottom row: lock, -10, prev, play, next, +10
 *
 * Gestures:
 *  - Single tap: toggle controls
 *  - Double tap left/right: step-seek (configurable)
 *  - Horizontal drag: scrub
 *  - Lock on: all gestures disabled until unlocked via the unlock button
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onExit: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val seekMsPerPx by viewModel.preferences.seekMsPerPx
        .collectAsStateWithLifecycle(initialValue = PlayerSettings.DEFAULT_SEEK_MS_PER_PX)
    val doubleTapSeekSec by viewModel.preferences.doubleTapSeekSeconds
        .collectAsStateWithLifecycle(initialValue = PlayerSettings.DEFAULT_DOUBLE_TAP_SEEK_SEC)
    // Collected once for ChapterStrip — do NOT re-collect inside ChapterStrip call to avoid double-subscription
    val position by viewModel.position.collectAsStateWithLifecycle()

    // Controls visibility, interaction timestamp for auto-hide, and MX-Player-
    // style local UI state (lock, rotation-lock, aspect mode, remaining-time).
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var locked by remember { mutableStateOf(false) }
    var rotationLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showRemaining by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // Transient UI states
    var scrubPreview by remember { mutableStateOf<ScrubPreview?>(null) }
    var stepLeft by remember { mutableStateOf<StepSeek?>(null) }
    var stepRight by remember { mutableStateOf<StepSeek?>(null) }
    // D-11: right-anchored player settings panel
    var showSettingsPanel by remember { mutableStateOf(false) }

    // Force landscape unless rotation is locked to the current orientation.
    DisposableEffect(activity, rotationLocked) {
        val prior = activity?.requestedOrientation
        activity?.requestedOrientation =
            if (rotationLocked) {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = prior ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // playerView is a top-level remember ref set once by AndroidView.factory — stable after first frame (PERF-04: STABLE)
    // Sync resizeMode into the native PlayerView whenever it changes
    LaunchedEffect(resizeMode, playerView) { playerView?.resizeMode = resizeMode }

    LaunchedEffect(controlsVisible, state.isPlaying, lastInteraction, locked) {
        if (!locked && controlsVisible && state.isPlaying) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteraction >= 2900) controlsVisible = false
        }
    }

    LaunchedEffect(stepLeft?.generation) {
        if (stepLeft != null) {
            delay(800)
            stepLeft = null
        }
    }
    LaunchedEffect(stepRight?.generation) {
        if (stepRight != null) {
            delay(800)
            stepRight = null
        }
    }

    PredictiveBackHandler { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                // Phase 2: no animation work — accept the system's default preview.
                // (Future Spine Phase 5 MAY consume backEvent.progress here to drive
                // custom scale/fade on the player surface.)
            }
            // Flow completed normally → commit the back action.
            onExit()
        } catch (e: CancellationException) {
            // User cancelled the swipe; do NOT call onExit().
            // Re-throw to preserve coroutine cancellation propagation
            // (Kotlin etiquette — never swallow CancellationException).
            throw e
        }
    }

    // D-06: pre-compute scrim brushes once per composition (stable reference)
    val topScrimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
        )
    }
    val bottomScrimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xEB000000)),
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Media surface — drives `Surface.setFrameRate()` when the video
        // reports its fps, so the display compositor can schedule refresh
        // seamlessly on VRR panels (S23 et al.).
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx)
                    .apply {
                        useController = false
                        player = viewModel.player
                        setKeepContentOnPlayerReset(true)
                        this.resizeMode = resizeMode
                    }.also { playerView = it }
            },
            update = {
                it.player = viewModel.player
            },
        )
        // Rate-limit frame-rate setting to actual videoFrameRate changes, not every recomposition (PERF-09)
        LaunchedEffect(state.videoFrameRate) {
            playerView?.let { applyVideoFrameRate(it, state.videoFrameRate) }
        }

        // D-06 top scrim (90dp) — behind controls, above the media surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(topScrimBrush)
                .align(Alignment.TopCenter),
        )

        // D-06 bottom scrim (160dp) — behind controls, above the media surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(bottomScrimBrush)
                .align(Alignment.BottomCenter),
        )

        if (!locked) {
            // Gesture layer — tap / double-tap / horizontal drag
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(doubleTapSeekSec) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                lastInteraction = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                val width = size.width
                                val stepMs = doubleTapSeekSec * 1000L
                                val isLeft = offset.x < width / 2
                                val deltaMs = if (isLeft) -stepMs else stepMs
                                viewModel.seekBy(deltaMs)
                                if (isLeft) {
                                    val accum = (stepLeft?.totalSeconds ?: 0) - doubleTapSeekSec
                                    stepLeft = StepSeek(accum, (stepLeft?.generation ?: 0) + 1)
                                } else {
                                    val accum = (stepRight?.totalSeconds ?: 0) + doubleTapSeekSec
                                    stepRight = StepSeek(accum, (stepRight?.generation ?: 0) + 1)
                                }
                                lastInteraction = System.currentTimeMillis()
                            },
                        )
                    }.pointerInput(seekMsPerPx) {
                        var dragTotalPx = 0f
                        var startPositionMs = 0L
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragTotalPx = 0f
                                startPositionMs = viewModel.position.value.positionMs
                            },
                            onDragEnd = {
                                val targetMs =
                                    (startPositionMs + (dragTotalPx * seekMsPerPx).toLong())
                                        .coerceAtLeast(0L)
                                viewModel.seekTo(targetMs)
                                scrubPreview = null
                            },
                            onDragCancel = { scrubPreview = null },
                            onHorizontalDrag = { _, delta ->
                                dragTotalPx += delta
                                val deltaMs = (dragTotalPx * seekMsPerPx).toLong()
                                scrubPreview =
                                    ScrubPreview(
                                        targetMs = (startPositionMs + deltaMs).coerceAtLeast(0L),
                                        deltaMs = deltaMs,
                                    )
                            },
                        )
                    },
            )
        } else {
            // Locked: only a single unlock affordance is interactive.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            controlsVisible = !controlsVisible
                            lastInteraction = System.currentTimeMillis()
                        })
                    },
            )
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        stepLeft?.let { StepSeekCallout(it, Alignment.CenterStart) }
        stepRight?.let { StepSeekCallout(it, Alignment.CenterEnd) }

        scrubPreview?.let { preview ->
            // Duration rarely changes — read it once per scene for the preview.
            val snapshotDuration = viewModel.position.value.durationMs
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScrubPreviewCard(preview, snapshotDuration)
            }
        }

        AnimatedVisibility(
            visible = state.banner != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(240)) + slideOutVertically(tween(240)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
        ) {
            state.banner?.let { BannerPill(it) }
        }

        state.error?.let { err ->
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Playback error: $err", color = Color.White)
            }
        }

        // COMPLY-01 (per D-04, RESEARCH §F2): control-overlay layer inset
        // away from system bars. SurfaceView/PlayerView + gesture-detection
        // layers stay full-bleed (they are SIBLINGS of this Box). Only the
        // chrome (locked unlock button + PlayerControls top/bottom bars) is
        // inset. See 02.1-DEVIATIONS.md DEV-02 for wrap-scope rationale.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
        ) {
            // Lock-state overlay: show ONLY the unlock button, nothing else
            if (locked && controlsVisible) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    IconButton(
                        onClick = {
                            locked = false
                            lastInteraction = System.currentTimeMillis()
                        },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Unlock",
                            tint = SpineColors.AccentPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            // Bottom area: ChapterStrip + PlayerControls, anchored to bottom
            // D-05/SPINE-11: ChapterStrip sits above the transport controls as a sibling.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
            ) {
                // ChapterStrip — only visible when controls are visible (same guard as PlayerControls)
                if (!locked && controlsVisible && state.current?.markers.orEmpty().isNotEmpty()) {
                    ChapterStrip(
                        markers = state.current?.markers.orEmpty().toPersistentList(),
                        positionMs = position.positionMs,   // use already-collected val — do NOT re-collect
                        durationMs = position.durationMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .navigationBarsPadding(),       // T-05-10 mitigation: avoid nav-bar clipping
                    )
                }

                // Full control overlay (only when not locked)
                if (!locked) {
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter =
                            fadeIn(tween(220, easing = LinearOutSlowInEasing)) +
                                slideInVertically(tween(220)) { it / 12 },
                        exit = fadeOut(tween(340, easing = FastOutLinearInEasing)),
                    ) {
                        PlayerControls(
                            title =
                                state.current
                                    ?.summary
                                    ?.displayTitle
                                    .orEmpty(),
                            subtitle =
                                state.current
                                    ?.summary
                                    ?.studio
                                    ?.name,
                            queuePosition =
                                state.queue?.let {
                                    if (it.items.size > 1) "${it.currentIndex + 1}/${it.items.size}" else null
                                },
                            isPlaying = state.isPlaying,
                            shuffled = state.queue?.shuffled ?: false,
                            repeatMode = state.queue?.repeatMode ?: RepeatMode.OFF,
                            // Playhead state flow is observed inside TimelineBar so
                            // ticks only recompose the bar, not the full control tree.
                            positionFlow = viewModel.position,
                            markers =
                                state.current
                                    ?.markers
                                    .orEmpty()
                                    .toPersistentList(),
                            playbackSpeed = state.playbackSpeed,
                            canSkipPrev =
                                state.queue?.let {
                                    it.currentIndex > 0 || it.repeatMode == RepeatMode.ALL
                                } ?: false,
                            canSkipNext =
                                state.queue?.let {
                                    it.currentIndex < it.items.lastIndex || it.repeatMode != RepeatMode.OFF
                                } ?: false,
                            codecLabel = codecLabel(),
                            rotationLocked = rotationLocked,
                            resizeMode = resizeMode,
                            doubleTapSeekSec = doubleTapSeekSec,
                            showRemaining = showRemaining,
                            onPlayPause = {
                                val p = viewModel.player
                                if (p.isPlaying) p.pause() else p.play()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onNext = {
                                viewModel.skipNext()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onPrevious = {
                                viewModel.skipPrevious()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onShuffle = {
                                viewModel.toggleShuffle()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onRepeat = {
                                viewModel.cycleRepeat()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onSeek = { pos ->
                                viewModel.seekTo(pos)
                                lastInteraction = System.currentTimeMillis()
                            },
                            onSeekBy = { delta ->
                                viewModel.seekBy(delta)
                                lastInteraction = System.currentTimeMillis()
                            },
                            onPip = {
                                activity?.let { enterPip(it) }
                                lastInteraction = System.currentTimeMillis()
                            },
                            onClose = onExit,
                            onLock = {
                                locked = true
                                lastInteraction = System.currentTimeMillis()
                            },
                            onToggleRotationLock = {
                                rotationLocked = !rotationLocked
                                lastInteraction = System.currentTimeMillis()
                            },
                            onCycleResize = {
                                resizeMode = nextResize(resizeMode)
                                lastInteraction = System.currentTimeMillis()
                            },
                            onCycleSpeed = {
                                viewModel.cyclePlaybackSpeed()
                                lastInteraction = System.currentTimeMillis()
                            },
                            onToggleRemaining = {
                                showRemaining = !showRemaining
                                lastInteraction = System.currentTimeMillis()
                            },
                            onScreenshot = {
                                // TODO: capture frame. MediaMetadataRetriever path works
                                // but needs storage permission + URI handling — punting
                                // to a follow-up pass since it's more than UI plumbing.
                                viewModel.flashBanner("Screenshot — coming soon")
                            },
                            onToggleSettings = {
                                showSettingsPanel = !showSettingsPanel
                                lastInteraction = System.currentTimeMillis()
                            },
                        )
                    }
                }
            }
        }

        // D-11: PlayerSettingsPanel — right-anchored, ~40% landscape width, slides in from right
        PlayerSettingsPanel(
            visible = showSettingsPanel,
            playbackSpeed = state.playbackSpeed,
            onSpeedChange = { speed ->
                viewModel.setPlaybackSpeed(speed)
                lastInteraction = System.currentTimeMillis()
            },
            onDismiss = {
                showSettingsPanel = false
                lastInteraction = System.currentTimeMillis()
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

// ---- Helpers ----------------------------------------------------------------

/**
 * Declare the video's native fps to the Surface so the display compositor
 * can enter VRR or switch to a matching refresh mode seamlessly.
 *
 * Available since Android 11 (API 30); no-op on older releases. Called on
 * every compose `update` pass of the [AndroidView]; cheap because
 * `setFrameRate()` is idempotent when the value hasn't changed.
 */
private fun applyVideoFrameRate(
    playerView: PlayerView,
    fps: Float?,
) {
    if (fps == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return
    val videoView = playerView.videoSurfaceView ?: return
    val surface = (videoView as? android.view.SurfaceView)?.holder?.surface
    runCatching {
        surface?.setFrameRate(fps, android.view.Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
    }
}

private fun enterPip(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params =
            PictureInPictureParams
                .Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
        activity.enterPictureInPictureMode(params)
    }
}

private fun codecLabel(): String =
    when {
        CodecCapabilities.ffmpegExtensionUsable -> "HW+FF"
        else -> "HW"
    }

private fun nextResize(current: Int): Int =
    when (current) {
        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
