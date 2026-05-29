package io.stashapp.android.feature.settings

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeMedium
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Read version info via PackageManager — BuildConfig is not available in feature modules
    val versionName =
        remember(context) {
            try {
                val pkgInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                pkgInfo.versionName ?: "unknown"
            } catch (_: PackageManager.NameNotFoundException) {
                "unknown"
            }
        }
    // Detect build type from versionName suffix: "-alpha"/"-debug" => debug build, else release
    val buildType =
        if (versionName.contains("alpha", ignoreCase = true) ||
            versionName.contains("debug", ignoreCase = true)
        ) {
            "debug build"
        } else {
            "release"
        }

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "About & Diagnostics",
                        style =
                            TextStyle(
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

        // Version block (centered)
        item {
            val accent = LocalAccentColors.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 18.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .background(SpineColors.SurfaceHigh, ShapeMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Diamond,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Slopper",
                    style =
                        TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = SpineColors.OnSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "v$versionName · $buildType",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                )
                serverInfo?.buildTime?.let { buildTime ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "built $buildTime",
                        style = MonoSmall,
                        color = SpineColors.OnSurfaceFaint,
                    )
                }
            }
        }

        // Group: Capabilities (static stubs for v1)
        item {
            DetailGroup(
                title = "Capabilities",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Codec support",
                    hint = "HW + FFmpeg",
                    trailing = {
                        Text("Full", style = MetaMono, color = SpineColors.Success)
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Hardware decoder",
                    hint = "MediaCodec available",
                    trailing = {
                        Text("Yes", style = MetaMono, color = SpineColors.Success)
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "OpenGL",
                    hint = "ES 3.1",
                    trailing = {
                        Text("Supported", style = MetaMono, color = SpineColors.Success)
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "HDR",
                    hint = "PQ + HLG",
                    trailing = {
                        Text("Supported", style = MetaMono, color = SpineColors.Success)
                    },
                )
            }
        }

        // Group: Storage (static stubs)
        item {
            DetailGroup(
                title = "Storage",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Image cache",
                    hint = "Stored in app external cache",
                    trailing = {
                        Text("—", style = MetaMono, color = SpineColors.OnSurfaceMuted)
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Database",
                    hint = "Room DB for offline state",
                    trailing = {
                        Text("—", style = MetaMono, color = SpineColors.OnSurfaceMuted)
                    },
                )
            }
        }

        // Group: Diagnostics
        item {
            DetailGroup(
                title = "Diagnostics",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "View logs",
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = SpineColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Run network test",
                    trailing = {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Network test — coming soon", Toast.LENGTH_SHORT).show()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpineColors.Border),
                            shape = ShapeSmall,
                        ) {
                            Text("Test", style = MetaMono, color = SpineColors.OnSurfaceVariant)
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Send debug report",
                    hint = "Anonymized app state only",
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = SpineColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
            }
        }

        // Group: Legal
        item {
            DetailGroup(
                title = "Legal",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Open-source licenses",
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = SpineColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Privacy policy",
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = SpineColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Built on",
                    hint = "Stash · ExoPlayer · Jetpack Compose",
                )
            }
        }
    }
}
