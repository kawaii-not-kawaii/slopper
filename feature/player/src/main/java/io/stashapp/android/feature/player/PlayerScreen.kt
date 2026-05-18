package io.stashapp.android.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.model.Marker
import io.stashapp.android.core.model.RepeatMode
import kotlinx.collections.immutable.ImmutableList
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
        .collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SEEK_MS_PER_PX)
    val doubleTapSeekSec by viewModel.preferences.doubleTapSeekSeconds
        .collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_DOUBLE_TAP_SEEK_SEC)

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
                            tint = StashColors.AccentPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
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
                    markers = state.current?.markers.orEmpty().toPersistentList(),
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
                )
                }
            }
        }
    }
}

// ---- MX-Player-style controls ----------------------------------------------

@Composable
private fun PlayerControls(
    title: String,
    subtitle: String?,
    queuePosition: String?,
    isPlaying: Boolean,
    shuffled: Boolean,
    repeatMode: RepeatMode,
    positionFlow: kotlinx.coroutines.flow.StateFlow<PlayerPositionState>,
    markers: ImmutableList<Marker>,
    playbackSpeed: Float,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    codecLabel: String,
    rotationLocked: Boolean,
    resizeMode: Int,
    doubleTapSeekSec: Int,
    showRemaining: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPip: () -> Unit,
    onClose: () -> Unit,
    onLock: () -> Unit,
    onToggleRotationLock: () -> Unit,
    onCycleResize: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleRemaining: () -> Unit,
    onScreenshot: () -> Unit,
) {
    // Note: outer wrap in PlayerScreen Box(safeDrawingPadding()) already
    // handles system-bar inset for the entire control overlay layer per
    // COMPLY-01 (RESEARCH §F2). Internal Column padding(sysBarsPadding) lines
    // were removed to avoid double-padding (see 02.1-DEVIATIONS.md DEV-02).

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val topH = 260f
                val bottomH = 320f
                // F2 = 95% opacity — heavier scrim so controls stay legible
                // over bright scenes without a full blackout.
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color(0xF2000000),
                            0.7f to Color(0x80000000),
                            1f to Color.Transparent,
                            endY = topH,
                        ),
                    size = Size(size.width, topH),
                )
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color(0x80000000),
                            1f to Color(0xF2000000),
                            startY = 0f,
                            endY = bottomH,
                        ),
                    topLeft = Offset(0f, size.height - bottomH),
                    size = Size(size.width, bottomH),
                )
            },
    ) {
        // Top block: main bar + secondary toggles row
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .align(Alignment.TopStart),
        ) {
            // Main top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    Modifier.weight(1f).padding(start = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title.ifBlank { "Playing" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        queuePosition?.let {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = StashColors.AccentSecondary,
                            )
                        }
                    }
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = StashColors.OnSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                // Right-side status chips — uniform height + corner radius so
                // they read as a single chip row regardless of content width.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodecBadge(codecLabel)
                    SpeedPill(playbackSpeed, onCycleSpeed)
                    PipChip(onClick = onPip)
                }
            }

            // Secondary toggles row (MX-Player style dense utility strip)
            Row(
                Modifier.padding(start = 4.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UtilityIconButton(
                    icon = if (rotationLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                    active = rotationLocked,
                    contentDescription = "Rotation lock",
                    onClick = onToggleRotationLock,
                )
                UtilityIconButton(
                    icon = Icons.Filled.AspectRatio,
                    active = false,
                    contentDescription = "Aspect: ${resizeLabel(resizeMode)}",
                    onClick = onCycleResize,
                )
                UtilityIconButton(
                    icon = Icons.Filled.PhotoCamera,
                    active = false,
                    contentDescription = "Screenshot",
                    onClick = onScreenshot,
                )
                UtilityIconButton(
                    icon = Icons.Filled.Shuffle,
                    active = shuffled,
                    contentDescription = "Shuffle",
                    onClick = onShuffle,
                )
                UtilityIconButton(
                    icon =
                        when (repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                    active = repeatMode != RepeatMode.OFF,
                    contentDescription = "Repeat",
                    onClick = onRepeat,
                )
            }
        }

        // Bottom block: progress + transport row — compact, tight vertical spacing
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TimelineBar(
                positionFlow = positionFlow,
                markers = markers,
                showRemaining = showRemaining,
                onSeek = onSeek,
                onToggleRightLabel = onToggleRemaining,
            )

            // Transport row — zero gap above, sits tight against the progress bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UtilityIconButton(
                    icon = Icons.Filled.LockOpen,
                    active = false,
                    contentDescription = "Lock screen",
                    onClick = onLock,
                )
                UtilityIconButton(
                    icon = Icons.Filled.Replay10,
                    active = false,
                    contentDescription = "Back ${doubleTapSeekSec}s",
                    onClick = { onSeekBy(-doubleTapSeekSec * 1000L) },
                )
                TransportIcon(
                    icon = Icons.Filled.SkipPrevious,
                    size = 36.dp,
                    enabled = canSkipPrev,
                    onClick = onPrevious,
                    contentDescription = "Previous",
                )
                PlayPauseFlat(isPlaying = isPlaying, onClick = onPlayPause)
                TransportIcon(
                    icon = Icons.Filled.SkipNext,
                    size = 36.dp,
                    enabled = canSkipNext,
                    onClick = onNext,
                    contentDescription = "Next",
                )
                UtilityIconButton(
                    icon = Icons.Filled.Forward10,
                    active = false,
                    contentDescription = "Forward ${doubleTapSeekSec}s",
                    onClick = { onSeekBy(doubleTapSeekSec * 1000L) },
                )
                UtilityIconButton(
                    icon = Icons.Filled.AspectRatio,
                    active = false,
                    contentDescription = "Aspect",
                    onClick = onCycleResize,
                )
            }
        }
    }
}

