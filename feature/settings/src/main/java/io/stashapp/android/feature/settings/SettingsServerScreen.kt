package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeLarge
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsServerScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "Server",
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
                actions = {
                    // Switch button: stub for v1, navigates to ConnectionScreen in a future phase
                    OutlinedButton(
                        onClick = { /* navigates to ConnectionScreen in a future phase */ },
                        modifier = Modifier.padding(end = 12.dp),
                        border = BorderStroke(1.dp, SpineColors.Border),
                        shape = ShapeSmall,
                    ) {
                        Text(
                            "Switch",
                            style = MetaMono,
                            color = SpineColors.OnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpineColors.Bg),
            )
        }

        // Status panel — null-guarded (T-06-03)
        item {
            val info = serverInfo
            if (info != null) {
                ConnectedInfoPanel(
                    server = activeServer,
                    info = info,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                ConnectedStubPanel(
                    server = activeServer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // Group: Network
        item {
            DetailGroup(
                title = "Network",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Endpoint",
                    hint = activeServer?.baseUrl ?: "—",
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "TLS",
                    hint = if (activeServer?.baseUrl?.startsWith("https") == true) "Enabled" else "Disabled",
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "API key",
                    hint = if (activeServer?.apiKey != null) "Configured" else "Not set",
                    trailing = {
                        // Stub — navigates to ConnectionScreen in future phase
                        OutlinedButton(
                            onClick = { },
                            border = BorderStroke(1.dp, SpineColors.Border),
                            shape = ShapeSmall,
                        ) {
                            Text("Replace", style = MetaMono, color = SpineColors.OnSurfaceVariant)
                        }
                    },
                )
            }
        }

        // Group: Actions
        item {
            DetailGroup(
                title = "Actions",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Refresh library now",
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
                    label = "Trigger server scan",
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
                    label = "Edit connection",
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

        // Danger zone — Disconnect
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = ShapeSmall,
                border = BorderStroke(1.dp, SpineColors.Border),
                color = SpineColors.Surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.disconnect(onDisconnected) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SpineColors.Error.copy(alpha = 0.08f), ShapeSmall),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerOff,
                            contentDescription = null,
                            tint = SpineColors.Error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Disconnect server",
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = SpineColors.Error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedInfoPanel(
    server: StashServer?,
    info: ServerInfo,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        border = BorderStroke(1.dp, SpineColors.Border),
        color = SpineColors.Surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SpineColors.Success, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CONNECTED",
                    style = MetaMono.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = SpineColors.Success,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = server?.baseUrl ?: "",
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = SpineColors.OnSurface,
            )
            Spacer(Modifier.height(4.dp))
            val tlsStatus = if (server?.baseUrl?.startsWith("https") == true) "on" else "off"
            val apiKeyStatus = if (server?.apiKey != null) "configured" else "not set"
            Text(
                text = "Stash v${info.version} · TLS $tlsStatus · API key $apiKeyStatus",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
            )
            Spacer(Modifier.height(16.dp))
            // 4-column count grid
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Scenes" to info.sceneCount,
                    "Studios" to info.studioCount,
                    "Performers" to info.performerCount,
                    "Tags" to info.tagCount,
                ).forEachIndexed { idx, (label, count) ->
                    if (idx > 0) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .background(SpineColors.Border),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SpineColors.Bg)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = count.toString(),
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = SpineColors.OnSurface,
                        )
                        Text(
                            text = label,
                            style = MetaMono,
                            color = SpineColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedStubPanel(
    server: StashServer?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        border = BorderStroke(1.dp, SpineColors.Border),
        color = SpineColors.Surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = server?.displayName ?: "Not connected",
                style = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SpineColors.OnSurface,
            )
            if (server?.baseUrl != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = server.baseUrl,
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                )
            }
        }
    }
}
