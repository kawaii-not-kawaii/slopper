package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.component.DRowStacked
import io.stashapp.android.core.designsystem.theme.EmberAccent
import io.stashapp.android.core.designsystem.theme.SageAccent
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SignalAccent
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDisplayScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val accentPalette by viewModel.uiPrefs.accentPalette.collectAsStateWithLifecycle("sage")
    val amoled by viewModel.uiPrefs.amoledBlackMode.collectAsStateWithLifecycle(false)
    val reduceMotion by viewModel.uiPrefs.reduceMotion.collectAsStateWithLifecycle(false)
    val gridColumns by viewModel.uiPrefs.gridColumns.collectAsStateWithLifecycle("auto")
    val cardDensity by viewModel.uiPrefs.cardDensity.collectAsStateWithLifecycle("comfortable")
    val longPress by viewModel.uiPrefs.longPressBehavior.collectAsStateWithLifecycle("quick_menu")
    val showRating by viewModel.uiPrefs.showRatingOnCards.collectAsStateWithLifecycle(true)
    val showPlayCount by viewModel.uiPrefs.showPlayCountOnCards.collectAsStateWithLifecycle(true)
    val showResolution by viewModel.uiPrefs.showResolutionOnCards.collectAsStateWithLifecycle(true)
    val showResumeBar by viewModel.uiPrefs.showResumeBar.collectAsStateWithLifecycle(false)
    val showStudioCaption by viewModel.uiPrefs.showStudioCaption.collectAsStateWithLifecycle(false)
    val showChapterStrip by viewModel.uiPrefs.showChapterStrip.collectAsStateWithLifecycle(false)
    val tapToPeek by viewModel.uiPrefs.tapToPeekInfo.collectAsStateWithLifecycle(false)

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "Display",
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SpineColors.OnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = SpineColors.OnSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpineColors.Bg),
            )
        }

        // Group: Theme (palette picker + AMOLED + reduce motion)
        item {
            DetailGroup(
                title = "Theme",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                // Palette swatch row (D-08)
                val palettes = listOf(
                    Triple("sage", "Sage", SageAccent.primary),
                    Triple("ember", "Ember", EmberAccent.primary),
                    Triple("signal", "Signal", SignalAccent.primary),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    palettes.forEach { (id, name, color) ->
                        val isActive = accentPalette == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ShapeSmall)
                                .background(
                                    if (isActive) color.copy(alpha = 0.08f) else SpineColors.Bg,
                                )
                                .border(1.dp, if (isActive) color else SpineColors.Border, ShapeSmall)
                                .clickable { viewModel.setAccentPalette(id) }
                                .padding(10.dp),
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color, RoundedCornerShape(6.dp)),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    style = TextStyle(
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = SpineColors.OnSurface,
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd),
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "AMOLED black mode",
                    hint = "True black surfaces for OLED panels",
                    trailing = {
                        SpineSwitch(checked = amoled) {
                            viewModel.setUi { setAmoledBlackMode(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Reduce motion",
                    hint = "Minimize animations throughout the app",
                    trailing = {
                        SpineSwitch(checked = reduceMotion) {
                            viewModel.setUi { setReduceMotion(it) }
                        }
                    },
                )
            }
        }

        // Group: Library layout
        item {
            DetailGroup(
                title = "Library layout",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Grid columns") {
                    ChipRow(
                        options = listOf(
                            "Auto" to "auto",
                            "2" to "2",
                            "3" to "3",
                            "4" to "4",
                        ),
                        selected = gridColumns,
                        onSelect = { v -> viewModel.setUi { setGridColumns(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(label = "Card density") {
                    ChipRow(
                        options = listOf(
                            "Compact" to "compact",
                            "Comfortable" to "comfortable",
                            "Spacious" to "spacious",
                        ),
                        selected = cardDensity,
                        onSelect = { v -> viewModel.setUi { setCardDensity(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(label = "Long-press behavior") {
                    ChipRow(
                        options = listOf(
                            "Play queue" to "play_queue",
                            "Quick menu" to "quick_menu",
                            "Off" to "off",
                        ),
                        selected = longPress,
                        onSelect = { v -> viewModel.setUi { setLongPressBehavior(v) } },
                    )
                }
            }
        }

        // Group: Card chrome
        item {
            DetailGroup(
                title = "Card chrome",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Rating",
                    trailing = {
                        SpineSwitch(checked = showRating) {
                            viewModel.setUi { setShowRatingOnCards(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Play count",
                    trailing = {
                        SpineSwitch(checked = showPlayCount) {
                            viewModel.setUi { setShowPlayCountOnCards(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Resolution badge",
                    trailing = {
                        SpineSwitch(checked = showResolution) {
                            viewModel.setUi { setShowResolutionOnCards(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Resume bar",
                    trailing = {
                        SpineSwitch(checked = showResumeBar) {
                            viewModel.setUi { setShowResumeBar(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Studio caption",
                    trailing = {
                        SpineSwitch(checked = showStudioCaption) {
                            viewModel.setUi { setShowStudioCaption(it) }
                        }
                    },
                )
            }
        }

        // Group: Player
        item {
            DetailGroup(
                title = "Player",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Show chapter strip",
                    hint = "Chapter markers above the timeline",
                    trailing = {
                        SpineSwitch(checked = showChapterStrip) {
                            viewModel.setUi { setShowChapterStrip(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Tap to peek info",
                    hint = "Single tap reveals scene info overlay",
                    trailing = {
                        SpineSwitch(checked = tapToPeek) {
                            viewModel.setUi { setTapToPeekInfo(it) }
                        }
                    },
                )
            }
        }
    }
}