@Composable
private fun PlayPauseFlat(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    // Flat, no circle wrapper — matches MX Player. AnimatedContent gives the
    // icon swap a bit of motion so it doesn't feel too austere.
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(tween(160), initialScale = 0.7f) + fadeIn(tween(160))) togetherWith
                    (scaleOut(tween(160), targetScale = 1.2f) + fadeOut(tween(160)))
            },
            label = "play-pause-icon",
        ) { playing ->
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun TransportIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun UtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) StashColors.AccentPrimary else Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Height/corner shared by all three top-right chips so they read as a row. */
private val TopChipHeight = 36.dp
private val TopChipShape = RoundedCornerShape(10.dp)
private val TopChipMinWidth = 52.dp

@Composable
private fun CodecBadge(label: String) {
    Surface(
        color = StashColors.AccentSecondary.copy(alpha = 0.18f),
        contentColor = StashColors.AccentSecondary,
        shape = TopChipShape,
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = StashColors.AccentSecondary.copy(alpha = 0.6f),
            ),
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth),
    ) {
        Box(
            Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun SpeedPill(
    speed: Float,
    onClick: () -> Unit,
) {
    val active = speed != 1f
    Surface(
        color = if (active) StashColors.AccentPrimary else StashColors.SurfaceHigh.copy(alpha = 0.85f),
        contentColor = if (active) StashColors.AccentOnPrimary else Color.White,
        shape = TopChipShape,
        border =
            if (!active) {
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                )
            } else {
                null
            },
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth)
                .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                Icons.Filled.Speed,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                PlayerViewModel.formatSpeed(speed),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun PipChip(onClick: () -> Unit) {
    Surface(
        color = StashColors.SurfaceHigh.copy(alpha = 0.85f),
        contentColor = Color.White,
        shape = TopChipShape,
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
            ),
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth)
                .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PictureInPicture,
                contentDescription = "Picture in Picture",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---- Transient pieces -------------------------------------------------------

private data class ScrubPreview(
    val targetMs: Long,
    val deltaMs: Long,
)

private data class StepSeek(
    val totalSeconds: Int,
    val generation: Int,
)

@Composable
private fun StepSeekCallout(
    step: StepSeek,
    alignment: Alignment,
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(380, easing = FastOutLinearInEasing),
        label = "arc-progress-${step.generation}",
    )
    Box(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val baseRadius = size.minDimension * 0.25f
                val maxExpansion = size.minDimension * 0.12f
                val alpha = (1f - progress) * 0.30f
                val strokeColor = Color.White.copy(alpha = alpha)
                listOf(0f, 0.4f).forEach { offsetFrac ->
                    val r = baseRadius + (progress + offsetFrac) * maxExpansion
                    drawArc(
                        color = strokeColor,
                        startAngle = -130f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft =
                            Offset(
                                (size.width - 2 * r) / 2f,
                                (size.height - 2 * r) / 2f,
                            ),
                        size = Size(2 * r, 2 * r),
                        style = Stroke(width = 2f),
                    )
                }
            }
            val label = if (step.totalSeconds >= 0) "+${step.totalSeconds}s" else "${step.totalSeconds}s"
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun ScrubPreviewCard(
    preview: ScrubPreview,
    durationMs: Long,
) {
    val target = formatDuration(preview.targetMs)
    val total = durationMs.takeIf { it > 0 }?.let { formatDuration(it) }
    val deltaSeconds = preview.deltaMs / 1000
    val delta = if (deltaSeconds >= 0) "+${deltaSeconds}s" else "${deltaSeconds}s"

    Surface(
        color = StashColors.SurfaceHigh,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                target,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            if (total != null) {
                Text(
                    "of $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = StashColors.OnSurfaceVariant,
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = StashColors.AccentPrimary,
            )
        }
    }
}

@Composable
private fun BannerPill(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StashColors.SurfaceHigh.copy(alpha = 0.92f),
        modifier =
            Modifier.drawBehind {
                drawRect(
                    color = StashColors.AccentPrimary,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = StashColors.AccentPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

// ---- Timeline ---------------------------------------------------------------

@Composable
private fun TimelineBar(
    positionFlow: kotlinx.coroutines.flow.StateFlow<PlayerPositionState>,
    markers: ImmutableList<Marker>,
    showRemaining: Boolean,
    onSeek: (Long) -> Unit,
    onToggleRightLabel: () -> Unit,
) {
    // Subscribe to position here — this keeps the 250ms tick's recomposition
    // scoped to just this subtree.
    val pos by positionFlow.collectAsStateWithLifecycle()
    val positionMs = pos.positionMs
    val durationMs = pos.durationMs
    val bufferedMs = pos.bufferedMs

    val density = LocalDensity.current
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val hasDuration = durationMs > 0
    val playedFraction = if (hasDuration) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (hasDuration) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val displayFraction = dragFraction ?: playedFraction

    val trackHeight by animateDpAsState(if (dragging) 10.dp else 7.dp, label = "track-h")

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatDuration(if (dragFraction != null) (dragFraction!! * durationMs).toLong() else positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = if (dragging) StashColors.AccentPrimary else StashColors.OnSurfaceVariant,
            modifier = Modifier.size(width = 44.dp, height = 14.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(28.dp)
                .pointerInput(durationMs) {
                    if (!hasDuration) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                            dragging = false
                        },
                        onDragCancel = {
                            dragFraction = null
                            dragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                }.pointerInput(durationMs) {
                    if (!hasDuration) return@pointerInput
                    detectTapGestures(onTap = { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toLong())
                    })
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val th = size.height
                val corner = th / 2
                drawRoundRect(
                    color = StashColors.OnSurfaceFaint,
                    size = Size(size.width, th),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(corner, corner),
                )
                if (bufferedFraction > 0f) {
                    drawRoundRect(
                        color = Color(0x59A8B0C0),
                        size = Size(size.width * bufferedFraction, th),
                        cornerRadius =
                            androidx.compose.ui.geometry
                                .CornerRadius(corner, corner),
                    )
                }
                if (displayFraction > 0f) {
                    drawRoundRect(
                        color = StashColors.AccentPrimary,
                        size = Size(size.width * displayFraction, th),
                        cornerRadius =
                            androidx.compose.ui.geometry
                                .CornerRadius(corner, corner),
                    )
                }
            }

            if (hasDuration && markers.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val dotRadius = with(density) { 2.5.dp.toPx() }
                    val ringRadius = with(density) { 4.dp.toPx() }
                    val centerY = size.height / 2f
                    markers.forEach { marker ->
                        val frac = (marker.seconds / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                        val x = size.width * frac
                        drawCircle(
                            color = StashColors.AccentPrimary.copy(alpha = 0.6f),
                            radius = ringRadius,
                            center = Offset(x, centerY),
                            style = Stroke(width = 1.5f),
                        )
                        drawCircle(
                            color = StashColors.OnSurface,
                            radius = dotRadius,
                            center = Offset(x, centerY),
                        )
                    }
                }
            }

            if (hasDuration) {
                val thumbR by animateDpAsState(if (dragging) 10.dp else 7.dp, label = "thumb-r")
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val r = with(density) { thumbR.toPx() }
                    drawCircle(
                        color = StashColors.AccentPrimary,
                        radius = r,
                        center = Offset(size.width * displayFraction, size.height / 2f),
                    )
                }
            }
        }
        // Right-side time label — tap to toggle total vs remaining
        val remainingMs =
            (durationMs - (dragFraction?.let { (it * durationMs).toLong() } ?: positionMs))
                .coerceAtLeast(0L)
        val rightText =
            when {
                !hasDuration -> "--:--"
                showRemaining -> "-${formatDuration(remainingMs)}"
                else -> formatDuration(durationMs)
            }
        Text(
            rightText,
            style = MaterialTheme.typography.labelSmall,
            color = StashColors.OnSurfaceVariant,
            modifier =
                Modifier
                    .size(width = 56.dp, height = 14.dp)
                    .clickable(onClick = onToggleRightLabel),
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

private fun resizeLabel(mode: Int): String =
    when (mode) {
        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop"
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
        else -> "Fit"
    }

private fun formatDuration(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
